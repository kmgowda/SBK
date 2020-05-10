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

import io.sbk.api.CallbackReader;
import io.sbk.api.Benchmark;
import io.sbk.api.DataType;
import io.sbk.api.Parameters;
import io.sbk.api.Performance;
import io.sbk.api.Reader;
import io.sbk.api.ResultLogger;
import io.sbk.api.Config;
import io.sbk.api.Storage;
import io.sbk.api.Writer;
import lombok.Synchronized;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
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
    final private Storage storage;
    final private ExecutorService executor;
    final private Parameters params;
    final private Performance writeStats;
    final private Performance readStats;
    final private ScheduledExecutorService timeoutExecutor;
    final private int maxQs;
    private List<Writer> writers;
    private List<Reader> readers;
    private List<CallbackReader> callbackReaders;
    private CompletableFuture<Void> ret;

    /**
     * Create SBK Benchmark.
     *
     * @param  action   Action String
     * @param  config   Configuration parameters
     * @param  params   Benchmarking input Parameters
     * @param  storage  Storage device/client/driver for benchmarking
     * @param  logger   Result Logger
     * @param  metricsLogger    Log the results to metrics logger
     */
    public SbkBenchmark(String  action, Config config, Parameters params,
                        Storage storage, ResultLogger logger, ResultLogger metricsLogger) {
        this.params = params;
        this.storage = storage;
        if (config.maxQs > 0) {
            this.maxQs = config.maxQs;
        } else {
            this.maxQs = Math.max(Config.MIN_Q_PER_WORKER, config.qPerWorker);
        }

        final int threadCount = params.getWritersCount() + params.getReadersCount() + 6;
        if (config.fork) {
            executor = new ForkJoinPool(threadCount);
        } else {
            executor = Executors.newFixedThreadPool(threadCount);
        }
        if (params.getWritersCount() > 0 && !params.isWriteAndRead()) {
            writeStats = new SbkPerformance(action, config, params.getWritersCount(),
                                       params.getCsvFile(), metricsLogger, logger, executor);
        } else {
            writeStats = null;
        }

        if (params.getReadersCount() > 0) {
            readStats = new SbkPerformance(action, config, params.getReadersCount(),
                                    params.getCsvFile(), metricsLogger, logger, executor);
        } else {
            readStats = null;
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
     * @param beginTime StartTime
     * @throws IOException If an exception occurred.
     * @throws IllegalStateException If an exception occurred.
     */
    @Override
    @Synchronized
    public CompletableFuture<Void> start(long beginTime) throws IOException, IllegalStateException {
        if (ret != null) {
            throw  new IllegalStateException("SbkBenchmark is already started\n");
        }
        storage.openStorage(params);
        final DataType dType = storage.getDataType();
        final List<SbkWriter> sbkWriters;
        final List<SbkReader> sbkReaders;
        final List<SbkCallback> sbkCallbackReaders;
        final List<CompletableFuture<Void>> writeFutures;
        final List<CompletableFuture<Void>> readFutures;

        writers = IntStream.range(0, params.getWritersCount())
                .boxed()
                .map(i -> storage.createWriter(i, params))
                .filter(Objects::nonNull)
                .collect(Collectors.toList());

        readers = IntStream.range(0, params.getReadersCount())
                .boxed()
                .map(i -> storage.createReader(i, params))
                .filter(Objects::nonNull)
                .collect(Collectors.toList());

        callbackReaders = IntStream.range(0, params.getReadersCount())
                .boxed()
                .map(i -> storage.createCallbackReader(i, params))
                .filter(Objects::nonNull)
                .collect(Collectors.toList());

        if (writers != null && writers.size() > 0) {
            if (writeStats != null) {
                sbkWriters = IntStream.range(0, params.getWritersCount())
                        .boxed()
                        .map(i -> new SbkWriter(i, maxQs, params, writeStats.get(), dType, writers.get(i)))
                        .collect(Collectors.toList());
            } else {
                sbkWriters = IntStream.range(0, params.getWritersCount())
                        .boxed()
                        .map(i -> new SbkWriter(i, maxQs, params, null, dType, writers.get(i)))
                        .collect(Collectors.toList());
            }
        } else {
            sbkWriters = null;
        }

        if (readers != null && readers.size() > 0) {
            sbkReaders = IntStream.range(0, params.getReadersCount())
                    .boxed()
                    .map(i -> new SbkReader(i, maxQs, params, readStats.get(), dType, readers.get(i)))
                    .collect(Collectors.toList());
            sbkCallbackReaders = null;
        } else if (callbackReaders != null && callbackReaders.size() > 0) {
            sbkCallbackReaders = IntStream.range(0, params.getReadersCount())
                    .boxed()
                    .map(i -> new SbkCallback(i, maxQs, params, readStats.get(), dType))
                    .collect(Collectors.toList());
            sbkReaders = null;
        } else {
            sbkReaders = null;
            sbkCallbackReaders = null;
        }

        final long startTime = System.currentTimeMillis();
        if (writeStats != null && !params.isWriteAndRead() && sbkWriters != null) {
            writeStats.start(startTime);
        }
        if (readStats != null && (sbkReaders != null || sbkCallbackReaders != null)) {
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
        } else if (sbkCallbackReaders != null) {
            readFutures = sbkCallbackReaders.stream()
                    .map(x -> x.start(startTime)).collect(Collectors.toList());
            for (int i = 0; i < params.getReadersCount(); i++) {
                callbackReaders.get(i).start(sbkCallbackReaders.get(i));
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
            timeoutExecutor.schedule(() -> stop(System.currentTimeMillis()),
                    params.getSecondsToRun() + 1, TimeUnit.SECONDS);
        }
        return ret;
    }

    /**
     * Stop/shutdown SBK Benchmark.
     *
     * closes all writers/readers.
     * closes the storage device/client.
     *
     * @param  endTime End Time.
     */
    @Override
    @Synchronized
    public void stop(long endTime) {
        if (writeStats != null && !params.isWriteAndRead()) {
            writeStats.stop(endTime);
        }
        if (readStats != null) {
            readStats.stop(endTime);
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
        if (callbackReaders != null) {
            callbackReaders.forEach(c -> {
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
