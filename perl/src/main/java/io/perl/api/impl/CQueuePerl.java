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
import io.perl.api.BenchmarkTermination;
import io.perl.api.PerformanceRecorder;
import io.perl.api.PeriodicRecorder;
import io.perl.api.Perl;
import io.perl.api.PerlChannel;
import io.perl.api.TimeStamp;
import io.perl.api.TimeStampNode;
import io.perl.config.PerlConfig;
import io.perl.system.PerlPrinter;
import io.state.State;
import io.time.Time;
import lombok.Synchronized;
import org.jetbrains.annotations.NotNull;

import javax.annotation.concurrent.GuardedBy;
import javax.annotation.concurrent.NotThreadSafe;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.locks.LockSupport;


/**
 * Class for Concurrent Queue based PerL.
 */
final public class CQueuePerl implements Perl {
    private static final long DRAIN_POLL_NANOS = TimeUnit.MILLISECONDS.toNanos(1);
    final private PerformanceRecorder perlReceiver;
    final private Channel[] channels;
    final private Time time;
    final private ExecutorService executor;
    final private CompletableFuture<Void> retFuture;
    final private int idleTimeoutSeconds;
    final private int shutdownTimeoutSeconds;
    final private long finalResultPublicationNanos;

    @GuardedBy("this")
    private int index;

    @GuardedBy("this")
    private State state;

    @GuardedBy("this")
    private CompletableFuture<Void> qFuture;

    @GuardedBy("this")
    private long secondsToRun;

    @GuardedBy("this")
    private long recordsCount;


    /**
     * Constructor CQueuePerl initialize all values.
     *
     * @param perlConfig          NotNull PerlConfig
     * @param periodicRecorder    PeriodicRecorder
     * @param reportingIntervalMS int
     * @param time                Time
     * @param executor            ExecutorService
     * @throws IllegalArgumentException when cleanup timing is invalid
     */
    public CQueuePerl(@NotNull PerlConfig perlConfig, PeriodicRecorder periodicRecorder,
                      int reportingIntervalMS, Time time, ExecutorService executor) {
        int maxQs;
        this.time = time;
        this.executor = executor;
        this.retFuture = new CompletableFuture<>();
        this.idleTimeoutSeconds = perlConfig.idleTimeoutSeconds;
        if (perlConfig.shutdownTimeoutSeconds < 1) {
            throw new IllegalArgumentException("PerL shutdown timeout seconds must be positive");
        }
        if (perlConfig.finalResultPublicationMillis < 1
                || perlConfig.finalResultPublicationMillis >= TimeUnit.SECONDS.toMillis(
                perlConfig.shutdownTimeoutSeconds)) {
            throw new IllegalArgumentException("PerL final-result publication time must be positive "
                    + "and shorter than the shutdown timeout");
        }
        this.shutdownTimeoutSeconds = perlConfig.shutdownTimeoutSeconds;
        this.finalResultPublicationNanos = TimeUnit.MILLISECONDS.toNanos(
                perlConfig.finalResultPublicationMillis);
        this.state = State.BEGIN;
        if (perlConfig.maxQs > 0) {
            maxQs = perlConfig.maxQs;
            this.index = 1;
        } else {
            maxQs = Math.max(PerlConfig.MIN_Q_PER_WORKER, perlConfig.qPerWorker);
            this.index = Math.max(perlConfig.workers, PerlConfig.MIN_WORKERS);
        }
        this.channels = new Channel[this.index];
        for (int i = 0; i < channels.length; i++) {
            channels[i] = perlConfig.mpscQueueEnable
                    ? new TimeStampMpscQueueChannel(maxQs, new OnError())
                    : new CQueueChannel(maxQs, new OnError());
        }
        if (perlConfig.sleepMS > 0) {
            this.perlReceiver = new PerformanceRecorderIdleSleep(periodicRecorder, channels, time, reportingIntervalMS,
                    Math.min(perlConfig.sleepMS, reportingIntervalMS), perlConfig.idleTimeoutSeconds);
        } else {
            this.perlReceiver = new PerformanceRecorderElasticWait(periodicRecorder, channels, time, reportingIntervalMS,
                    Math.max(PerlConfig.MIN_IDLE_NS, perlConfig.idleNS), perlConfig.idleTimeoutSeconds);
        }
    }


