/**
 * Copyright (c) KMG. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.sbm.api.impl;

import io.perl.api.impl.ConcurrentLinkedQueueArray;
import io.perl.exception.BenchmarkIdleTimeoutException;
import io.sbk.api.Benchmark;
import io.sbm.api.SbmPeriodicRecorder;
import io.sbp.grpc.MessageLatenciesRecord;
import io.sbm.api.SbmRegistry;
import io.sbk.system.Printer;
import io.state.State;
import io.time.Time;
import lombok.Synchronized;
import org.jetbrains.annotations.NotNull;

import javax.annotation.concurrent.GuardedBy;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicLong;


/**
 * In-memory aggregator that receives latency records from remote clients and reports metrics.
 *
 * <p>Backed by a fixed array of concurrent queues, one per client modulo index, to reduce
 * contention. Periodically flushes a window to the configured {@link SbmPeriodicRecorder}, and
 * on stop prints total results.
 */
final public class SbmLatencyBenchmark extends ConcurrentLinkedQueueArray<MessageLatenciesRecord> implements Benchmark,
        SbmRegistry {
    static final String CONSUMER_THREAD_NAME = "sbm-latency-consumer";
    private final int maxQs;
    private final int idleMS;
    private final Time time;
    private final int reportingIntervalMS;
    private final long idleTimeoutMS;
    private final int idleTimeoutSeconds;
    private final boolean idleTimeoutEnabled;
    private final SbmPeriodicRecorder window;
    private final AtomicLong counter;
    private final CompletableFuture<Void> retFuture;
    private final ExecutorService executor;

    @GuardedBy("this")
    private State state;

    @GuardedBy("this")
    private CompletableFuture<Void> qFuture;

    /**
     * Constructor RamBenchmark initializing all values.
     *
     * @param maxQs               number of internal queues used for sharding
     * @param idleMS              sleep in milliseconds when queues are empty
     * @param time                time source
     * @param window              periodic/total latency recorder
     * @param reportingIntervalMS interval in ms between periodic window prints
     * @param idleTimeoutSeconds maximum interval without an SBK performance batch
     * @throws IllegalArgumentException when the idle timeout is not positive or does not exceed the reporting interval
     */
    public SbmLatencyBenchmark(int maxQs, int idleMS, Time time, SbmPeriodicRecorder window, int reportingIntervalMS,
                               int idleTimeoutSeconds) {
        this(maxQs, idleMS, time, window, reportingIntervalMS, idleTimeoutSeconds, false);
    }

    /**
     * Creates the latency consumer with an optional fixed-record idle deadline.
     *
     * @param maxQs maximum number of client queues
     * @param idleMS empty-queue sleep interval in milliseconds
     * @param time time source
     * @param window periodic/total latency recorder
     * @param reportingIntervalMS interval in ms between periodic window prints
     * @param idleTimeoutSeconds maximum interval without an SBK performance batch
     * @param idleTimeoutEnabled whether fixed-record idle enforcement is enabled
     * @throws IllegalArgumentException when the idle timeout is not positive or does not exceed the reporting interval
     */
    public SbmLatencyBenchmark(int maxQs, int idleMS, Time time, SbmPeriodicRecorder window, int reportingIntervalMS,
                               int idleTimeoutSeconds, boolean idleTimeoutEnabled) {
        super(maxQs);
        if (idleTimeoutSeconds <= 0) {
            throw new IllegalArgumentException("SBM idle timeout seconds must be greater than zero");
        }
        final long configuredIdleTimeoutMS = Math.multiplyExact(
                (long) idleTimeoutSeconds, Time.MS_PER_SEC);
        if (configuredIdleTimeoutMS <= reportingIntervalMS) {
            throw new IllegalArgumentException("SBM idle timeout seconds must be greater than the reporting "
                    + "interval of " + reportingIntervalMS + " milliseconds");
        }
        this.maxQs = maxQs;
        this.idleMS = idleMS;
        this.window = window;
        this.time = time;
        this.reportingIntervalMS = reportingIntervalMS;
        this.idleTimeoutSeconds = idleTimeoutSeconds;
        this.idleTimeoutMS = configuredIdleTimeoutMS;
        this.idleTimeoutEnabled = idleTimeoutEnabled;
        this.counter = new AtomicLong(BASE_CLIENT_ID_VALUE);
        this.retFuture = new CompletableFuture<>();
        this.executor = Executors.newSingleThreadExecutor(
                Thread.ofPlatform().name(CONSUMER_THREAD_NAME).factory());
        this.state = State.BEGIN;
        this.qFuture = null;
    }

    /**
     * Main processing loop: drains records from all queues, records latencies, and rotates
     * periodic windows at the configured interval. Terminates when a sentinel with
     * sequenceNumber <= 0 is observed.
     *
     * @throws InterruptedException if the thread sleep or processing is interrupted
     * @throws IllegalStateException if a latency batch cannot be aggregated
     * @throws BenchmarkIdleTimeoutException when no performance batch arrives before the idle deadline
     */
    void run() throws InterruptedException {
        Printer.log.info("SbmLatencyBenchmark Started : {} milliseconds idle sleep", this.idleMS);
        if (idleTimeoutEnabled) {
            runWithIdleTimeout();
        } else {
            runWithoutIdleTimeout();
        }
    }

    private void runWithoutIdleTimeout() throws InterruptedException {
        MessageLatenciesRecord record;
        boolean doWork = true;
        boolean notFound;
        boolean receivedBatchInWindow = false;
        long currentTime = time.getCurrentTime();
        window.start(currentTime);
        window.startWindow(currentTime);
        while (doWork) {
            notFound = true;
            for (int qIndex = 0; qIndex < maxQs; qIndex++) {
                record = poll(qIndex);
                if (record != null) {
                    notFound = false;
                    if (record.getSequenceNumber() > 0) {
                        try {
                            window.record(currentTime, record);
                        } catch (RuntimeException exception) {
                            throw new IllegalStateException("SBM failed to aggregate latency batch for client "
                                    + record.getClientID() + " at sequence " + record.getSequenceNumber(), exception);
                        }
                        receivedBatchInWindow = true;
                    } else {
                        doWork = false;
                    }
                }
            }
            if (notFound) {
                Thread.sleep(idleMS);
            }

            currentTime = time.getCurrentTime();
            if (window.elapsedMilliSecondsWindow(currentTime) > reportingIntervalMS) {
                /*
                 * SBM starts before remote SBK processes and can remain alive after
                 * they finish. Do not manufacture empty aggregate windows when no
                 * client supplied a regular SBK reporting batch. A received batch
                 * is still printed even when it legitimately contains zero records.
                 */
                if (receivedBatchInWindow) {
                    window.stopWindow(currentTime);
                }
                window.startWindow(currentTime);
                receivedBatchInWindow = false;
            }
        }
        window.stop(currentTime);
    }

    private void runWithIdleTimeout() throws InterruptedException {
        MessageLatenciesRecord record;
        boolean doWork = true;
        boolean notFound;
        boolean receivedBatchInWindow = false;
        long recordsInSweep;
        long currentTime = time.getCurrentTime();
        long lastEventTime = currentTime;
        window.start(currentTime);
        window.startWindow(currentTime);
        while (doWork) {
            notFound = true;
            recordsInSweep = 0;
            for (int qIndex = 0; qIndex < maxQs; qIndex++) {
                record = poll(qIndex);
                if (record != null) {
                    notFound = false;
                    if (record.getSequenceNumber() > 0) {
                        try {
                            window.record(currentTime, record);
                        } catch (RuntimeException exception) {
                            throw new IllegalStateException("SBM failed to aggregate latency batch for client "
                                    + record.getClientID() + " at sequence " + record.getSequenceNumber(), exception);
                        }
                        recordsInSweep |= record.getTotalRecords();
                        receivedBatchInWindow = true;
                    } else {
                        doWork = false;
                    }
                }
            }
            if (notFound) {
                Thread.sleep(idleMS);
                currentTime = time.getCurrentTime();
                if (time.elapsedMilliSeconds(currentTime, lastEventTime) >= idleTimeoutMS) {
                    throw new BenchmarkIdleTimeoutException(idleTimeoutSeconds);
                }
            } else {
                currentTime = time.getCurrentTime();
            }
            if (recordsInSweep > 0) {
                lastEventTime = currentTime;
            }
            if (window.elapsedMilliSecondsWindow(currentTime) > reportingIntervalMS) {
                if (receivedBatchInWindow) {
                    window.stopWindow(currentTime);
                }
                window.startWindow(currentTime);
                receivedBatchInWindow = false;
            }
        }
        window.stop(currentTime);
    }


    /**
     * Allocate a unique client ID for a new connection.
     */
    @Override
    public long getID() {
        return counter.getAndIncrement();
    }

    /**
     * Enqueue a latency record into a sharded queue based on client ID.
     */
    @Override
    public void enQueue(@NotNull MessageLatenciesRecord record) {
        final int index = (int) (record.getClientID() % maxQs);
        add(index, record);
    }

    @Synchronized
    private void shutdown(Throwable ex) {
        if (state != State.END) {
            state = State.END;
            Throwable terminalFailure = unwrapCompletionFailure(ex);
            InterruptedException interruption = null;
            if (qFuture != null) {
                if (!qFuture.isDone()) {
                    add(0, MessageLatenciesRecord.newBuilder().setSequenceNumber(-1).build());
                }
                boolean receiverCompleted = false;
                while (!receiverCompleted) {
                    try {
                        qFuture.get();
                        receiverCompleted = true;
                    } catch (ExecutionException failure) {
                        terminalFailure = retainFailure(terminalFailure, failure.getCause());
                        receiverCompleted = true;
                    } catch (InterruptedException interrupted) {
                        if (interruption == null) {
                            interruption = interrupted;
                        }
                    }
                }
                clear();
                qFuture = null;
            }
            executor.shutdown();
            terminalFailure = retainFailure(terminalFailure, interruption);
            if (interruption != null) {
                Thread.currentThread().interrupt();
            }
            if (terminalFailure != null) {
                Printer.log.warn("SbmLatencyBenchmark exited due to internal exception", terminalFailure);
                retFuture.completeExceptionally(terminalFailure);
            } else {
                Printer.log.info("SbmLatencyBenchmark Shutdown");
                retFuture.complete(null);
            }
        }
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
     * Starts the SBM latency consumer on its dedicated platform thread.
     *
     * @return future completed after the consumer has terminated
     * @throws IllegalStateException if the benchmark cannot be started from
     *                               its current state
     */
    @Override
    @Synchronized
    public CompletableFuture<Void> start() throws IllegalStateException {
        if (state == State.BEGIN) {
            state = State.RUN;
            qFuture = CompletableFuture.runAsync(() -> {
                try {
                    run();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    throw new CompletionException(e);
                }
            }, executor);
            qFuture.whenComplete((ret, ex) -> {
                shutdown(ex);
            });
        }
        return retFuture.toCompletableFuture();
    }

    @Override
    public void stop() {
        shutdown(null);
    }

}
