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
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.locks.LockSupport;
import java.nio.file.Files;
import java.nio.file.Paths;

import io.sbk.api.Performance;
import io.sbk.api.ResultLogger;
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
    final private int messageSize;
    final private int windowInterval;
    final private ConcurrentLinkedQueue<TimeStamp> queue;
    final private ResultLogger logger;
    final private ExecutorService executor;

    @GuardedBy("this")
    private Future<Void> ret;

    public SbkPerformance(String action, int reportingInterval, int messageSize,
               String csvFile, ResultLogger logger, ExecutorService executor) {
        this.action = action;
        this.messageSize = messageSize;
        this.windowInterval = reportingInterval;
        this.csvFile = csvFile;
        this.logger = logger;
        this.executor = executor;
        this.queue = new ConcurrentLinkedQueue<>();
        this.ret = null;
    }

    /**
     * Private class for start and end time.
     */
    final static private class TimeStamp {
        final private long startTime;
        final private long endTime;
        final private int bytes;
        final private int records;

        private TimeStamp(long startTime, long endTime, int bytes, int records) {
            this.startTime = startTime;
            this.endTime = endTime;
            this.bytes = bytes;
            this.records = records;
        }

        private TimeStamp(long endTime) {
            this(-1, endTime, 0, 0);
        }

        private boolean isEnd() {
            return this.records == 0 && this.startTime == -1;
        }
    }

    /**
     * Private class for start and end time.
     */
    final private class QueueProcessor implements Callable {
        final static int MS_PER_SEC = 1000;
        final static int MS_PER_MIN = MS_PER_SEC * 60;
        final static int MS_PER_HR = MS_PER_MIN * 60;
        final private long startTime;

        private QueueProcessor(long startTime) {
            this.startTime = startTime;
        }

        public Void call() throws IOException {
            final TimeWindow window = new TimeWindow(action, startTime, MS_PER_MIN, windowInterval);
            final LatencyWriter latencyRecorder = csvFile == null ? new LatencyWriter(action+"(Total)", startTime, MS_PER_HR) :
                    new CSVLatencyWriter(action+"(Total)", startTime, MS_PER_HR, action, csvFile);
            boolean doWork = true;
            long time = startTime;
            TimeStamp t;

            while (doWork) {
                t = queue.poll();
                if (t != null) {
                    if (t.isEnd()) {
                        doWork = false;
                    } else {
                        final int latency = (int) (t.endTime - t.startTime);
                        window.record(startTime, t.bytes, t.records, latency);
                        latencyRecorder.record(t.startTime, t.bytes, t.records, latency);
                    }
                    time =  t.endTime;
                    if (window.elapsedTimeMS(time) > windowInterval) {
                        window.print(time, logger);
                        window.reset(time);
                    }
                } else {
                        window.busyWaitPrint(logger);
                }
            }
            latencyRecorder.print(time, logger);
            return null;
        }
    }

    /**
     * Private class for counter implementation to reduce System.currentTimeMillis() invocation.
     */
    @NotThreadSafe
    final static private class ElasticCounter {
        final private static int NS_PER_MICRO = 1000;
        final private static int MICROS_PER_MS = 1000;
        final private static int NS_PER_MS = NS_PER_MICRO * MICROS_PER_MS;
        final private static int PARK_NS = NS_PER_MS;
        final private int minWindowInterval;
        final private int minWaitTimeMS;
        final private long minIdleCount;
        private long elasticCount;
        private long idleCount;
        private long totalCount;

        private ElasticCounter(int interval) {
            minWindowInterval = interval;
            minWaitTimeMS = interval / 50;
            minIdleCount = (NS_PER_MS / PARK_NS) * minWaitTimeMS;
            elasticCount = minIdleCount;
            idleCount = 0;
            totalCount = 0;
        }

        private boolean canPrint() {
            LockSupport.parkNanos(PARK_NS);
            idleCount++;
            totalCount++;
            return idleCount > elasticCount;
        }

        private void reset() {
            idleCount = 0;
        }

        private void updateElastic(long diffTime) {
            elasticCount = Math.max((NS_PER_MS / PARK_NS) * (minWindowInterval - diffTime), minWaitTimeMS);
        }

        private void setElastic(long diffTime) {
            elasticCount =  (totalCount * minWindowInterval) / diffTime;
            totalCount = 0;
        }
    }


    /**
     * Private class for Performance statistics within a given time window.
     */
    @NotThreadSafe
    static private class LatencyWriter {
        final double[] percentiles = {0.5, 0.75, 0.95, 0.99, 0.999, 0.9999};
        final private String action;
        final private int latencyThreshold;
        private int[] latencies;
        private long startTime;
        private long records;
        private long bytes;
        private int maxLatency;
        private long totalLatency;
        private long discard;
        private ArrayList<int[]> latencyRanges;

        LatencyWriter(String action, long start, int latencyThreshold) {
            this.action = action;
            this.latencyThreshold = latencyThreshold;
            resetValues(start);
        }

        private void resetValues(long start) {
            this.startTime = start;
            this.records = 0;
            this.bytes = 0;
            this.maxLatency = 0;
            this.totalLatency = 0;
            this.discard = 0;
            this.latencyRanges = null;
            this.latencies = new int[latencyThreshold];
        }

        public void reset(long start) {
            resetValues(start);
        }

        private void countLatencies() {
            records = 0;
            latencyRanges = new ArrayList<>();
            for (int i = 0, cur = 0; i < latencies.length; i++) {
                if (latencies[i] > 0) {
                    latencyRanges.add(new int[]{cur, cur + latencies[i], i});
                    cur += latencies[i] + 1;
                    totalLatency += i * latencies[i];
                    records += latencies[i];
                    maxLatency = i;
                }
            }
        }

        private int[] getPercentiles() {
            int[] percentileIds = new int[percentiles.length];
            int[] values = new int[percentileIds.length];
            int index = 0;

            for (int i = 0; i < percentiles.length; i++) {
                percentileIds[i] = (int) (records * percentiles[i]);
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
                this.bytes += bytes;
                latencies[latency] += events;
            } else {
                discard++;
            }
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

            logger.print(action, records, recsPerSec, mbPerSec, totalLatency / (double) records, maxLatency,
                    discard, percs[0], percs[1], percs[2], percs[3], percs[4], percs[5]);
        }
    }

    @NotThreadSafe
    final static private class TimeWindow extends LatencyWriter {
        final private ElasticCounter counter;
        final private int windowInterval;

        private TimeWindow(String action, long start, int latencyThreshold, int interval) {
            super(action, start, latencyThreshold);
            this.counter = new ElasticCounter(interval);
            this.windowInterval = interval;
        }

        @Override
        public void reset(long start) {
            super.reset(start);
            this.counter.reset();
        }

        private void busyWaitPrint(ResultLogger logger) {
            if (counter.canPrint()) {
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

    @Override
    @Synchronized
    public void start(long startTime) {
        if (this.ret == null) {
            this.ret = executor.submit(new QueueProcessor(startTime));
        }
    }

    @Override
    @Synchronized
    public void shutdown(long endTime) throws ExecutionException, InterruptedException {
        if (this.ret != null) {
            queue.add(new TimeStamp(endTime));
            ret.get();
            queue.clear();
            this.ret = null;
        }
    }

    @Override
    public void recordTime(long startTime, long endTime, int bytes, int records) {
        queue.add(new TimeStamp(startTime, endTime, bytes, records));
    }
}