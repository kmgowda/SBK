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

import io.sbk.api.AsyncReader;
import io.sbk.api.Benchmark;
import io.sbk.api.DataType;
import io.sbk.api.Parameters;
import io.sbk.api.Performance;
import io.sbk.api.QuadConsumer;
import io.sbk.api.Reader;
import io.sbk.api.ResultLogger;
import io.sbk.api.Storage;
import io.sbk.api.Writer;
import lombok.Synchronized;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

/**
 * Class for performing the benchmark.
 */
public class SbkBenchmark implements Benchmark {
    final static private boolean USEFORK = true;
    final private Storage storage;
    final private ExecutorService executor;
    final private Parameters params;
    final private Performance writeStats;
    final private Performance readStats;
    final private QuadConsumer writeTime;
    final private QuadConsumer readTime;
    final private ScheduledExecutorService timeoutExecutor;
    private List<Writer> writers;
    private List<Reader> readers;
    private List<AsyncReader> asyncReaders;
    private CompletableFuture<Void> ret;

    /**
     * Create SBK Benchmark.
     *
     * @param  action   Action String
     * @param  params   Parameters
     * @param  storage  Storage device/client/driver for benchmarking
     * @param  logger   Result Logger
     * @param  metricsLogger    Log the results to metrics logger
     * @param  reportingInterval Results reporting interval
     */
    public SbkBenchmark(String  action, Parameters params, Storage storage, ResultLogger logger,
                        ResultLogger metricsLogger, int reportingInterval) {
        this.params = params;
        this.storage = storage;
        final int threadCount = params.getWritersCount() + params.getReadersCount() + 6;
        if (USEFORK) {
            executor = new ForkJoinPool(threadCount);
        } else {
            executor = Executors.newFixedThreadPool(threadCount);
        }
        if (params.getWritersCount() > 0 && !params.isWriteAndRead()) {
            writeStats = new SbkPerformance(action, reportingInterval, params.getRecordSize(),
                    params.getCsvFile(), metricsLogger, logger, executor);
            writeTime = writeStats::recordTime;
        } else {
            writeStats = null;
            writeTime = null;
        }

        if (params.getReadersCount() > 0) {
            readStats = new SbkPerformance(action, reportingInterval, params.getRecordSize(),
                    params.getCsvFile(), metricsLogger, logger, executor);
            readTime = readStats::recordTime;
        } else {
            readStats = null;
            readTime = null;
        }
        timeoutExecutor = Executors.newScheduledThreadPool(1);
        ret = null;
    }

