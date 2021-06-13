/**
 * Copyright (c) KMG. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 */

package io.sbk.ram.impl;

import io.sbk.api.Benchmark;
import io.sbk.api.RWCount;
import io.sbk.grpc.LatenciesRecord;
import io.sbk.perl.Print;
import io.sbk.perl.ReportLatencies;
import io.sbk.perl.Time;
import io.sbk.perl.LatencyRecordWindow;
import io.sbk.ram.RamRegistry;
import io.sbk.system.Printer;
import lombok.Synchronized;

import javax.annotation.concurrent.GuardedBy;
import java.io.IOException;
import java.util.HashMap;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicLong;


public class RamBenchmark implements Benchmark, RamRegistry  {
    private final int idleMS;
    private final Time time;
    private final int reportingIntervalMS;
    private final LatencyRecordWindow window;
    private final ReportLatencies reportLatencies;
    private final RWCount rwCount;
    private final Print logger;
    private final ConcurrentLinkedQueue<LatenciesRecord>[] cQueues;
    private final HashMap<Long, RW> table;
    private final AtomicLong counter;

    @GuardedBy("this")
    private CompletableFuture<Void> retFuture;

    @GuardedBy("this")
    private CompletableFuture<Void> qFuture;

    public RamBenchmark(int maxQueue, int idleMS, LatencyRecordWindow window, Time time,
                        int reportingIntervalMS, ReportLatencies reportLatencies,
                        RWCount rwCount, Print logger) {
        this.idleMS = idleMS;
        this.window = window;
        this.time = time;
        this.reportingIntervalMS = reportingIntervalMS;
        this.reportLatencies = reportLatencies;
        this.rwCount = rwCount;
        this.logger = logger;
        this.table = new HashMap<>();
        this.cQueues = new ConcurrentLinkedQueue[maxQueue];
        for (int i = 0; i < cQueues.length; i++) {
            cQueues[i] = new ConcurrentLinkedQueue<>();
        }
        this.counter = new AtomicLong(0);
        this.retFuture = null;
        this.qFuture = null;
    }

    void run() throws InterruptedException {
        LatenciesRecord record;
        boolean doWork = true;
        boolean notFound;
        Printer.log.info("LatenciesRecord Benchmark Started" );
        long currentTime = time.getCurrentTime();
        window.reset(currentTime);
        while (doWork) {
            notFound = true;
            for (ConcurrentLinkedQueue<LatenciesRecord> queue : cQueues) {
                record = queue.poll();
                if (record != null) {
                    notFound = false;
                    if (record.getSequenceNumber() > 0) {
                        addRW(record.getClientID(), record.getReaders(), record.getWriters(),
                                record.getMaxReaders(), record.getMaxWriters());
                        window.maxLatency = Math.max(record.getMaxLatency(), window.maxLatency);
                        window.totalRecords += record.getTotalRecords();
                        window.totalBytes += record.getTotalBytes();
                        window.totalLatency += record.getTotalLatency();
                        window.higherLatencyDiscardRecords += record.getHigherLatencyDiscardRecords();
                        window.lowerLatencyDiscardRecords += record.getLowerLatencyDiscardRecords();
                        window.validLatencyRecords += record.getValidLatencyRecords();
                        window.invalidLatencyRecords += record.getInvalidLatencyRecords();
                        record.getLatencyMap().forEach(window::reportLatency);

                        if (window.isOverflow()) {
                            flush(time.getCurrentTime());
                        }
                    } else {
                        doWork = false;
                    }
                }
            }
            if (notFound) {
                Thread.sleep(idleMS);
            }

            currentTime = time.getCurrentTime();
            if (window.elapsedMilliSeconds(currentTime) > reportingIntervalMS) {
                flush(currentTime);
            }
        }

        if (window.totalRecords > 0) {
            flush(currentTime);
        }
    }


    void flush(long currentTime) {
        final RW rwStore = new RW();
        sumRW(rwStore);
        rwCount.setReaders(rwStore.readers);
        rwCount.setWriters(rwStore.writers);
        rwCount.setMaxReaders(rwStore.maxReaders);
        rwCount.setMaxWriters(rwStore.maxWriters);
        window.print(currentTime, logger, reportLatencies);
        window.reset(currentTime);
    }

    @Override
    public long getID() {
        return counter.incrementAndGet();
    }

    @Override
    public void enQueue(LatenciesRecord record) {
        final int index = (int) (record.getClientID() % cQueues.length);
        cQueues[index].add(record);
    }


    private static class RW {
        public int readers;
        public int writers;
        public int maxReaders;
        public int maxWriters;

        public RW() {
            reset();
        }

        public void reset() {
            readers = writers = maxWriters = maxReaders = 0;
        }


        public void update(int readers, int writers, int maxReaders, int maxWriters) {
            this.readers = Math.max(this.readers, readers);
            this.writers = Math.max(this.writers, writers);
            this.maxReaders = Math.max(this.maxReaders, maxReaders);
            this.maxWriters = Math.max(this.maxWriters, maxWriters);
        }
    }



    private void addRW(long key, int readers, int writers, int maxReaders, int maxWriters) {
        RW cur = table.get(key);
        if (cur == null) {
            cur = new RW();
            table.put(key, cur);
        }
        cur.update(readers, writers, maxReaders, maxWriters);
    }

    private void sumRW(RW ret) {
        table.forEach((k, data) -> {
            ret.readers += data.readers;
            ret.writers += data.writers;
            ret.maxReaders += data.maxReaders;
            ret.maxWriters += data.maxWriters;
        });
        table.clear();
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
                    cQueues[0].add(LatenciesRecord.newBuilder().setSequenceNumber(-1).build());
                    qFuture.get();
                    for (ConcurrentLinkedQueue<LatenciesRecord> queue : cQueues) {
                        queue.clear();
                    }
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


