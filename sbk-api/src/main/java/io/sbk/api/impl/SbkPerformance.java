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
import io.sbk.api.Logger;
import io.sbk.api.Performance;
import io.sbk.api.Print;
import io.sbk.api.SendChannel;
import io.sbk.api.Time;
import io.sbk.api.TimeStamp;
import io.sbk.api.Channel;
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
    final private String csvFile;
    final private int windowInterval;
    final private int idleNS;
    final private int baseLatency;
    final private int maxWindowLatency;
    final private int maxLatency;
    final private Time time;
    final private Logger logger;
    final private ExecutorService executor;
    final private Channel[] channels;
    final private double[] percentiles;

    @GuardedBy("this")
    private int index;

    @GuardedBy("this")
    private CompletableFuture<Void> retFuture;

    @GuardedBy("this")
    private CompletableFuture<Void> qFuture;


    public SbkPerformance(Config config, int workers, Logger periodicLogger,
                          Time time, ExecutorService executor, String csvFile) {
        this.idleNS = Math.max(Config.MIN_IDLE_NS, config.idleNS);
        this.baseLatency = Math.max(Config.DEFAULT_MIN_LATENCY, periodicLogger.getMinLatency());
        this.maxWindowLatency = Math.min(Integer.MAX_VALUE,
                                    Math.max(periodicLogger.getMaxWindowLatency(), Config.DEFAULT_WINDOW_LATENCY));
        this.maxLatency = Math.min(Integer.MAX_VALUE,
                                    Math.max(periodicLogger.getMaxLatency(), Config.DEFAULT_MAX_LATENCY));
        this.windowInterval = periodicLogger.getReportingIntervalSeconds() * Config.MS_PER_SEC;
        this.csvFile = csvFile;
        this.time = time;
        this.logger = periodicLogger;
        this.executor = executor;
        this.retFuture = null;
        int maxQs;
        if (config.maxQs > 0) {
            maxQs = config.maxQs;
            this.channels = new CQueueChannel[1];
            this.index = 1;
        } else {
            maxQs =  Math.max(Config.MIN_Q_PER_WORKER, config.qPerWorker);
            this.channels = new CQueueChannel[workers];
            this.index = workers;
        }
        for (int i = 0; i < channels.length; i++) {
            channels[i] = new CQueueChannel(maxQs, new OnError());
        }
        double[] percentilesIndices = periodicLogger.getPercentileIndices();
        this.percentiles = new double[percentilesIndices.length];
        for (int i = 0; i < percentiles.length; i++) {
            this.percentiles[i] = percentilesIndices[i] / 100.0;
        }
    }

    /**
     * Private class for start and end time.
     */
    final private class QueueProcessor implements Runnable {
        final private long msToRun;
        final private long totalRecords;
        final private double[] percentiles;

        private QueueProcessor(long secondsToRun, long records, double[] percentiles) {
            this.msToRun = secondsToRun * Config.MS_PER_SEC;
            this.totalRecords = records;
            this.percentiles = percentiles;
        }

        public void run() {
            final TimeWindow window;
            final ArrayLatencyWindow latencyWindow;
            final long startTime = time.getCurrentTime();
            boolean doWork = true;
            long ctime = startTime;
            long recordsCnt = 0;
            boolean notFound;
            TimeStamp t;

            if (csvFile != null) {
                try {
                    latencyWindow = new CSVArrayLatencyWriter(baseLatency, maxLatency, percentiles, time, startTime,
                            csvFile, time.getTimeUnit().toString());
                } catch (IOException ex) {
                    ex.printStackTrace();
                    return;
                }
            } else {
                latencyWindow = new ArrayLatencyWindow(baseLatency, maxLatency, percentiles, time, startTime);
            }
            window = new TimeWindow(baseLatency, maxWindowLatency, percentiles, time, startTime, windowInterval, idleNS);
            while (doWork) {
                notFound = true;
                for (int i = 0; doWork && (i < channels.length); i++) {
                    t = channels[i].receive(windowInterval);
                    if (t != null) {
                        notFound = false;
                        ctime = t.endTime;
                        if (t.isEnd()) {
                            doWork = false;
                        } else {
                            recordsCnt += t.records;
                            final long latency = t.endTime - t.startTime;
                            window.record(t.startTime, t.bytes, t.records, latency);
                            latencyWindow.record(t.startTime, t.bytes, t.records, latency);
                            if (totalRecords > 0  && recordsCnt >= totalRecords) {
                                doWork = false;
                            }
                            if (msToRun > 0 && time.elapsedMilliSeconds(ctime, startTime) >= msToRun) {
                                doWork = false;
                            }
                        }
                        if ((window.elapsedTimeMS(ctime) > windowInterval) || (window.isOverflow())) {
                            window.print(ctime, logger);
                            window.reset(ctime);
                        }
                    }
                }
                if (doWork) {
                    if (notFound) {
                        ctime = window.idleWaitPrint(ctime, logger);
                    }
                    if (msToRun > 0 && time.elapsedMilliSeconds(ctime, startTime) >= msToRun) {
                        doWork = false;
                    }
                    if (latencyWindow.isOverflow()) {
                        latencyWindow.print(ctime, logger::printTotal);
                    }
                }
            }
            window.printPendingData(ctime, logger);
            latencyWindow.print(ctime, logger::printTotal);
        }
    }

    /**
     * Private class for counter implementation to reduce time.getCurrentTime() invocation.
     */
    @NotThreadSafe
    final static private class ElasticCounter {
        final private int windowInterval;
        final private int idleNS;
        final private double countRatio;
        final private long minIdleCount;
        private long elasticCount;
        private long idleCount;
        private long totalCount;

        private ElasticCounter(int windowInterval, int idleNS) {
            this.windowInterval = windowInterval;
            this.idleNS = idleNS;
            double minWaitTimeMS = windowInterval / 50.0;
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
     *  Base class for Performance statistics.
     */
    @NotThreadSafe
    static private class LatencyRecorder {
        final public long lowLatency;
        final public long highLatency;
        public long validLatencyRecords;
        public long lowerLatencyDiscardRecords;
        public long higherLatencyDiscardRecords;
        public long invalidLatencyRecords;
        public long bytes;
        public long totalLatency;
        public long maxLatency;


        LatencyRecorder(long baseLatency, long latencyThreshold) {
            this.lowLatency = baseLatency;
            this.highLatency = latencyThreshold;
            reset();
        }

        public void reset() {
            this.validLatencyRecords = 0;
            this.lowerLatencyDiscardRecords = 0;
            this.higherLatencyDiscardRecords = 0;
            this.invalidLatencyRecords = 0;
            this.bytes = 0;
            this.maxLatency = 0;
            this.totalLatency = 0;
        }

        /**
         * is Overflow condition for this recorder
         *
         * @return isOverflow condition occurred or not
         */
        public boolean isOverflow() {
            return (this.totalLatency > Config.LONG_MAX) || (this.bytes > Config.LONG_MAX);
        }

        /**
         * Record the latency and return if the latecy is valid/not
         *
         * @param bytes number of bytes.
         * @param events number of events(records).
         * @param latency latency value in milliseconds.
         * @return is valid latency record or not
         */
        public boolean recordLatency(int bytes, int events, long latency) {
            this.bytes += bytes;
            this.maxLatency = Math.max(this.maxLatency, latency);
            if (latency < 0) {
                this.invalidLatencyRecords += events;
            } else {
                this.totalLatency +=  latency * events;
                if (latency < this.lowLatency) {
                    this.lowerLatencyDiscardRecords += events;
                } else if (latency > this.highLatency) {
                    this.higherLatencyDiscardRecords += events;
                } else {
                    this.validLatencyRecords += events;
                    return true;
                }
            }
            return false;
        }
    }

    /**
     *  class for Performance statistics.
     */
    @NotThreadSafe
    static private class ArrayLatencyRecorder extends LatencyRecorder {
        final private long[] latencies;

        ArrayLatencyRecorder(int baseLatency, int latencyThreshold) {
            super(baseLatency, latencyThreshold);
            this.latencies = new long[latencyThreshold-baseLatency];
            reset();
        }

        public long[] getPercentiles(final double[] percentiles) {
            final long[] values = new long[percentiles.length];
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
                            values[index] = i + lowLatency;
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
         * @param bytes number of bytes.
         * @param events number of events(records).
         * @param latency latency value in milliseconds.
         */
        public void record(int bytes, int events, long latency) {
            if (recordLatency(bytes, events, latency)) {
                final int index = (int) (latency - this.lowLatency);
                this.latencies[index] += events;
            }
        }
    }

    /**
     * Private class for Performance statistics within a given time window.
     */
    @NotThreadSafe
    static private class ArrayLatencyWindow extends ArrayLatencyRecorder {
        final public Time time;
        final private double[] percentiles;
        private long startTime;

        ArrayLatencyWindow(int baseLatency, int latencyThreshold, double[] percentiles, Time time, long start) {
            super(baseLatency, latencyThreshold);
            this.startTime = start;
            this.time = time;
            this.percentiles = percentiles;
        }

        public void reset(long start) {
            reset();
            this.startTime = start;
        }

        /**
         * Record the latency
         *
         * @param startTime start time.
         * @param bytes number of bytes.
         * @param events number of events(records).
         * @param latency latency value in milliseconds.
         */
        public void record(long startTime, int bytes, int events, long latency) {
            record(bytes, events, latency);
        }

        /**
         * Get the current time duration of this window
         *
         * @param currentTime current time.
         * @return elapsed Time in Milliseconds
         */
        public long elapsedTimeMS(long currentTime) {
            return (long) time.elapsedMilliSeconds(currentTime, startTime);
        }


        /**
         * print only if there is data recorded.
         *
         * @param time current time.
         */
        public void printPendingData(long time,  Logger logger) {
            if (this.validLatencyRecords > 0 || this.lowerLatencyDiscardRecords > 0 ||
                    this.higherLatencyDiscardRecords > 0 || this.invalidLatencyRecords > 0) {
                print(time, logger);
            }
        }

        /**
         * Print the window statistics
         */
        public void print(long endTime, Print logger) {
            final double elapsedSec = Math.max(time.elapsedSeconds(endTime, startTime), 1.0);
            final long totalLatencyRecords  = this.validLatencyRecords +
                    this.lowerLatencyDiscardRecords + this.higherLatencyDiscardRecords;
            final long totalRecords = totalLatencyRecords + this.invalidLatencyRecords;
            final double recsPerSec = totalRecords / elapsedSec;
            final double mbPerSec = (this.bytes / (1024.0 * 1024.0)) / elapsedSec;
            final double avgLatency = this.totalLatency / (double) totalLatencyRecords;
            long[] pecs = getPercentiles(percentiles);
            logger.print(this.bytes, totalRecords, recsPerSec, mbPerSec,
                    avgLatency, this.maxLatency, this.invalidLatencyRecords,
                    this.lowerLatencyDiscardRecords, this.higherLatencyDiscardRecords,
                    pecs);
        }
    }

    @NotThreadSafe
    final static private class TimeWindow extends ArrayLatencyWindow {
        final private ElasticCounter idleCounter;
        final private int windowInterval;

        private TimeWindow(int baseLatency, int latencyThreshold, double[] percentiles, Time time,
                           long start, int interval, int idleNS) {
            super(baseLatency, latencyThreshold, percentiles, time, start);
            this.idleCounter = new ElasticCounter(interval, idleNS);
            this.windowInterval = interval;
        }

        @Override
        public void reset(long start) {
            super.reset(start);
            this.idleCounter.reset();
        }

        private long waitCheckPrint(ElasticCounter counter, long ctime, Print logger) {
            if (counter.waitCheck()) {
                ctime = time.getCurrentTime();
                final long diffTime = elapsedTimeMS(ctime);
                if (diffTime > windowInterval) {
                    print(ctime, logger);
                    reset(ctime);
                    counter.setElastic(diffTime);
                } else {
                    counter.updateElastic(diffTime);
                }
            }
            return ctime;
        }

        private long  idleWaitPrint(long currentTime, Print logger) {
                return waitCheckPrint(idleCounter, currentTime, logger);
        }
    }


    @NotThreadSafe
    static private class CSVArrayLatencyWriter extends ArrayLatencyWindow {
        final private String csvFile;
        final private CSVPrinter csvPrinter;

        CSVArrayLatencyWriter(int baseLatency, int latencyThreshold, double[] percentiles, Time time, long start,
                              String csvFile, String unitString) throws IOException {
            super(baseLatency, latencyThreshold, percentiles, time, start);
            this.csvFile = csvFile;
            csvPrinter = new CSVPrinter(Files.newBufferedWriter(Paths.get(csvFile)), CSVFormat.DEFAULT
                    .withHeader("Start Time (" + unitString + ")", "data size (bytes)", "Records", " Latency (" + unitString + ")"));
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
        public void record(long startTime, int bytes, int events, long latency) {
            try {
                csvPrinter.printRecord(startTime, bytes, events, latency);
            } catch (IOException ex) {
                throw new RuntimeException(ex);
            }
        }

        @Override
        public void print(long endTime, Print logger) {
            try {
                csvPrinter.close();
            } catch (IOException ex) {
                throw new RuntimeException(ex);
            }
            readCSV();
            super.print(endTime, logger);
        }
    }

    interface Throw {
        void onException(Throwable ex);
    }




    @NotThreadSafe
    static final class CQueueChannel implements Channel {
        final private ConcurrentLinkedQueue<TimeStamp>[] cQueues;
        final private Throw eThrow;
        private int index;

        public CQueueChannel(int qSize, Throw eThrow) {
            this.index = qSize;
            this.eThrow = eThrow;
            this.cQueues = new ConcurrentLinkedQueue[qSize];
            for (int i = 0; i < cQueues.length; i++) {
                cQueues[i] = new ConcurrentLinkedQueue<>();
            }
        }

        public TimeStamp receive(int timeout) {
            index += 1;
            if (index >= cQueues.length) {
                index = 0;
            }
            return cQueues[index].poll();
        }

        public void sendEndTime(long endTime) {
            cQueues[0].add(new TimeStamp(endTime));
        }

        public void clear() {
            for (ConcurrentLinkedQueue<TimeStamp> q: cQueues) {
                q.clear();
            }
        }

        /* This Method is Thread Safe */
        public void send(int id, long startTime, long endTime, int bytes, int records) {
            cQueues[id].add(new TimeStamp(startTime, endTime, bytes, records));
        }

        public void sendException(int id, Throwable ex) {
            eThrow.onException(ex);
        }
    }

    @Override
    @Synchronized
    public SendChannel get() {
        if (channels.length == 1) {
                return channels[0];
        }
        index += 1;
        if (index >= channels.length) {
            index = 0;
        }
        return  channels[index];
    }

    final private class OnError implements Throw {
        public void onException(Throwable ex) {
            shutdown(ex);
        }
    }

    @Synchronized
    private void shutdown(Throwable ex) {
        if (retFuture == null) {
            return;
        }

        if (retFuture.isDone()) {
            retFuture = null;
            return;
        }

        if (qFuture != null) {
            if (!qFuture.isDone()) {
                long endTime = time.getCurrentTime();
                for (Channel ch : channels) {
                    ch.sendEndTime(endTime);
                }
                try {
                    qFuture.get();
                } catch (ExecutionException | InterruptedException e) {
                    e.printStackTrace();
                }
                for (Channel ch : channels) {
                    ch.clear();
                }
            }
            qFuture = null;
        }
        if (ex != null) {
            SbkLogger.log.warn("SBK Performance Shutdown with Exception:" + ex.toString());
            retFuture.completeExceptionally(ex);
        } else  {
            SbkLogger.log.info("SBK Performance Shutdown" );
            retFuture.complete(null);
        }
        retFuture = null;
    }


    @Override
    @Synchronized
    public CompletableFuture<Void> start(long secondsToRun, long records) {
        if (retFuture == null) {
            retFuture = new CompletableFuture<>();
            qFuture =  CompletableFuture.runAsync(new QueueProcessor(secondsToRun,
                    records, percentiles),
                    executor);
            qFuture.whenComplete((ret, ex) -> {
                shutdown(ex);
            });
        }
        return retFuture;
    }

    @Override
    public void stop()  {
            shutdown(null);
    }
}