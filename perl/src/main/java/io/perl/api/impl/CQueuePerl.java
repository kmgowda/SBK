/**
 * Copyright (c) KMG. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 */
package io.perl.api.impl;

import io.perl.api.Channel;
import io.perl.api.PeriodicLogger;
import io.perl.api.Perl;
import io.perl.api.PerlChannel;
import io.perl.config.PerlConfig;
import io.perl.system.PerlPrinter;
import io.perl.api.TimeStamp;
import io.state.State;
import io.time.Time;
import lombok.Synchronized;
import org.jetbrains.annotations.NotNull;

import javax.annotation.concurrent.GuardedBy;
import javax.annotation.concurrent.NotThreadSafe;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;


/**
 * Class for Performance statistics.
 */
final public class CQueuePerl implements Perl {
    final private PerformanceRecorder perlReceiver;
    final private Channel[] channels;
    final private Time time;
    final private ExecutorService executor;
    final private CompletableFuture<Void> retFuture;

    @GuardedBy("this")
    private int index;

    @GuardedBy("this")
    private State state;

    @GuardedBy("this")
    private CompletableFuture<Void> qFuture;


    public CQueuePerl(@NotNull PerlConfig perlConfig, PeriodicLogger periodicRecorder,
                      int reportingIntervalMS, Time time, ExecutorService executor) {
        int maxQs;
        this.time = time;
        this.executor = executor;
        this.retFuture = new CompletableFuture<>();
        this.state = State.BEGIN;
        if (perlConfig.maxQs > 0) {
            maxQs = perlConfig.maxQs;
            this.index = 1;
        } else {
            maxQs = Math.max(PerlConfig.MIN_Q_PER_WORKER, perlConfig.qPerWorker);
            this.index = Math.max(perlConfig.workers, PerlConfig.MIN_WORKERS);
        }
        this.channels = new CQueueChannel[this.index];
        for (int i = 0; i < channels.length; i++) {
            channels[i] = new CQueueChannel(maxQs, new OnError());
        }
        this.perlReceiver = new PerformanceRecorder(periodicRecorder, channels, time, reportingIntervalMS,
                Math.max(PerlConfig.MIN_IDLE_NS, perlConfig.idleNS));
    }


    @Override
    @Synchronized
    public PerlChannel getPerlChannel() {
        if (channels.length == 1) {
            return channels[0].getPerlChannel();
        }
        index += 1;
        if (index >= channels.length) {
            index = 0;
        }
        return channels[index].getPerlChannel();
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
                PerlPrinter.log.warn("CQueuePerl Shutdown with Exception:" + ex);
                retFuture.completeExceptionally(ex);
            } else {
                PerlPrinter.log.info("CQueuePerl Shutdown");
                retFuture.complete(null);
            }
        }
    }

    @Override
    @Synchronized
    public CompletableFuture<Void> run(long secondsToRun, long recordsCount) {
        if (state == State.BEGIN) {
            state = State.RUN;
            PerlPrinter.log.info("CQueuePerl Start");
            qFuture = CompletableFuture.runAsync(() -> perlReceiver.run(secondsToRun, recordsCount),
                    executor);
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


    @NotThreadSafe
    static final class CQueueChannel implements Channel {
        final private ConcurrentLinkedQueue<TimeStamp>[] cQueues;
        final private Throw eThrow;
        private int rIndex;

        public CQueueChannel(int qSize, Throw eThrow) {
            this.rIndex = qSize;
            this.eThrow = eThrow;
            this.cQueues = new ConcurrentLinkedQueue[qSize];
            for (int i = 0; i < cQueues.length; i++) {
                cQueues[i] = new ConcurrentLinkedQueue<>();
            }
        }

        public TimeStamp receive(int timeout) {
            rIndex += 1;
            if (rIndex >= cQueues.length) {
                rIndex = 0;
            }
            return cQueues[rIndex].poll();
        }

        public void sendEndTime(long endTime) {
            cQueues[0].add(new TimeStamp(endTime));
        }

        public void clear() {
            for (ConcurrentLinkedQueue<TimeStamp> q : cQueues) {
                q.clear();
            }
        }

        public PerlChannel getPerlChannel() {
            return new CQueuePerlChannel();
        }

        public void sendException(int id, Throwable ex) {
            eThrow.onException(ex);
        }

        @NotThreadSafe
        private final class CQueuePerlChannel implements PerlChannel {
            private int wIndex;

            public CQueuePerlChannel() {
                this.wIndex = 0;
            }

            @Override
            public void send(long startTime, long endTime, int dataSize, int records) {
                this.wIndex += 1;
                if (this.wIndex >= cQueues.length) {
                    this.wIndex = 0;
                }
                cQueues[wIndex].add(new TimeStamp(startTime, endTime, dataSize, records));
            }

            @Override
            public void sendException(Throwable ex) {
                eThrow.onException(ex);
            }
        }

    }

    final private class OnError implements Throw {
        public void onException(Throwable ex) {
            shutdown(ex);
        }
    }
}