    /**
     * Get Perl channel.
     *
     * @return PerlChannel Interface
     */
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
    private void shutdown(Throwable ex, BenchmarkTermination requestedTermination,
                          long cleanupDeadlineNanos, boolean drainMeasurements) {
        if (state != State.END) {
            state = State.END;
            Throwable terminalFailure = unwrapCompletionFailure(ex);
            InterruptedException interruption = null;
            if (qFuture != null) {
                final long drainDeadlineNanos = cleanupDeadlineNanos - finalResultPublicationNanos;
                if (drainMeasurements && terminalFailure == null && !qFuture.isDone()) {
                    while (!channelsEmpty() && !qFuture.isDone()
                            && System.nanoTime() < drainDeadlineNanos) {
                        LockSupport.parkNanos(Math.min(DRAIN_POLL_NANOS,
                                Math.max(1, drainDeadlineNanos - System.nanoTime())));
                        if (Thread.interrupted()) {
                            interruption = new InterruptedException(
                                    "Interrupted while draining PerL measurements");
                            break;
                        }
                    }
                    if (!channelsEmpty() && !qFuture.isDone() && interruption == null) {
                        PerlPrinter.log.warn("PerL cleanup reached its bounded drain limit; "
                                + "publishing the final Total before shutdown");
                        for (Channel channel : channels) {
                            channel.clear();
                        }
                    }
                }
                if (!qFuture.isDone()) {
                    final long endTime = time.getCurrentTime();
                    for (Channel ch : channels) {
                        ch.sendEndTime(endTime);
                    }
                }
                if (interruption == null) {
                    try {
                        final long remainingNanos = cleanupDeadlineNanos - System.nanoTime();
                        if (remainingNanos <= 0) {
                            throw new TimeoutException("PerL cleanup deadline expired");
                        }
                        qFuture.get(remainingNanos, TimeUnit.NANOSECONDS);
                    } catch (ExecutionException failure) {
                        terminalFailure = retainFailure(terminalFailure, failure.getCause());
                    } catch (InterruptedException interrupted) {
                        interruption = interrupted;
                    } catch (TimeoutException timeout) {
                        PerlPrinter.log.warn("PerL final-result publication reached the hard "
                                + "cleanup deadline; continuing bounded shutdown");
                    }
                }
                if (qFuture.isDone()) {
                    for (Channel ch : channels) {
                        ch.clear();
                    }
                } else {
                    qFuture.cancel(true);
                }
                qFuture = null;
            }
            terminalFailure = retainFailure(terminalFailure, interruption);
            if (interruption != null) {
                Thread.currentThread().interrupt();
            }
            if (terminalFailure != null) {
                final BenchmarkTermination termination = BenchmarkTermination.resolve(
                        requestedTermination, terminalFailure);
                PerlPrinter.log.warn("PerL Shutdown: {}", termination.describe(
                        secondsToRun, recordsCount, idleTimeoutSeconds, terminalFailure), terminalFailure);
                retFuture.completeExceptionally(terminalFailure);
            } else {
                PerlPrinter.log.info("PerL Shutdown: {}", requestedTermination.describe(
                        secondsToRun, recordsCount, idleTimeoutSeconds, null));
                retFuture.complete(null);
            }
        }
    }

    private boolean channelsEmpty() {
        for (Channel channel : channels) {
            if (!channel.isEmpty()) {
                return false;
            }
        }
        return true;
    }

    private static Throwable retainFailure(Throwable currentFailure, Throwable additionalFailure) {
        final Throwable normalizedFailure = unwrapCompletionFailure(additionalFailure);
        if (normalizedFailure == null) {
            return currentFailure;
        }
        if (currentFailure == null) {
            return normalizedFailure;
        }
        if (currentFailure != normalizedFailure) {
            currentFailure.addSuppressed(normalizedFailure);
        }
        return currentFailure;
    }

    private static Throwable unwrapCompletionFailure(Throwable failure) {
        Throwable unwrapped = failure;
        while ((unwrapped instanceof CompletionException || unwrapped instanceof ExecutionException)
                && unwrapped.getCause() != null) {
            unwrapped = unwrapped.getCause();
        }
        return unwrapped;
    }

    /**
     * Run the CQ Perl.
     *
     * @param secondsToRun Number of seconds to Run
     * @param recordsCount If secondsToRun is 0, then this indicates the total number of records to benchmark or
     *                     read/write. If secondsToRun is higher than 0, then this parameter is ignored.
     * @return CompletableFuture retFuture.
     */
    @Override
    @Synchronized
    public CompletableFuture<Void> run(long secondsToRun, long recordsCount) {
        return start(secondsToRun, recordsCount, secondsToRun, recordsCount);
    }

    @Override
    @Synchronized
    public CompletableFuture<Void> runOrchestrated(long secondsToRun, long recordsCount) {
        return secondsToRun > 0
                ? start(secondsToRun, recordsCount, 0, 0)
                : start(secondsToRun, recordsCount, secondsToRun, recordsCount);
    }

