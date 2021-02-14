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

import io.sbk.api.Action;
import io.sbk.api.CallbackReader;
import io.sbk.api.Benchmark;
import io.sbk.api.DataReader;
import io.sbk.api.DataType;
import io.sbk.api.DataWriter;
import io.sbk.api.Logger;
import io.sbk.api.Parameters;
import io.sbk.api.Performance;
import io.sbk.api.Config;
import io.sbk.api.PeriodicLatencyRecorder;
import io.sbk.api.Storage;
import io.sbk.api.Time;
import lombok.Synchronized;

import javax.annotation.concurrent.GuardedBy;
import java.io.EOFException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

/**
 * Class for performing the benchmark.
 */
public class SbkBenchmark implements Benchmark {
    final private String storageName;
    final private Action action;
    final private Config config;
    final private Storage<Object> storage;
    final private Time time;
    final private Logger logger;
    final private ExecutorService executor;
    final private Parameters params;
    final private Performance writeStats;
    final private Performance readStats;
    final private int maxQs;
    final private double[] percentileFractions;
    final private ScheduledExecutorService timeoutExecutor;
    private List<DataWriter<Object>> writers;
    private List<DataReader<Object>> readers;
    private List<CallbackReader<Object>> callbackReaders;



    @GuardedBy("this")
    private CompletableFuture<Void> retFuture;

    /**
     * Create SBK Benchmark.
     *
     * @param  storageName          Storage Name
     * @param  action               Action
     * @param  config               Configuration parameters
     * @param  params               Benchmarking input Parameters
     * @param  storage              Storage device/client/driver for benchmarking
     * @param  logger               output logger
     * @param  time                 time interface
     * @throws IOException          If Exception occurs.
     */
    public SbkBenchmark(String storageName, Action action, Config config,
                        Parameters params, Storage<Object> storage, Logger logger, Time time) throws IOException {
        this.storageName = storageName;
        this.action = action;
        this.config = config;
        this.params = params;
        this.storage = storage;
        this.logger = logger;
        this.time = time;
        final double[] percentiles = logger.getPercentiles();
        percentileFractions = new double[percentiles.length];

        for (int i = 0; i < percentiles.length; i++) {
            percentileFractions[i] = percentiles[i] / 100.0;
        }

        if (config.maxQs > 0) {
            this.maxQs = config.maxQs;
        } else {
            this.maxQs = Math.max(Config.MIN_Q_PER_WORKER, config.qPerWorker);
        }

        final int threadCount = params.getWritersCount() + params.getReadersCount() + 10;
        if (config.fork) {
            executor = new ForkJoinPool(threadCount);
        } else {
            executor = Executors.newFixedThreadPool(threadCount);
        }
        if (params.getWritersCount() > 0 && !params.isWriteAndRead()) {
            writeStats = new SbkPerformance(config, params.getWritersCount(), createLatencyRecorder(),
                    logger.getReportingIntervalSeconds() * Config.MS_PER_SEC, this.time, executor);
        } else {
            writeStats = null;
        }

        if (params.getReadersCount() > 0) {
            readStats = new SbkPerformance(config, params.getReadersCount(), createLatencyRecorder(),
                    logger.getReportingIntervalSeconds() * Config.MS_PER_SEC, this.time, executor);
        } else {
            readStats = null;
        }
        timeoutExecutor = Executors.newScheduledThreadPool(1);
        retFuture = null;
    }


