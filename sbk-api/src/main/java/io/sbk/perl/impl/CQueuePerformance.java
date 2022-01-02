/**
 * Copyright (c) KMG. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 */
package io.sbk.perl.impl;

import io.sbk.config.PerlConfig;
import io.sbk.perl.Channel;
import io.sbk.perl.Performance;
import io.sbk.perl.PeriodicRecorder;
import io.sbk.perl.SendChannel;
import io.sbk.perl.TimeStamp;
import io.sbk.state.State;
import io.sbk.system.Printer;
import io.sbk.time.Time;
import lombok.Synchronized;
import org.jetbrains.annotations.NotNull;

import javax.annotation.concurrent.GuardedBy;
import javax.annotation.concurrent.NotThreadSafe;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.locks.LockSupport;


/**
 * Class for Performance statistics.
 */
final public class CQueuePerformance implements Performance {
    final private int windowIntervalMS;
    final private int idleNS;
    final private int timeoutMS;
    final private Time time;
    final private PeriodicRecorder periodicLogger;
    final private ExecutorService executor;
    final private Channel[] channels;
    final private CompletableFuture<Void> retFuture;

    @GuardedBy("this")
    private int index;

    @GuardedBy("this")
    private State state;

    @GuardedBy("this")
    private CompletableFuture<Void> qFuture;


    public CQueuePerformance(@NotNull PerlConfig perlConfig, int workers, PeriodicRecorder periodicLogger,
                             int reportingIntervalMS, int timeoutMS, Time time, ExecutorService executor) {
        this.idleNS = Math.max(PerlConfig.MIN_IDLE_NS, perlConfig.idleNS);
        this.windowIntervalMS = reportingIntervalMS;
        this.timeoutMS = timeoutMS;
        this.time = time;
        this.periodicLogger = periodicLogger;
        this.executor = executor;
        this.retFuture = new CompletableFuture<>();
        this.state = State.BEGIN;
        int maxQs;
        if (perlConfig.maxQs > 0) {
            maxQs = perlConfig.maxQs;
            this.channels = new CQueueChannel[1];
            this.index = 1;
        } else {
            maxQs = Math.max(PerlConfig.MIN_Q_PER_WORKER, perlConfig.qPerWorker);
            this.channels = new CQueueChannel[workers];
            this.index = workers;
        }
        for (int i = 0; i < channels.length; i++) {
            channels[i] = new CQueueChannel(maxQs, new OnError());
        }
    }


    private void runPerformance(final long secondsToRun, final long totalRecords) {
        final long msToRun = secondsToRun * Time.MS_PER_SEC;
        final ElasticWaitCounter idleCounter = new ElasticWaitCounter(windowIntervalMS, timeoutMS, idleNS);
        final long startTime = time.getCurrentTime();
        boolean doWork = true;
        long ctime = startTime;
        long recordsCnt = 0;
        boolean notFound;
        TimeStamp t;
        Printer.log.info("Performance Logger Started");
        periodicLogger.start(startTime);
        periodicLogger.startWindow(startTime);
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
                        periodicLogger.record(t.startTime, t.endTime, t.bytes, t.records);
                        if (msToRun > 0) {
                            if (time.elapsedMilliSeconds(ctime, startTime) >= msToRun) {
                                doWork = false;
                            }
                        } else if (totalRecords > 0 && recordsCnt >= totalRecords) {
                            doWork = false;
                        }
                    }
                    if (periodicLogger.elapsedMilliSecondsWindow(ctime) > windowIntervalMS) {
                        periodicLogger.stopWindow(ctime);
                        periodicLogger.startWindow(ctime);
                        idleCounter.reset();
                    }
                }
            }
            if (doWork) {
                if (notFound) {
                    if (idleCounter.waitAndCheck()) {
                        ctime = time.getCurrentTime();
                        final long diffTime = periodicLogger.elapsedMilliSecondsWindow(ctime);
                        if (diffTime > windowIntervalMS) {
                            periodicLogger.stopWindow(ctime);
                            periodicLogger.startWindow(ctime);
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
        periodicLogger.stop(ctime);
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
        return channels[index];
    }

    @Synchronized
    private void shutdown(Throwable ex) {
        if (state != State.END) {
            state = State.END;
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
                Printer.log.warn("Performance Logger Shutdown with Exception:" + ex);
                retFuture.completeExceptionally(ex);
            } else {
                Printer.log.info("Performance Logger Shutdown");
                retFuture.complete(null);
            }
        }
    }

    @Override
    @Synchronized
    public CompletableFuture<Void> run(long secondsToRun, long recordsCount) {
        if (state == State.BEGIN) {
            state = State.RUN;
            qFuture = CompletableFuture.runAsync(() -> runPerformance(secondsToRun, recordsCount), executor);
            qFuture.whenComplete((ret, ex) -> {
                shutdown(ex);
            });
        }
        return retFuture;
    }

    @Override
    public void stop() {
        shutdown(null);
    }

    interface Throw {
        void onException(Throwable ex);
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

        public ElasticWaitCounter(int windowInterval, int timeoutMS, int idleNS) {
            this.windowInterval = windowInterval;
            this.idleNS = idleNS;
            countRatio = (Time.NS_PER_MS * 1.0) / this.idleNS;
            minIdleCount = (long) (countRatio * timeoutMS);
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
            elasticCount = (totalCount * windowInterval) / diffTime;
            totalCount = 0;
        }
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
            for (ConcurrentLinkedQueue<TimeStamp> q : cQueues) {
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

    final private class OnError implements Throw {
        public void onException(Throwable ex) {
            shutdown(ex);
        }
    }
}