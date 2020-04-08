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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.locks.LockSupport;
import java.nio.file.Files;
import java.nio.file.Paths;

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
    final private static int NS_PER_MICRO = 1000;
    final private static int MICROS_PER_MS = 1000;
    final private static int MS_PER_SEC = 1000;
    final private static int NS_PER_MS = NS_PER_MICRO * MICROS_PER_MS;
    final private static int MS_PER_MIN = MS_PER_SEC * 60;
    final private static int MS_PER_HR = MS_PER_MIN * 60;
    final private static int MIN_REPORTING_INTERVAL_MS = 5000;
    final private static int MIN_Q_PER_WORKER = 1;

    final private String action;
    final private String csvFile;
    final private int messageSize;
    final private int windowInterval;
    final private int workerIdleNS;
    final private int idleNS;
    final private ResultLogger periodicLogger;
    final private ResultLogger totalLogger;
    final private ExecutorService executor;
    final private ConcurrentLinkedQueue<TimeStamp>[][] cQueues;

    @GuardedBy("this")
    private int size;

    @GuardedBy("this")
    private CompletableFuture<Void> ret;

    public SbkPerformance(String action, int messageSize, int workers, int reportingInterval, int qPerWorker,
               int workerIdleNS, int idleNS, String csvFile, ResultLogger periodicLogger,
               ResultLogger totalLogger, ExecutorService executor) {
        final int numQ = Math.max(MIN_Q_PER_WORKER, qPerWorker);
        this.action = action;
        this.messageSize = messageSize;
        this.windowInterval = Math.max(MIN_REPORTING_INTERVAL_MS, reportingInterval);
        this.idleNS = idleNS;
        this.workerIdleNS = Math.min(this.idleNS, workerIdleNS);
        this.csvFile = csvFile;
        this.periodicLogger = periodicLogger;
        this.totalLogger = totalLogger;
        this.executor = executor;
        this.cQueues = new ConcurrentLinkedQueue[workers][numQ];
        this.size = 0;
        this.ret = null;
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
            final boolean idleWorker = cQueues.length > 1 && workerIdleNS > 0;
            boolean doWork = true;
            long time = startTime;
            boolean notFound;
            TimeStamp t;

            if (csvFile != null) {
                try {
                    latencyRecorder = new CSVLatencyWriter(action + "(Total)", startTime, MS_PER_HR, action, csvFile);
                } catch (IOException ex) {
                    ex.printStackTrace();
                    return;
                }
            } else {
                latencyRecorder = new LatencyWriter(action+"(Total)", startTime, MS_PER_HR);
            }
            window = new TimeWindow(action, startTime, MS_PER_MIN, windowInterval, workerIdleNS, idleNS);
            while (doWork) {
                notFound = true;
                for (ConcurrentLinkedQueue<TimeStamp>[] queues : cQueues) {
                    for (ConcurrentLinkedQueue<TimeStamp> queue : queues) {
                        t = queue.poll();
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
                    if (notFound && idleWorker) {
                       window.minWait();
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
            countRatio = (NS_PER_MS * 1.0) / idleNS;
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
        final double[] percentiles = {0.1, 0.25, 0.5, 0.75, 0.95, 0.99, 0.999, 0.9999};
        final private String action;
        final private int[] latencies;
        private long startTime;
        private long records;
        private long latencyRecords;
        private long bytes;
        private int maxLatency;
        private long totalLatency;
        private long discard;
        private ArrayList<int[]> latencyRanges;

        LatencyWriter(String action, long start, int latencyThreshold) {
            this.action = action;
            this.latencies = new int[latencyThreshold];
            resetValues(start);
        }

        private void resetValues(long start) {
            this.startTime = start;
            this.records = 0;
            this.latencyRecords = 0;
            this.bytes = 0;
            this.maxLatency = 0;
            this.totalLatency = 0;
            this.discard = 0;
            this.latencyRanges = null;
            Arrays.fill(this.latencies, 0);
        }

        public void reset(long start) {
            resetValues(start);
        }

        private void countLatencies() {
            latencyRecords = 0;
            latencyRanges = new ArrayList<>();
            for (int i = 0, cur = 0; i < latencies.length; i++) {
                if (latencies[i] > 0) {
                    latencyRanges.add(new int[]{cur, cur + latencies[i], i});
                    cur += latencies[i] + 1;
                    totalLatency += i * latencies[i];
                    latencyRecords += latencies[i];
                    maxLatency = i;
                }
            }
        }

        private int[] getPercentiles() {
            int[] percentileIds = new int[percentiles.length];
            int[] values = new int[percentileIds.length];
            int index = 0;

            for (int i = 0; i < percentiles.length; i++) {
                percentileIds[i] = (int) (latencyRecords * percentiles[i]);
            }

            for (int[] lr : latencyRanges) {
                while ((index < percentileIds.length) &&
                        (lr[0] <= percentileIds[index]) && (percentileIds[index] <= lr[1])) {
                    values[index++] = lr[2];
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
            if (latency  < latencies.length && latency > -1) {
                latencies[latency] += events;
            } else {
                discard += events;
            }
            this.records += events;
            this.bytes += bytes;
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
            countLatencies();
            final double elapsed = (endTime - startTime) / 1000.0;
            final double recsPerSec = records / elapsed;
            final double mbPerSec = (this.bytes / (1024.0 * 1024.0)) / elapsed;
            int[] percs = getPercentiles();

            logger.print(action, bytes, records, recsPerSec, mbPerSec, totalLatency / (double) latencyRecords,
                    maxLatency, discard, percs[0], percs[1], percs[2], percs[3],
                    percs[4], percs[5], percs[6], percs[7]);
        }
    }

    @NotThreadSafe
    final static private class TimeWindow extends LatencyWriter {
        final private ElasticCounter idleCounter;
        final private int windowInterval;
        final private int minIdleNS;

        private TimeWindow(String action, long start, int latencyThreshold, int interval, int minIdleNS, int idleNS) {
            super(action, start, latencyThreshold);
            this.idleCounter = new ElasticCounter(interval, idleNS);
            this.minIdleNS = minIdleNS;
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

        private void minWait() {
            LockSupport.parkNanos(minIdleNS);
        }
    }


    @NotThreadSafe
    static private class CSVLatencyWriter extends LatencyWriter {
        final private String csvFile;
        final private CSVPrinter csvPrinter;

        CSVLatencyWriter(String action, long start,  int latencyThreshold,  String latencyName, String csvFile) throws IOException {
            super(action, start, latencyThreshold);
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

    static final class TimeRecorder implements RecordTime {
        final private ConcurrentLinkedQueue<TimeStamp>[] queue;

        TimeRecorder(ConcurrentLinkedQueue<TimeStamp>[] queue) {
            this.queue = queue;
        }

        @Override
        public void accept(int id, long startTime, long endTime, int bytes, int records) {
            queue[id].add(new TimeStamp(startTime, endTime, bytes, records));
        }
    }

    @Override
    @Synchronized
    public RecordTime get() {
        if (size >= cQueues.length) {
            return null;
        }
        for (int i = 0; i < cQueues[size].length; i++) {
            cQueues[size][i] = new ConcurrentLinkedQueue<>();
        }
        size += 1;
        return new TimeRecorder(cQueues[size-1]);
    }


    @Override
    @Synchronized
    public CompletableFuture<Void> start(long startTime) {
        if (this.ret == null) {
            this.ret = CompletableFuture.runAsync(new QueueProcessor(startTime), executor);
        }
        return this.ret;
    }

    @Override
    @Synchronized
    public void stop(long endTime)  {
        if (this.ret != null) {
            if (cQueues.length > 0) {
                for (ConcurrentLinkedQueue<TimeStamp>[] queues : cQueues) {
                    queues[0].add(new TimeStamp(endTime));
                }
                try {
                    ret.get();
                } catch (ExecutionException | InterruptedException ex) {
                    ex.printStackTrace();
                }
                for (ConcurrentLinkedQueue<TimeStamp>[] queues : cQueues) {
                    for (ConcurrentLinkedQueue<TimeStamp> queue : queues) {
                        queue.clear();
                    }
                }
            }
            this.ret = null;
        }
    }
}