    private PeriodicLatencyRecorder createLatencyRecorder() {
        final long latencyRange = logger.getMaxLatency() - logger.getMinLatency();
        final long memSizeMB = (latencyRange * Config.LATENCY_VALUE_SIZE_BYTES) / (1024 * 1024);
        final LatencyWindow window;
        final PeriodicLatencyRecorder latencyRecorder;

        if (memSizeMB < config.maxArraySizeMB && latencyRange < Integer.MAX_VALUE) {
            window = new ArrayLatencyRecorder(logger.getMinLatency(), logger.getMaxLatency(),
                    Config.LONG_MAX, Config.LONG_MAX, Config.LONG_MAX, percentileFractions, time);
            SbkLogger.log.info("Window Latency Store: Array");
        } else {
            window = new HashMapLatencyRecorder(logger.getMinLatency(), logger.getMaxLatency(),
                    Config.LONG_MAX, Config.LONG_MAX, Config.LONG_MAX, percentileFractions, time, config.maxHashMapSizeMB);
            SbkLogger.log.info("Window Latency Store: HashMap");

        }
        if (config.csv) {
            latencyRecorder = new CompositeCSVLatencyRecorder(window, config.maxHashMapSizeMB, logger, logger::printTotal);
            SbkLogger.log.info("Total Window Latency Store: HashMap and CSV file");
        } else {
            latencyRecorder = new CompositeHashMapLatencyRecorder(window, config.maxHashMapSizeMB, logger, logger::printTotal);
            SbkLogger.log.info("Total Window Latency Store: HashMap");
        }
        return latencyRecorder;
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
        if (retFuture != null) {
            throw  new IllegalStateException("SbkBenchmark is already started\n");
        }
        logger.open(params, storageName, action, time);
        storage.openStorage(params);
        final DataType<Object> dType = storage.getDataType();
        final AtomicInteger readersErrCnt = new AtomicInteger(0);
        final AtomicInteger writersErrCnt = new AtomicInteger(0);
        final List<SbkWriter> sbkWriters;
        final List<SbkReader> sbkReaders;
        final List<SbkCallbackReader> sbkCallbackReaders;
        final List<CompletableFuture<Void>> writeFutures;
        final List<CompletableFuture<Void>> readFutures;
        final CompletableFuture<Void> wStatFuture;
        final CompletableFuture<Void> rStatFuture;
        final CompletableFuture<Void> chainFuture;
        final int readFuturesCnt;
        final int writeFuturesCnt;

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
                        .map(i -> new SbkWriter(i, maxQs, params, writeStats.getSendChannel(),
                                dType, time, writers.get(i)))
                        .collect(Collectors.toList());
            } else {
                sbkWriters = IntStream.range(0, params.getWritersCount())
                        .boxed()
                        .map(i -> new SbkWriter(i, maxQs,  params, null,
                                dType, time, writers.get(i)))
                        .collect(Collectors.toList());
            }
        } else {
            sbkWriters = null;
        }

        if (readers != null && readers.size() > 0) {
            sbkReaders = IntStream.range(0, params.getReadersCount())
                    .boxed()
                    .map(i -> new SbkReader(i, maxQs, params,
                            readStats.getSendChannel(), dType, time, readers.get(i)))
                    .collect(Collectors.toList());
            sbkCallbackReaders = null;
        } else if (callbackReaders != null && callbackReaders.size() > 0) {
            sbkCallbackReaders = IntStream.range(0, params.getReadersCount())
                    .boxed()
                    .map(i -> new SbkCallbackReader(i, maxQs, params,
                            readStats.getSendChannel(), dType, time))
                    .collect(Collectors.toList());
            sbkReaders = null;
        } else {
            sbkReaders = null;
            sbkCallbackReaders = null;
        }

        if (writeStats != null && !params.isWriteAndRead() && sbkWriters != null) {
            wStatFuture = writeStats.start(params.getSecondsToRun(), params.getSecondsToRun() <= 0 ?
                    params.getRecordsPerWriter() * params.getWritersCount() : 0);
        } else {
            wStatFuture = null;
        }
        if (readStats != null && (sbkReaders != null || sbkCallbackReaders != null)) {
            rStatFuture = readStats.start(params.getSecondsToRun(), params.getSecondsToRun() <= 0 ?
                    params.getRecordsPerReader() * params.getReadersCount() : 0);
        } else {
            rStatFuture = null;
        }
        if (sbkWriters != null) {
            writeFutures = sbkWriters.stream()
                    .map(x -> CompletableFuture.runAsync(() -> {
                        try {
                            x.run();
                        }  catch (IOException ex) {
                            ex.printStackTrace();
                            writersErrCnt.incrementAndGet();
                        }
                    }, executor)).collect(Collectors.toList());
            writeFuturesCnt = writeFutures.size();
        } else {
            writeFutures = null;
            writeFuturesCnt = 0;
        }

        if (sbkReaders != null) {
            readFutures = sbkReaders.stream()
                    .map(x -> CompletableFuture.runAsync(() -> {
                        try {
                            x.run();
                        } catch (EOFException ex) {
                            SbkLogger.log.info("Reader " + x.id +" exited with EOF");
                            readersErrCnt.incrementAndGet();
                        } catch (IOException ex) {
                            ex.printStackTrace();
                            readersErrCnt.incrementAndGet();
                        }
                    }, executor)).collect(Collectors.toList());
            readFuturesCnt = readFutures.size();
        } else if (sbkCallbackReaders != null) {
            readFutures = sbkCallbackReaders.stream()
                    .map(SbkCallbackReader::start).collect(Collectors.toList());
            for (int i = 0; i < params.getReadersCount(); i++) {
                callbackReaders.get(i).start(sbkCallbackReaders.get(i));
            }
            readFuturesCnt = readFutures.size();
        } else {
            readFutures = null;
            readFuturesCnt = 0;
        }

        if (writeFutures != null && readFutures != null) {
            chainFuture = CompletableFuture.allOf(Stream.concat(writeFutures.stream(), readFutures.stream()).
                    collect(Collectors.toList()).toArray(new CompletableFuture[writeFutures.size() + readFutures.size()]));
        } else if (readFutures != null) {
            chainFuture = CompletableFuture.allOf(new ArrayList<>(readFutures).toArray(new CompletableFuture[readFutures.size()]));
        } else if (writeFutures != null) {
            chainFuture = CompletableFuture.allOf(new ArrayList<>(writeFutures).toArray(new CompletableFuture[writeFutures.size()]));
        } else {
            throw new IllegalStateException("No Writers and/or Readers\n");
        }

        if (params.getSecondsToRun() > 0) {
            timeoutExecutor.schedule(this::stop, params.getSecondsToRun() + 1, TimeUnit.SECONDS);
        }

        retFuture = chainFuture.thenRunAsync(() -> {
            try {
                if ((wStatFuture != null) && (writeFuturesCnt != writersErrCnt.get())) {
                    wStatFuture.get();
                }

                if ((rStatFuture != null) && (readFuturesCnt != readersErrCnt.get()) ) {
                    rStatFuture.get();
                }
            }  catch (InterruptedException | ExecutionException ex) {
                shutdown(ex);
                return;
            }
            shutdown(null);
        }, executor);

        if (wStatFuture != null && !wStatFuture.isDone()) {
            wStatFuture.exceptionally(ex -> {
                shutdown(ex);
                return null;
            });
        }

        if (rStatFuture != null && !rStatFuture.isDone()) {
            rStatFuture.exceptionally(ex -> {
                shutdown(ex);
                return null;
            });
        }

        return retFuture;
    }

    /**
     * Shutdown SBK Benchmark.
     *
     * closes all writers/readers.
     * closes the storage device/client.
     *
     */
    @Synchronized
    private void shutdown(Throwable ex) {
        if (retFuture == null) {
            return;
        }

        if (retFuture.isDone()) {
            retFuture = null;
            return;
        }

        if (writeStats != null ) {
            writeStats.stop();
        }
        if (readStats != null) {
            readStats.stop();
        }
        if (readers != null) {
            readers.forEach(c -> {
                try {
                    c.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            });
        }
        if (callbackReaders != null) {
            callbackReaders.forEach(c -> {
                try {
                    c.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            });
        }
        if (writers != null) {
            writers.forEach(c -> {
                try {
                    c.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            });
        }
        try {
            storage.closeStorage(params);
            logger.close(params);
        } catch (IOException e) {
            e.printStackTrace();
        }
        executor.shutdown();
        try {
            executor.awaitTermination(1, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            ex.printStackTrace();
        }

        if (ex != null) {
            SbkLogger.log.warn("SBK Benchmark Shutdown with Exception "+ex.toString());
            retFuture.completeExceptionally(ex);
        } else {
            SbkLogger.log.info("SBK Benchmark Shutdown");
            retFuture.complete(null);
        }
        retFuture = null;
    }



    /**
     * Stop/shutdown SBK Benchmark.
     *
     * closes all writers/readers.
     * closes the storage device/client.
     *
     */
    @Override
    public void stop() {
        shutdown(null);
    }
}
