/**
 * Copyright (c) KMG. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 */
package io.sbk.api.impl;

import java.io.IOException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.locks.LockSupport;
import java.nio.file.Files;
import java.nio.file.Paths;

import io.sbk.api.Config;
import io.sbk.api.Performance;
import io.sbk.api.RecordTime;
import io.sbk.api.ResultLogger;
import io.sbk.api.TimeStamp;
import lombok.Synchronized;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;

import javax.annotation.concurrent.GuardedBy;
import javax.annotation.concurrent.NotThreadSafe;


/**
 * Class for Performance statistics.
 */
final public class SbkPerformance implements Performance {
    final private String action;
    final private String csvFile;
    final private int windowInterval;
    final private int idleNS;
    final private int baseLatency;
    final private int maxWindowLatency;
    final private int maxLatency;
    final private int maxQs;
    final private ResultLogger periodicLogger;
    final private ResultLogger totalLogger;
    final private ExecutorService executor;
    final private TimeRecorder[]  timeRecorders;

    @GuardedBy("this")
    private int index;

    @GuardedBy("this")
    private CompletableFuture<Void> retFuture;

    public SbkPerformance(String action, Config config, int workers, String csvFile,
                          ResultLogger periodicLogger, ResultLogger totalLogger, ExecutorService executor) {
        this.action = action;
        this.idleNS = Math.max(Config.MIN_IDLE_NS, config.idleNS);
        this.baseLatency = Math.max(Config.DEFAULT_MIN_LATENCY, config.minLatency);
        this.windowInterval = Math.max(Config.MIN_REPORTING_INTERVAL_MS, config.reportingMS);
        this.maxWindowLatency = Math.min(Integer.MAX_VALUE,
                                    Math.max(config.maxWindowLatency, Config.DEFAULT_WINDOW_LATENCY));
        this.maxLatency = Math.min(Integer.MAX_VALUE,
                                    Math.max(config.maxLatency, Config.DEFAULT_MAX_LATENCY));
        this.csvFile = csvFile;
        this.periodicLogger = periodicLogger;
        this.totalLogger = totalLogger;
        this.executor = executor;
        this.retFuture = null;
        if (config.maxQs > 0) {
            maxQs = config.maxQs;
            this.timeRecorders = new TimeRecorder[1];
            this.index = 1;
        } else {
            maxQs =  Math.max(Config.MIN_Q_PER_WORKER, config.qPerWorker);
            this.timeRecorders = new TimeRecorder[workers];
            this.index = workers;
        }
        for (int i = 0; i < timeRecorders.length; i++) {
            timeRecorders[i] = new TimeRecorder(maxQs);
        }
    }

    /**
     * Private class for start and end time.
     */
    final private class QueueProcessor implements Runnable {
        final private long startTime;

        private QueueProcessor(long startTime) {
            this.startTime = startTime;
        }

        public void run() {
            final TimeWindow window;
            final LatencyWriter latencyRecorder;
            boolean doWork = true;
            long time = startTime;
            boolean notFound;
            TimeStamp t;

            if (csvFile != null) {
                try {
                    latencyRecorder = new CSVLatencyWriter(action + "(Total)", startTime, baseLatency,
                            maxLatency, action, csvFile);
                } catch (IOException ex) {
                    ex.printStackTrace();
                    return;
                }
            } else {
                latencyRecorder = new LatencyWriter(action+"(Total)", startTime, baseLatency, maxLatency);
            }
            window = new TimeWindow(action, startTime, baseLatency, maxWindowLatency, windowInterval, idleNS);
            while (doWork) {
                notFound = true;
                for (TimeRecorder recorder : timeRecorders) {
                    t = recorder.poll();
                    if (t != null) {
                        notFound = false;
                        if (t.isEnd()) {
                            doWork = false;
                        } else {
                            final int latency = (int) (t.endTime - t.startTime);
                            window.record(startTime, t.bytes, t.records, latency);
                            latencyRecorder.record(t.startTime, t.bytes, t.records, latency);
                        }
                        time = t.endTime;
                        if (window.elapsedTimeMS(time) > windowInterval) {
                            window.print(time, periodicLogger);
                            window.reset(time);
                        }
                    }
                }
                if (notFound) {
                    window.idleWaitPrint(periodicLogger);
                }
            }
            latencyRecorder.print(time, totalLogger);
        }
    }

    /**
     * Private class for counter implementation to reduce System.currentTimeMillis() invocation.
     */
    @NotThreadSafe
    final static private class ElasticCounter {
        final private int windowInterval;
        final private int idleNS;
        final private double minWaitTimeMS;
        final private double countRatio;
        final private long minIdleCount;
        private long elasticCount;
        private long idleCount;
        private long totalCount;

