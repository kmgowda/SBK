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

import io.sbk.api.Benchmark;
import io.sbk.api.TransactionRecord;
import io.sbk.api.RWCount;
import io.sbk.perl.Print;
import io.sbk.perl.Time;
import io.sbk.perl.impl.LatencyWindow;
import io.sbk.system.Printer;
import lombok.Synchronized;

import javax.annotation.concurrent.GuardedBy;
import java.io.IOException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;


public class TransactionsBenchmark implements Benchmark {
    private final Time time;
    private final int reportingIntervalMS;
    private final LatencyWindow window;
    private final RWCount rwCount;
    private final Print logger;
    private final LinkedBlockingQueue<TransactionRecord> queue;

    @GuardedBy("this")
    private CompletableFuture<Void> retFuture;

    @GuardedBy("this")
    private CompletableFuture<Void> qFuture;

    public TransactionsBenchmark(LatencyWindow window, Time time, int reportingIntervalMS,
                                 RWCount rwCount, Print logger, LinkedBlockingQueue<TransactionRecord> queue) {
        this.window = window;
        this.time = time;
        this.reportingIntervalMS = reportingIntervalMS;
        this.rwCount = rwCount;
        this.logger = logger;
        this.queue = queue;
        this.retFuture = null;
        this.qFuture = null;
    }

    void run() throws InterruptedException {
        TransactionRecord trans;
        long startTime = time.getCurrentTime();
        long endTime;
        boolean doWork = true;
        int writers = 0;
        int maxWriters = 0;
        int readers = 0;
        int maxReaders = 0;
        Printer.log.info("Transactions Benchmark Started" );
        window.reset(startTime);
        while (doWork) {
            trans = queue.poll(reportingIntervalMS, TimeUnit.MILLISECONDS);
            if (trans != null) {
                if (trans.transID > 0) {
                    writers += trans.writers;
                    readers += trans.readers;
                    maxWriters += trans.maxWriters;
                    maxReaders += trans.maxReaders;
                    window.updateRecord(trans.record);
                    trans.list.forEach(lt -> {
                        lt.forEach((k, v) -> {
                            window.record(startTime, 0, v, k);
                        });
                    });
                } else {
                    doWork = false;
                }
            }
            endTime = time.getCurrentTime();
            if (window.elapsedMilliSeconds(endTime) > reportingIntervalMS) {
                window.print(endTime, logger, null);
                window.reset(endTime);
                rwCount.setWriters(writers);
                rwCount.setMaxWriters(maxWriters);
                rwCount.setReaders(readers);
                rwCount.setMaxReaders(maxReaders);
                writers = maxWriters = readers = maxReaders = 0;
            }
        }

        if (window.totalRecords > 0) {
            window.print(time.getCurrentTime(), logger, null);
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
                try {
                    queue.put(new TransactionRecord(-1));
                    qFuture.get();
                } catch (ExecutionException | InterruptedException e) {
                    e.printStackTrace();
                }
            }
            qFuture = null;
        }
        if (ex != null) {
            Printer.log.warn("Transactions Benchmark with Exception:" + ex.toString());
            retFuture.completeExceptionally(ex);
        } else  {
            Printer.log.info("Transactions Benchmark Shutdown" );
            retFuture.complete(null);
        }
        retFuture = null;
    }



    @Override
    @Synchronized
    public CompletableFuture<Void> start() throws IOException, InterruptedException, ExecutionException,
            IllegalStateException {
        if (retFuture != null) {
            throw  new IllegalStateException("Transactions Benchmark is already started\n");
        }
        retFuture = new CompletableFuture<>();
        qFuture =  CompletableFuture.runAsync(() -> {
            try {
                run();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        });
        qFuture.whenComplete((ret, ex) -> {
            shutdown(ex);
        });
        return retFuture;
    }

    @Override
    public void stop() {
        shutdown(null);
    }

}


