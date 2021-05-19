/**
 * Copyright (c) KMG. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 */
package io.sbk.perl.impl;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.locks.LockSupport;

import io.sbk.perl.PerlConfig;
import io.sbk.system.Printer;
import io.sbk.perl.Performance;
import io.sbk.perl.PeriodicLatencyRecorder;
import io.sbk.perl.SendChannel;
import io.sbk.perl.Time;
import io.sbk.perl.TimeStamp;
import io.sbk.perl.Channel;
import lombok.Synchronized;

import javax.annotation.concurrent.GuardedBy;
import javax.annotation.concurrent.NotThreadSafe;


/**
 * Class for Performance statistics.
 */
final public class CQueuePerformance implements Performance {
    final private int windowIntervalMS;
    final private int idleNS;
    final private Time time;
    final private PeriodicLatencyRecorder latencyLogger;
    final private ExecutorService executor;
    final private Channel[] channels;

    @GuardedBy("this")
    private int index;

    @GuardedBy("this")
    private CompletableFuture<Void> retFuture;

    @GuardedBy("this")
    private CompletableFuture<Void> qFuture;


    public CQueuePerformance(PerlConfig perlConfig, int workers, PeriodicLatencyRecorder periodicLogger,
                             int reportingIntervalMS, Time time, ExecutorService executor) {
        this.idleNS = Math.max(PerlConfig.MIN_IDLE_NS, perlConfig.idleNS);
        this.windowIntervalMS = reportingIntervalMS;
        this.time = time;
        this.latencyLogger = periodicLogger;
        this.executor = executor;
        this.retFuture = null;
        int maxQs;
        if (perlConfig.maxQs > 0) {
            maxQs = perlConfig.maxQs;
            this.channels = new CQueueChannel[1];
            this.index = 1;
        } else {
            maxQs =  Math.max(PerlConfig.MIN_Q_PER_WORKER, perlConfig.qPerWorker);
            this.channels = new CQueueChannel[workers];
            this.index = workers;
        }
        for (int i = 0; i < channels.length; i++) {
            channels[i] = new CQueueChannel(maxQs, new OnError());
        }
    }

    /**
     * Private class for start and end time.
     */
    final private class QueueProcessor implements Runnable {
        final private long msToRun;
        final private long totalRecords;

        private QueueProcessor(long secondsToRun, long recordsCount) {
            this.msToRun = secondsToRun * PerlConfig.MS_PER_SEC;
            this.totalRecords = recordsCount;
        }

        public void run() {
            final ElasticWaitCounter idleCounter = new ElasticWaitCounter(windowIntervalMS, idleNS);
            final long startTime = time.getCurrentTime();
            boolean doWork = true;
            long ctime = startTime;
            long recordsCnt = 0;
            boolean notFound;
            TimeStamp t;
            Printer.log.info("Performance Logger Started" );
            latencyLogger.start(startTime);
            while (doWork) {
                notFound = true;
                for (int i = 0; doWork && (i < channels.length); i++) {
                    t = channels[i].receive(windowIntervalMS);
                    if (t != null) {
                        notFound = false;
                        ctime = t.endTime;
                        if (t.isEnd()) {
                            doWork = false;
                        } else {
                            recordsCnt += t.records;
                            final long latency = t.endTime - t.startTime;
                            latencyLogger.record(t.startTime, t.bytes, t.records, latency);
                            if (totalRecords > 0  && recordsCnt >= totalRecords) {
                                doWork = false;
                            }
                            if (msToRun > 0 && time.elapsedMilliSeconds(ctime, startTime) >= msToRun) {
                                doWork = false;
                            }
                        }
                        if (latencyLogger.elapsedMilliSeconds(ctime) > windowIntervalMS) {
                            latencyLogger.print(ctime);
                            latencyLogger.resetWindow(ctime);
                            idleCounter.reset();
                        }
                    }
                }
                if (doWork) {
                    if (notFound) {
                        if (idleCounter.waitAndCheck()) {
                            ctime = time.getCurrentTime();
                            final long diffTime = latencyLogger.elapsedMilliSeconds(ctime);
                            if (diffTime > windowIntervalMS) {
                                latencyLogger.print(ctime);
                                latencyLogger.resetWindow(ctime);
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
                }
            }
            latencyLogger.printTotal(ctime);
        }
    }

    /**
     * Private class for counter implementation to reduce time.getCurrentTime() invocation.
     */
    @NotThreadSafe
    final static private class ElasticWaitCounter {
        final private int windowInterval;
        final private int idleNS;
        final private double countRatio;
        final private long minIdleCount;
        private long elasticCount;
        private long idleCount;
        private long totalCount;

        public ElasticWaitCounter(int windowInterval, int idleNS) {
            this.windowInterval = windowInterval;
            this.idleNS = idleNS;
            double minWaitTimeMS = windowInterval / 50.0;
            countRatio = (PerlConfig.NS_PER_MS * 1.0) / idleNS;
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
    public SendChannel getSendChannel() {
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
            Printer.log.warn("Performance Logger Shutdown with Exception:" + ex.toString());
            retFuture.completeExceptionally(ex);
        } else  {
            Printer.log.info("Performance Logger Shutdown" );
            retFuture.complete(null);
        }
        retFuture = null;
    }


    @Override
    @Synchronized
    public CompletableFuture<Void> run(long secondsToRun, long recordsCount) {
        if (retFuture == null) {
            retFuture = new CompletableFuture<>();
            qFuture =  CompletableFuture.runAsync(new QueueProcessor(secondsToRun,
                            recordsCount),
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