    /**
     * Start SBK Benchmark.
     *
     * opens the storage device/client , creates the writers/readers.
     * conducts the performance benchmarking for given time in seconds
     * or exits if the input the number of records are written/read.
     * NOTE: This method does NOT invoke parsing of parameters, storage device/client.
     *
     * @throws IOException If an exception occurred.
     * @throws IllegalStateException If an exception occurred.
     */
    @Override
    @Synchronized
    public CompletableFuture<Void> start() throws IOException, IllegalStateException {
        if (ret != null) {
            throw  new IllegalStateException("SbkBenchmark is already started\n");
        }
        storage.openStorage(params);
        final DataType data = storage.getDataType();
        final List<SbkWriter> sbkWriters;
        final List<SbkReader> sbkReaders;
        final List<SbkAsyncReader> sbkAsyncReaders;
        final List<CompletableFuture<Void>> writeFutures;
        final List<CompletableFuture<Void>> readFutures;

        writers = IntStream.range(0, params.getWritersCount())
                .boxed()
                .map(i -> storage.createWriter(i, params))
                .filter(x -> x != null)
                .collect(Collectors.toList());

        readers = IntStream.range(0, params.getReadersCount())
                .boxed()
                .map(i -> storage.createReader(i, params))
                .filter(x -> x != null)
                .collect(Collectors.toList());

        asyncReaders = IntStream.range(0, params.getReadersCount())
                .boxed()
                .map(i -> storage.createAsyncReader(i, params))
                .filter(x -> x != null)
                .collect(Collectors.toList());

        if (writers != null && writers.size() > 0) {
            sbkWriters = IntStream.range(0, params.getWritersCount())
                    .boxed()
                    .map(i -> new SbkWriter(i, params, writeTime, data, writers.get(i)))
                    .filter(x -> x != null)
                    .collect(Collectors.toList());
        } else {
            sbkWriters = null;
        }

        if (readers != null && readers.size() > 0) {
            sbkReaders = IntStream.range(0, params.getReadersCount())
                    .boxed()
                    .map(i -> new SbkReader(i, params, readTime, data, readers.get(i)))
                    .filter(x -> x != null)
                    .collect(Collectors.toList());
            sbkAsyncReaders = null;
        } else if (asyncReaders != null && asyncReaders.size() > 0) {
            sbkAsyncReaders = IntStream.range(0, params.getReadersCount())
                    .boxed()
                    .map(i -> new SbkAsyncReader(i, params, readTime, data))
                    .filter(x -> x != null)
                    .collect(Collectors.toList());
            sbkReaders = null;
        } else {
            sbkReaders = null;
            sbkAsyncReaders = null;
        }

        final long startTime = System.currentTimeMillis();
        if (writeStats != null && !params.isWriteAndRead() && sbkWriters != null) {
            writeStats.start(startTime);
        }
        if (readStats != null && (sbkReaders != null || sbkAsyncReaders != null)) {
            readStats.start(startTime);
        }
        if (sbkWriters != null) {
            writeFutures = sbkWriters.stream()
                    .map(x -> CompletableFuture.runAsync(x, executor)).collect(Collectors.toList());
        } else {
            writeFutures = null;
        }

        if (sbkReaders != null) {
            readFutures = sbkReaders.stream()
                    .map(x -> CompletableFuture.runAsync(x, executor)).collect(Collectors.toList());
        } else if (sbkAsyncReaders != null) {
            readFutures = sbkAsyncReaders.stream()
                    .map(x -> x.start(startTime)).collect(Collectors.toList());
            for (int i = 0; i < params.getReadersCount(); i++) {
                asyncReaders.get(i).start(sbkAsyncReaders.get(i));
            }
        } else {
            readFutures = null;
        }

        if (writeFutures != null && readFutures != null) {
            ret = CompletableFuture.allOf(Stream.concat(writeFutures.stream(), readFutures.stream()).
                    collect(Collectors.toList()).toArray(new CompletableFuture[writeFutures.size() + readFutures.size()]));
        } else if (readFutures != null) {
            ret = CompletableFuture.allOf(new ArrayList<>(readFutures).toArray(new CompletableFuture[readFutures.size()]));
        } else if (writeFutures != null) {
            ret = CompletableFuture.allOf(new ArrayList<>(writeFutures).toArray(new CompletableFuture[writeFutures.size()]));
        } else {
            ret = null;
        }

        if (params.getSecondsToRun() > 0) {
            timeoutExecutor.schedule(this::stop, params.getSecondsToRun() + 1, TimeUnit.SECONDS);
        }
        return ret;
    }

    /**
     * Stop/shutdown SBK Benchmark.
     *
     * closes all writers/readers.
     * closes the storage device/client.
     *
     */
    @Override
    @Synchronized
    public void stop() {
        try {
            if (writeStats != null && !params.isWriteAndRead()) {
                writeStats.shutdown(System.currentTimeMillis());
            }
            if (readStats != null) {
                readStats.shutdown(System.currentTimeMillis());
            }
        } catch (InterruptedException | ExecutionException ex) {
            ex.printStackTrace();
        }
        if (readers != null) {
            readers.forEach(c -> {
                try {
                    c.close();
                } catch (IOException ex) {
                    ex.printStackTrace();
                }
            });
        }
        if (asyncReaders != null) {
            asyncReaders.forEach(c -> {
                try {
                    c.close();
                } catch (IOException ex) {
                    ex.printStackTrace();
                }
            });
        }
        if (writers != null) {
            writers.forEach(c -> {
                try {
                    c.close();
                } catch (IOException ex) {
                    ex.printStackTrace();
                }
            });
        }
        try {
            storage.closeStorage(params);
        } catch (IOException ex) {
            ex.printStackTrace();
        }
        executor.shutdown();
        try {
            executor.awaitTermination(1, TimeUnit.SECONDS);
        } catch (InterruptedException ex) {
            ex.printStackTrace();
        }
        if (ret != null) {
            ret.complete(null);
        }
        ret = null;
    }
}