        private ElasticCounter(int windowInterval, int idleNS) {
            this.windowInterval = windowInterval;
            this.idleNS = idleNS;
            minWaitTimeMS = windowInterval / 50.0;
            countRatio = (Config.NS_PER_MS * 1.0) / idleNS;
            minIdleCount = (long) (countRatio * minWaitTimeMS);
            elasticCount = minIdleCount;
            idleCount = 0;
            totalCount = 0;
        }

        private boolean waitCheck() {
            LockSupport.parkNanos(idleNS);
            idleCount++;
            totalCount++;
            return idleCount > elasticCount;
        }

        private void reset() {
            idleCount = 0;
        }

        private void updateElastic(long diffTime) {
            elasticCount = Math.max((long) (countRatio * (windowInterval - diffTime)), minIdleCount);
        }

        private void setElastic(long diffTime) {
            elasticCount =  (totalCount * windowInterval) / diffTime;
            totalCount = 0;
        }
    }


    /**
     * Private class for Performance statistics within a given time window.
     */
    @NotThreadSafe
    static private class LatencyWriter {
        final private double[] percentiles = {0.1, 0.25, 0.5, 0.75, 0.95, 0.99, 0.999, 0.9999};
        final private String action;
        final private int baseLatency;
        final private long[] latencies;
        private long startTime;
        private long validLatencyRecords;
        private long lowerLatencyDiscardRecords;
        private long higherLatencyDiscardRecords;
        private long bytes;
        private long totalLatency;
        private int maxLatency;

        LatencyWriter(String action, long start, int baseLatency, int latencyThreshold) {
            this.action = action;
            this.baseLatency = baseLatency;
            this.latencies = new long[latencyThreshold-baseLatency];
            resetValues(start);
        }

        private void resetValues(long start) {
            this.startTime = start;
            this.validLatencyRecords = 0;
            this.lowerLatencyDiscardRecords = 0;
            this.higherLatencyDiscardRecords = 0;
            this.bytes = 0;
            this.maxLatency = 0;
            this.totalLatency = 0;
        }

        public void reset(long start) {
            resetValues(start);
        }

        private int[] getPercentiles() {
            final int[] values = new int[percentiles.length];
            final long[] percentileIds = new long[percentiles.length];
            long cur = 0;
            int index = 0;

            for (int i = 0; i < percentileIds.length; i++) {
                percentileIds[i] = (long) (validLatencyRecords * percentiles[i]);
            }

            for (int i = 0; i < Math.min(latencies.length, this.maxLatency+1); i++) {
                if (latencies[i] > 0) {
                     while (index < values.length) {
                         if (percentileIds[index] >= cur && percentileIds[index] < (cur + latencies[i])) {
                             values[index] = i + baseLatency;
                             index += 1;
                         } else {
                             break;
                         }
                    }
                    cur += latencies[i];
                    latencies[i] = 0;
                }
            }
            return values;
        }

        /**
         * Record the latency
         *
         * @param startTime start time.
         * @param bytes number of bytes.
         * @param events number of events(records).
         * @param latency latency value in milliseconds.
         */
        public void record(long startTime, int bytes, int events, int latency) {
            if (latency < this.baseLatency) {
                this.lowerLatencyDiscardRecords += events;
            } else {
                final int index = latency - this.baseLatency;
                if (index < this.latencies.length) {
                    this.latencies[index] += events;
                    this.validLatencyRecords += events;
                } else {
                    this.higherLatencyDiscardRecords += events;
                }
            }
            this.bytes += bytes;
            this.totalLatency += latency * events;
            this.maxLatency = Math.max(this.maxLatency, latency);
        }

        /**
         * Get the current time duration of this window
         *
         * @param time current time.
         */
        public long elapsedTimeMS(long time) {
            return time - startTime;
        }

        /**
         * Print the window statistics
         */
        public void print(long endTime, ResultLogger logger) {
            final long totalRecords  = this.validLatencyRecords +
                    this.lowerLatencyDiscardRecords + this.higherLatencyDiscardRecords;
            final double elapsed = (endTime - startTime) / 1000.0;
            final double recsPerSec = totalRecords / elapsed;
            final double mbPerSec = (this.bytes / (1024.0 * 1024.0)) / elapsed;
            int[] percs = getPercentiles();

            logger.print(action, this.bytes, totalRecords, recsPerSec, mbPerSec,
                    this.totalLatency / (double) totalRecords, this.maxLatency,
                    this.lowerLatencyDiscardRecords, this.higherLatencyDiscardRecords,
                    percs[0], percs[1], percs[2], percs[3], percs[4], percs[5], percs[6], percs[7]);
        }
    }

    @NotThreadSafe
    final static private class TimeWindow extends LatencyWriter {
        final private ElasticCounter idleCounter;
        final private int windowInterval;