    private CompletableFuture<Void> start(long secondsToRun, long recordsCount,
                                          long recorderSecondsToRun,
                                          long recorderRecordsCount) {
        if (state == State.BEGIN) {
            state = State.RUN;
            this.secondsToRun = secondsToRun;
            this.recordsCount = recordsCount;
            PerlPrinter.log.info("CQueuePerl Start");
            qFuture = CompletableFuture.runAsync(() -> perlReceiver.run(
                    recorderSecondsToRun, recorderRecordsCount),
                    executor);
            qFuture.whenComplete((ret, ex) -> {
                shutdown(ex, BenchmarkTermination.configured(secondsToRun, recordsCount),
                        standaloneCleanupDeadline(), false);
            });
        }
        return retFuture.toCompletableFuture();
    }

    /**
     * Stop the CQ Perl.
     */
    @Override
    public void stop() {
        shutdown(null, BenchmarkTermination.STOP_REQUESTED,
                standaloneCleanupDeadline(), true);
    }

    @Override
    public void stop(BenchmarkTermination termination) {
        shutdown(null, termination, standaloneCleanupDeadline(), true);
    }

    @Override
    public void stopBefore(BenchmarkTermination termination,
                           long cleanupDeadlineNanos) {
        shutdown(null, termination, cleanupDeadlineNanos, true);
    }

    private long standaloneCleanupDeadline() {
        return System.nanoTime() + TimeUnit.SECONDS.toNanos(shutdownTimeoutSeconds);
    }

    interface Throw {
        void onException(Throwable ex);
    }


    @NotThreadSafe
    static final class CQueueChannel
            extends ConcurrentLinkedQueueArray<TimeStamp>
            implements Channel {
        final private int maxQs;
        final private Throw eThrow;
        private int rIndex;

        public CQueueChannel(int maxQs, Throw eThrow) {
            super(maxQs);
            this.rIndex = maxQs;
            this.maxQs = maxQs;
            this.eThrow = eThrow;
        }

        public TimeStamp receive(int timeout) {
            rIndex += 1;
            if (rIndex >= maxQs) {
                rIndex = 0;
            }
            return poll(rIndex);
        }

        public void sendEndTime(long endTime) {
            add(0, new TimeStamp(endTime));
        }

        @Override
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
            public void send(
                    long startTime, long endTime, int records, int bytes) {
                this.wIndex += 1;
                if (this.wIndex >= maxQs) {
                    this.wIndex = 0;
                }
                add(this.wIndex,
                        new TimeStamp(startTime, endTime, records, bytes));
            }

            @Override
            public void throwException(Throwable ex) {
                eThrow.onException(ex);
            }
        }

    }

    @NotThreadSafe
    static final class TimeStampMpscQueueChannel
            extends TimeStampMpscQueueArray
            implements Channel {
        final private int maxQs;
        final private Throw eThrow;
        private int rIndex;

        /**
         * Creates an intrusive timestamp channel with the requested queue
         * count.
         *
         * @param maxQs number of queues distributed across producer sends
         * @param eThrow callback that propagates producer failures to PerL
         */
        public TimeStampMpscQueueChannel(int maxQs, Throw eThrow) {
            super(maxQs);
            this.rIndex = maxQs;
            this.maxQs = maxQs;
            this.eThrow = eThrow;
        }

        /**
         * {@inheritDoc}
         *
         * @param timeout unused polling timeout retained by the
         *                {@link Channel} contract
         * @return the next available timestamp, or {@code null}
         */
        @Override
        public TimeStamp receive(int timeout) {
            rIndex += 1;
            if (rIndex >= maxQs) {
                rIndex = 0;
            }
            return poll(rIndex);
        }

        /**
         * {@inheritDoc}
         *
         * @param endTime benchmark completion time
         */
        @Override
        public void sendEndTime(long endTime) {
            addNode(0, new TimeStampNode(endTime));
        }

        @Override
        public PerlChannel getPerlChannel() {
            return new TimeStampMpscPerlChannel();
        }

        /**
         * Propagates a producer exception to the owning PerL instance.
         *
         * @param id producer identifier reserved for channel implementations
         * @param ex producer failure
         */
        public void sendException(int id, Throwable ex) {
            eThrow.onException(ex);
        }

        @NotThreadSafe
        private final class TimeStampMpscPerlChannel
                implements PerlChannel {
            private int wIndex;

            private TimeStampMpscPerlChannel() {
                this.wIndex = 0;
            }

            @Override
            public void send(
                    long startTime, long endTime, int records, int bytes) {
                this.wIndex += 1;
                if (this.wIndex >= maxQs) {
                    this.wIndex = 0;
                }
                addNode(this.wIndex,
                        new TimeStampNode(
                                startTime, endTime, records, bytes));
            }

            @Override
            public void throwException(Throwable ex) {
                eThrow.onException(ex);
            }
        }
    }

    final private class OnError implements Throw {
        public void onException(Throwable ex) {
            shutdown(ex, BenchmarkTermination.INTERNAL_FAILURE,
                    standaloneCleanupDeadline(), false);
        }
    }
}
