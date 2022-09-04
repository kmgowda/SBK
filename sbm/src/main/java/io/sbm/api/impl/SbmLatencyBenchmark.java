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
import io.sbk.api.Benchmark;
import io.sbm.api.SbmPeriodicRecorder;
import io.sbp.grpc.LatenciesRecord;
import io.sbm.api.SbmRegistry;
import io.sbk.system.Printer;
import io.state.State;
import io.time.Time;
import lombok.Synchronized;
import org.jetbrains.annotations.NotNull;

import javax.annotation.concurrent.GuardedBy;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicLong;


/**
 * Class RamBenchmark.
 */
final public class SbmLatencyBenchmark extends ConcurrentLinkedQueueArray<LatenciesRecord> implements Benchmark,
        SbmRegistry {
    private final int maxQs;
    private final int idleMS;
    private final Time time;
    private final int reportingIntervalMS;
    private final SbmPeriodicRecorder window;
    private final AtomicLong counter;
    private final CompletableFuture<Void> retFuture;

    @GuardedBy("this")
    private State state;

    @GuardedBy("this")
    private CompletableFuture<Void> qFuture;

    /**
     * Constructor RamBenchmark initializing all values.
     *
     * @param maxQs              int
     * @param idleMS                int
     * @param time                  Time
     * @param window                RamPeriodicRecorder
     * @param reportingIntervalMS   int
     */
    public SbmLatencyBenchmark(int maxQs, int idleMS, Time time, SbmPeriodicRecorder window, int reportingIntervalMS) {
        super(maxQs);
        this.maxQs = maxQs;
        this.idleMS = idleMS;
        this.window = window;
        this.time = time;
        this.reportingIntervalMS = reportingIntervalMS;
        this.counter = new AtomicLong(BASE_CLIENT_ID_VALUE);
        this.retFuture = new CompletableFuture<>();
        this.state = State.BEGIN;
        this.qFuture = null;
    }

    void run() throws InterruptedException {
        LatenciesRecord record;
        boolean doWork = true;
        boolean notFound;
        Printer.log.info("LatenciesRecord Benchmark Started");
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
                        window.record(currentTime, record);
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
                window.stopWindow(currentTime);
                window.startWindow(currentTime);
            }
        }
        window.stop(currentTime);
    }


    @Override
    public long getID() {
        return counter.getAndIncrement();
    }

    @Override
    public void enQueue(@NotNull LatenciesRecord record) {
        final int index = (int) (record.getClientID() % maxQs);
        add(index, record);
    }

    @Synchronized
    private void shutdown(Throwable ex) {
        if (state != State.END) {
            state = State.END;
            if (qFuture != null) {
                if (!qFuture.isDone()) {
                    try {
                        add(0, LatenciesRecord.newBuilder().setSequenceNumber(-1).build());
                        qFuture.get();
                        clear();
                    } catch (ExecutionException | InterruptedException e) {
                        e.printStackTrace();
                    }
                }
                qFuture = null;
            }
            if (ex != null) {
                Printer.log.warn("LatenciesRecord Benchmark with Exception:" + ex);
                retFuture.completeExceptionally(ex);
            } else {
                Printer.log.info("LatenciesRecord Benchmark Shutdown");
                retFuture.complete(null);
            }
        }
    }


    @Override
    @Synchronized
    public CompletableFuture<Void> start() throws IllegalStateException {
        if (state == State.BEGIN) {
            state = State.RUN;
            qFuture = CompletableFuture.runAsync(() -> {
                try {
                    run();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            });
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