        private TimeWindow(String action, long start, int baseLatency, int latencyThreshold, int interval, int idleNS) {
            super(action, start, baseLatency, latencyThreshold);
            this.idleCounter = new ElasticCounter(interval, idleNS);
            this.windowInterval = interval;
        }

        @Override
        public void reset(long start) {
            super.reset(start);
            this.idleCounter.reset();
        }

        private void waitCheckPrint(ElasticCounter counter, ResultLogger logger) {
            if (counter.waitCheck()) {
                final long time = System.currentTimeMillis();
                final long diffTime = elapsedTimeMS(time);
                if (diffTime > windowInterval) {
                    print(time, logger);
                    reset(time);
                    counter.setElastic(diffTime);
                } else {
                    counter.updateElastic(diffTime);
                }
            }
        }

        private void idleWaitPrint(ResultLogger logger) {
                waitCheckPrint(idleCounter, logger);
        }
    }


    @NotThreadSafe
    static private class CSVLatencyWriter extends LatencyWriter {
        final private String csvFile;
        final private CSVPrinter csvPrinter;

        CSVLatencyWriter(String action, long start,  int baseLatency, int latencyThreshold,
                         String latencyName, String csvFile) throws IOException {
            super(action, start, baseLatency, latencyThreshold);
            this.csvFile = csvFile;
            csvPrinter = new CSVPrinter(Files.newBufferedWriter(Paths.get(csvFile)), CSVFormat.DEFAULT
                    .withHeader("Start Time (Milliseconds)", "data size (bytes)", "Records", latencyName + " Latency (Milliseconds)"));
        }

        private void readCSV() {
            try {
                CSVParser csvParser = new CSVParser(Files.newBufferedReader(Paths.get(csvFile)), CSVFormat.DEFAULT
                        .withFirstRecordAsHeader().withIgnoreHeaderCase().withTrim());

                for (CSVRecord csvEntry : csvParser) {
                    super.record(Long.parseLong(csvEntry.get(0)), Integer.parseInt(csvEntry.get(1)),
                            Integer.parseInt(csvEntry.get(2)), Integer.parseInt(csvEntry.get(3)));
                }
                csvParser.close();
            } catch (IOException ex) {
                throw new RuntimeException(ex);
            }
        }

        @Override
        public void record(long startTime, int bytes, int events, int latency) {
            try {
                csvPrinter.printRecord(startTime, bytes, events, latency);
            } catch (IOException ex) {
                throw new RuntimeException(ex);
            }
        }

        @Override
        public void print(long endTime, ResultLogger logger) {
            try {
                csvPrinter.close();
            } catch (IOException ex) {
                throw new RuntimeException(ex);
            }
            readCSV();
            super.print(endTime, logger);
        }
    }

    @NotThreadSafe
    static final class TimeRecorder implements RecordTime {
        final private ConcurrentLinkedQueue<TimeStamp>[] cQueues;
        private int index;

        public TimeRecorder(int qSize) {
            this.index = qSize;
            this.cQueues = new ConcurrentLinkedQueue[qSize];
            for (int i = 0; i < cQueues.length; i++) {
                cQueues[i] = new ConcurrentLinkedQueue<>();
            }
        }

        public TimeStamp poll() {
            index += 1;
            if (index >= cQueues.length) {
                index = 0;
            }
            return cQueues[index].poll();
        }

        public void enqEndTime(long endTime) {
            cQueues[0].add(new TimeStamp(endTime));
        }

        public void clear() {
            for (ConcurrentLinkedQueue<TimeStamp> q: cQueues) {
                q.clear();
            }
        }

        /* This Method is Thread Safe */
        @Override
        public void accept(int id, long startTime, long endTime, int bytes, int records) {
            cQueues[id].add(new TimeStamp(startTime, endTime, bytes, records));
        }
    }

    @Override
    @Synchronized
    public RecordTime get() {
        if (timeRecorders.length == 1) {
                return timeRecorders[0];
        }
        index += 1;
        if (index >= timeRecorders.length) {
            index = 0;
        }
        return  timeRecorders[index];
    }


    @Override
    @Synchronized
    public CompletableFuture<Void> start(long startTime) {
        if (this.retFuture == null) {
            this.retFuture = CompletableFuture.runAsync(new QueueProcessor(startTime), executor);
        }
        return this.retFuture;
    }

    @Override
    @Synchronized
    public void stop(long endTime)  {
        if (this.retFuture != null) {
            for (TimeRecorder recorder : timeRecorders) {
                recorder.enqEndTime(endTime);
            }
            try {
                retFuture.get();
            } catch (ExecutionException | InterruptedException ex) {
                ex.printStackTrace();
            }
            for (TimeRecorder recorder: timeRecorders) {
                recorder.clear();
            }
            this.retFuture = null;
        }
    }
}