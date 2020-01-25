/**
 * Copyright (c) 2020 KMG. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 */
package io.sbk.api;

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
        final private long startTime;

        private QueueProcessor(long startTime) {
            this.startTime = startTime;
        }

        public Void call() throws IOException {
            final TimeWindow window = new TimeWindow(action, startTime, windowInterval);
            final LatencyWriter latencyRecorder = csvFile == null ? new LatencyWriter(action, messageSize, startTime) :
                    new CSVLatencyWriter(action, messageSize, startTime, csvFile);
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
                        window.record(t.bytes, t.records, latency);
                        latencyRecorder.record(t.startTime, t.bytes, t.records, latency);
                    }
                    time =  t.endTime;
                    if (window.windowTimeMS(time) > windowInterval) {
                        window.print(time, logger);
                        window.reset(time);
                    }
                } else {
                        window.busyWaitPrint(logger);
                }
            }
            latencyRecorder.printTotal(time, logger);
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
    final static private class TimeWindow {
        final private String action;
        final private ElasticCounter counter;
        final private int windowInterval;
        private long startTime;
        private long lastTime;
        private long count;
        private long bytes;
        private int maxLatency;
        private long totalLatency;

        private TimeWindow(String action, long start, int interval) {
            this.action = action;
            this.counter = new ElasticCounter(interval);
            this.windowInterval = interval;
            reset(start);
        }

        private void reset(long start) {
            this.startTime = start;
            this.lastTime = this.startTime;
            this.count = 0;
            this.bytes = 0;
            this.maxLatency = 0;
            this.totalLatency = 0;
            this.counter.reset();
        }

        /**
         * Record the latency and bytes
         *
         * @param bytes   number of bytes.
         * @param latency latency in ms.
         */
        private void record(long bytes, int records, int latency) {
            this.count += records;
            this.totalLatency += latency;
            this.bytes += bytes;
            this.maxLatency = Math.max(this.maxLatency, latency);
        }

        /**
         * Print the window statistics
         */
        private void print(long time, ResultLogger logger) {
            this.lastTime = time;
            assert this.lastTime > this.startTime : "Invalid Start and EndTime";
            final double elapsed = (this.lastTime - this.startTime) / 1000.0;
            final double recsPerSec = count / elapsed;
            final double mbPerSec = (this.bytes / (1024.0 * 1024.0)) / elapsed;

            logger.print(action, count, recsPerSec, mbPerSec, totalLatency / (double) count, maxLatency);
        }

        /**
         * Get the current time duration of this window
         *
         * @param time current time.
         */
        private long windowTimeMS(long time) {
            return time - startTime;
        }

        private void busyWaitPrint(ResultLogger logger) {
            if (counter.canPrint()) {
                final long time = System.currentTimeMillis();
                final long diffTime = windowTimeMS(time);
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
    static private class LatencyWriter {
        final static int MS_PER_SEC = 1000;
        final static int MS_PER_MIN = MS_PER_SEC * 60;
        final static int MS_PER_HR = MS_PER_MIN * 60;
        final double[] percentiles = {0.5, 0.75, 0.95, 0.99, 0.999, 0.9999};
        final String action;
        final int messageSize;
        final long startTime;
        final int[] latencies;
        int discard;
        long count;
        long totalLatency;
        long maxLatency;
        long totalBytes;
        ArrayList<int[]> latencyRanges;

        LatencyWriter(String action, int messageSize, long startTime) {
            this.action = action;
            this.messageSize = messageSize;
            this.startTime = startTime;
            this.latencies = new int[MS_PER_HR];
            this.discard = 0;
            this.latencyRanges = null;
            this.totalLatency = 0;
            this.maxLatency = 0;
            this.count = 0;
        }

        private void countLatencies() {
            count = 0;
            latencyRanges = new ArrayList<>();
            for (int i = 0, cur = 0; i < latencies.length; i++) {
                if (latencies[i] > 0) {
                    latencyRanges.add(new int[]{cur, cur + latencies[i], i});
                    cur += latencies[i] + 1;
                    totalLatency += i * latencies[i];
                    count += latencies[i];
                    maxLatency = i;
                }
            }
        }

        private int[] getPercentiles() {
            int[] percentileIds = new int[percentiles.length];
            int[] values = new int[percentileIds.length];
            int index = 0;

            for (int i = 0; i < percentiles.length; i++) {
                percentileIds[i] = (int) (count * percentiles[i]);
            }

            for (int[] lr : latencyRanges) {
                while ((index < percentileIds.length) &&
                        (lr[0] <= percentileIds[index]) && (percentileIds[index] <= lr[1])) {
                    values[index++] = lr[2];
                }
            }
            return values;
        }

        public void record(int bytes, int events, int latency) {
            if (latency  < latencies.length) {
                totalBytes += bytes;
                latencies[latency] += events;
            } else {
                discard++;
            }
        }

        public void record(long start, int bytes, int events, int latency) {
            this.record(bytes, events, latency);
        }

        public void printTotal(long endTime, ResultLogger logger) {
            countLatencies();
            final double elapsed = (endTime - startTime) / 1000.0;
            final double recsPerSec = count / elapsed;
            final double mbPerSec = (this.totalBytes / (1024.0 * 1024.0)) / elapsed;
            int[] percs = getPercentiles();

            logger.print(action+" (Total) ", count, recsPerSec, mbPerSec, totalLatency / (double) count, maxLatency);
            logger.printLatencies(action +" Latencies", percs[0], percs[1], percs[2], percs[3], percs[4], percs[5]);
            logger.printDiscardedLatencies(action + "Discarded Latencies:", discard);
        }
    }

    @NotThreadSafe
    static private class CSVLatencyWriter extends LatencyWriter {
        final private String csvFile;
        final private CSVPrinter csvPrinter;

        CSVLatencyWriter(String action, int messageSize, long start, String csvFile) throws IOException {
            super(action, messageSize, start);
            this.csvFile = csvFile;
            csvPrinter = new CSVPrinter(Files.newBufferedWriter(Paths.get(csvFile)), CSVFormat.DEFAULT
                    .withHeader("Start Time (Milliseconds)", "data size (bytes)", "Records", action + " Latency (Milliseconds)"));
        }

        private void readCSV() {
            try {
                CSVParser csvParser = new CSVParser(Files.newBufferedReader(Paths.get(csvFile)), CSVFormat.DEFAULT
                        .withFirstRecordAsHeader().withIgnoreHeaderCase().withTrim());

                for (CSVRecord csvEntry : csvParser) {
                    record(Integer.parseInt(csvEntry.get(1)), Integer.parseInt(csvEntry.get(2)), Integer.parseInt(csvEntry.get(3)));
                }
                csvParser.close();
            } catch (IOException ex) {
                throw new RuntimeException(ex);
            }
        }

        @Override
        public void record(long start, int bytes, int events, int latency) {
            try {
                csvPrinter.printRecord(start, bytes, events, latency);
            } catch (IOException ex) {
                throw new RuntimeException(ex);
            }
        }

        @Override
        public void printTotal(long endTime, ResultLogger logger) {
            try {
                csvPrinter.close();
            } catch (IOException ex) {
                throw new RuntimeException(ex);
            }
            readCSV();
            super.printTotal(endTime, logger);
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