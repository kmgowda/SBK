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

import io.sbk.api.Config;
import io.sbk.api.Logger;
import io.sbk.api.Performance;
import io.sbk.api.SendChannel;
import io.sbk.api.Time;
import io.sbk.api.TimeStamp;
import io.sbk.api.Channel;
import lombok.Synchronized;

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
            final LatencyWindow window;
            final LatencyWindow totalWindow;
            final ElasticCounter idleCounter = new ElasticCounter(windowInterval, idleNS);
            final long startTime = time.getCurrentTime();
            boolean doWork = true;
            long ctime = startTime;
            long recordsCnt = 0;
            boolean notFound;
            TimeStamp t;

            if (csvFile != null) {
                try {
                    totalWindow = new CSVArrayLatencyWriter(baseLatency, maxLatency, percentiles, time, startTime,
                            csvFile, time.getTimeUnit().toString());
                } catch (IOException ex) {
                    ex.printStackTrace();
                    return;
                }
            } else {
                totalWindow = new ArrayLatencyRecorder(baseLatency, maxLatency, percentiles, time, startTime);
            }
            window = new ArrayLatencyRecorder(baseLatency, maxLatency, percentiles, time, startTime);
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
                            totalWindow.record(t.startTime, t.bytes, t.records, latency);
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
                            idleCounter.reset();
                        }
                    }
                }
                if (doWork) {
                    if (notFound) {
                        if (idleCounter.waitAndCheck()) {
                            ctime = time.getCurrentTime();
                            final long diffTime = window.elapsedTimeMS(ctime);
                            if (diffTime > windowInterval) {
                                window.print(ctime, logger);
                                window.reset(ctime);
                                idleCounter.reset();
                                idleCounter.setElastic(diffTime);
                            } else {
                                idleCounter.updateElastic(diffTime);
                            }
                        }
                    }
                    if (msToRun > 0 && time.elapsedMilliSeconds(ctime, startTime) >= msToRun) {
                        doWork = false;
                    }
                    if (totalWindow.isOverflow()) {
                        totalWindow.print(ctime, logger::printTotal);
                    }
                }
            }
            window.printPendingData(ctime, logger);
            totalWindow.print(ctime, logger::printTotal);
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

        public ElasticCounter(int windowInterval, int idleNS) {
            this.windowInterval = windowInterval;
            this.idleNS = idleNS;
            double minWaitTimeMS = windowInterval / 50.0;
            countRatio = (Config.NS_PER_MS * 1.0) / idleNS;
            minIdleCount = (long) (countRatio * minWaitTimeMS);
            elasticCount = minIdleCount;
            idleCount = 0;
            totalCount = 0;
        }

        public boolean waitAndCheck() {
            LockSupport.parkNanos(idleNS);
            idleCount++;
            totalCount++;
            return idleCount > elasticCount;
        }

        public void reset() {
            idleCount = 0;
        }

        public void updateElastic(long diffTime) {
            elasticCount = Math.max((long) (countRatio * (windowInterval - diffTime)), minIdleCount);
        }

        public void setElastic(long diffTime) {
            elasticCount =  (totalCount * windowInterval) / diffTime;
            totalCount = 0;
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