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
import io.sbk.api.RWCount;
import io.sbk.perl.LatencyRecord;
import io.sbk.perl.Print;
import io.sbk.perl.ReportLatencies;
import io.sbk.perl.Time;
import io.sbk.perl.LatencyRecordWindow;
import io.sbk.system.Printer;
import lombok.Synchronized;

import javax.annotation.concurrent.GuardedBy;
import java.io.IOException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;


public class LatenciesRecordsBenchmark implements Benchmark {
    private final Time time;
    private final int reportingIntervalMS;
    private final LatencyRecordWindow window;
    private final ReportLatencies reportLatencies;
    private final RWCount rwCount;
    private final Print logger;
    private final LinkedBlockingQueue<LatenciesRecord> queue;

    @GuardedBy("this")
    private CompletableFuture<Void> retFuture;

    @GuardedBy("this")
    private CompletableFuture<Void> qFuture;

    public LatenciesRecordsBenchmark(LatencyRecordWindow window, Time time, int reportingIntervalMS,
                                     ReportLatencies reportLatencies,  RWCount rwCount, Print logger,
                                     LinkedBlockingQueue<LatenciesRecord> queue) {
        this.window = window;
        this.time = time;
        this.reportingIntervalMS = reportingIntervalMS;
        this.reportLatencies = reportLatencies;
        this.rwCount = rwCount;
        this.logger = logger;
        this.queue = queue;
        this.retFuture = null;
        this.qFuture = null;
    }

    void run() throws InterruptedException {
        LatenciesRecord record;
        boolean doWork = true;
        int writers = 0;
        int maxWriters = 0;
        int readers = 0;
        int maxReaders = 0;
        Printer.log.info("LatenciesRecord Benchmark Started" );
        long currentTime = time.getCurrentTime();
        window.reset(currentTime);
        final LatencyRecord latencyRecord = new LatencyRecord();
        while (doWork) {
            record = queue.poll(reportingIntervalMS, TimeUnit.MILLISECONDS);
            if (record != null) {
                if (record.getSequenceNumber() > 0) {
                    writers += record.getWriters();
                    readers += record.getReaders();
                    maxWriters += record.getMaxWriters();
                    maxReaders += record.getMaxReaders();
                    latencyRecord.maxLatency = record.getMaxLatency();
                    latencyRecord.totalRecords = record.getTotalRecords();
                    latencyRecord.totalBytes = record.getTotalBytes();
                    latencyRecord.totalLatency = record.getTotalLatency();
                    latencyRecord.higherLatencyDiscardRecords = record.getHigherLatencyDiscardRecords();
                    latencyRecord.lowerLatencyDiscardRecords = record.getLowerLatencyDiscardRecords();
                    latencyRecord.validLatencyRecords = record.getValidLatencyRecords();
                    latencyRecord.invalidLatencyRecords = record.getInvalidLatencyRecords();
                    window.reportLatencyRecord(latencyRecord);
                    reportLatencies.reportLatencyRecord(latencyRecord);
                    record.getLatencyMap().forEach((k, v) -> {
                        window.reportLatency(k, v);
                        reportLatencies.reportLatency(k, v);
                    });
                } else {
                    doWork = false;
                }
            }
            currentTime = time.getCurrentTime();
            if (window.elapsedMilliSeconds(currentTime) > reportingIntervalMS) {
                window.print(currentTime, logger, null);
                window.reset(currentTime);
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
                    queue.put(LatenciesRecord.newBuilder().setSequenceNumber(-1).build());
                    qFuture.get();
                } catch (ExecutionException | InterruptedException e) {
                    e.printStackTrace();
                }
            }
            qFuture = null;
        }
        if (ex != null) {
            Printer.log.warn("LatenciesRecord Benchmark with Exception:" + ex.toString());
            retFuture.completeExceptionally(ex);
        } else  {
            Printer.log.info("LatenciesRecord Benchmark Shutdown" );
            retFuture.complete(null);
        }
        retFuture = null;
    }



    @Override
    @Synchronized
    public CompletableFuture<Void> start() throws IOException, InterruptedException, ExecutionException,
            IllegalStateException {
        if (retFuture != null) {
            throw  new IllegalStateException("LatenciesRecord Benchmark is already started\n");
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


