/**
 * Copyright (c) KMG. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 */
package io.sbk.api.impl;

import io.perl.Perl;
import io.sbk.action.Action;
import io.sbk.api.Benchmark;
import io.sbk.api.DataReader;
import io.sbk.api.DataWriter;
import io.sbk.api.ParameterOptions;
import io.sbk.api.Storage;
import io.sbk.config.Config;
import io.perl.PerlConfig;
import io.sbk.data.DataType;
import io.sbk.logger.Logger;
import io.perl.LatencyRecordWindow;
import io.perl.PeriodicLogger;
import io.perl.impl.ArrayLatencyRecorder;
import io.perl.impl.CQueuePerl;
import io.perl.impl.CSVExtendedLatencyRecorder;
import io.perl.impl.HashMapLatencyRecorder;
import io.perl.impl.HdrExtendedLatencyRecorder;
import io.perl.impl.TotalWindowLatencyPeriodicLogger;
import io.state.State;
import io.sbk.system.Printer;
import io.time.Time;
import lombok.Synchronized;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import javax.annotation.concurrent.GuardedBy;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * Class for performing the benchmark.
 */
final public class SbkBenchmark implements Benchmark {
    final private Action action;
    final private PerlConfig perlConfig;
    final private Storage<Object> storage;
    final private DataType<Object> dType;
    final private Time time;
    final private Logger logger;
    final private ExecutorService executor;
    final private ParameterOptions params;
    final private Perl writeStats;
    final private Perl readStats;
    final private int maxQs;
    final private double[] percentileFractions;
    final private ScheduledExecutorService timeoutExecutor;
    final private CompletableFuture<Void> retFuture;
    final private List<DataWriter<Object>> writers;
    final private List<DataReader<Object>> readers;

    @GuardedBy("this")
    private State state;

    /**
     * Create SBK Benchmark.
     *
     * @param action     Action
     * @param perlConfig Configuration parameters
     * @param params     Benchmarking input Parameters
     * @param storage    Storage device/client/driver for benchmarking
     * @param dType      Data Type.
     * @param logger     output logger
     * @param time       time interface
     * @throws IOException If Exception occurs.
     */
    public SbkBenchmark(Action action, PerlConfig perlConfig,
                        ParameterOptions params, Storage<Object> storage,
                        DataType<Object> dType, @NotNull Logger logger, Time time) throws IOException {
        this.dType = dType;
        this.action = action;
        this.perlConfig = perlConfig;
        this.params = params;
        this.storage = storage;
        this.logger = logger;
        this.time = time;
        final double[] percentiles = logger.getPercentiles();
        percentileFractions = new double[percentiles.length];

        for (int i = 0; i < percentiles.length; i++) {
            percentileFractions[i] = percentiles[i] / 100.0;
        }

        this.maxQs = perlConfig.maxQs > 0 ?
                perlConfig.maxQs : Math.max(PerlConfig.MIN_Q_PER_WORKER, perlConfig.qPerWorker);

        final int threadCount = params.getWritersCount() + params.getReadersCount() + 23;
        executor = Config.FORK ? new ForkJoinPool(threadCount) : Executors.newFixedThreadPool(threadCount);
        writeStats = params.getWritersCount() > 0 && !params.isWriteAndRead() ?
                new CQueuePerl(perlConfig, params.getWritersCount(), createLatencyRecorder(),
                logger.getReportingIntervalSeconds() * Time.MS_PER_SEC, params.getTimeoutMS(),
                        this.time, executor) : null;

        readStats = params.getReadersCount() > 0 ?
                new CQueuePerl(perlConfig, params.getReadersCount(), createLatencyRecorder(),
                logger.getReportingIntervalSeconds() * Time.MS_PER_SEC, params.getTimeoutMS(),
                        this.time, executor) : null;
        timeoutExecutor = Executors.newScheduledThreadPool(1);
        retFuture = new CompletableFuture<>();
        writers = new ArrayList<>();
        readers = new ArrayList<>();
        state = State.BEGIN;
    }

    private LatencyRecordWindow createLatencyWindow() {
        final long latencyRange = logger.getMaxLatency() - logger.getMinLatency();
        final long memSizeMB = (latencyRange * PerlConfig.LATENCY_VALUE_SIZE_BYTES) / PerlConfig.BYTES_PER_MB;
        final LatencyRecordWindow window;

        if (memSizeMB < perlConfig.maxArraySizeMB && latencyRange < Integer.MAX_VALUE) {
            window = new ArrayLatencyRecorder(logger.getMinLatency(), logger.getMaxLatency(),
                    PerlConfig.TOTAL_LATENCY_MAX, PerlConfig.LONG_MAX, PerlConfig.LONG_MAX, percentileFractions, time);
            Printer.log.info("Window Latency Store: Array, Size: " +
                    window.getMaxMemoryBytes() / PerlConfig.BYTES_PER_MB + " MB");
        } else {
            window = new HashMapLatencyRecorder(logger.getMinLatency(), logger.getMaxLatency(),
                    PerlConfig.TOTAL_LATENCY_MAX, PerlConfig.LONG_MAX, PerlConfig.LONG_MAX, percentileFractions,
                    time, perlConfig.maxHashMapSizeMB);
            Printer.log.info("Window Latency Store: HashMap, Size: " +
                    window.getMaxMemoryBytes() / PerlConfig.BYTES_PER_MB + " MB");
        }
        return window;
    }

    @Contract(" -> new")
    private @NotNull PeriodicLogger createLatencyRecorder() {
        final long latencyRange = logger.getMaxLatency() - logger.getMinLatency();
        final long memSizeMB = (latencyRange * PerlConfig.LATENCY_VALUE_SIZE_BYTES) / PerlConfig.BYTES_PER_MB;
        final LatencyRecordWindow window;
        final LatencyRecordWindow totalWindow;
        final LatencyRecordWindow totalWindowExtension;

        window = createLatencyWindow();

        totalWindow = new HashMapLatencyRecorder(logger.getMinLatency(), logger.getMaxLatency(),
                PerlConfig.TOTAL_LATENCY_MAX, PerlConfig.LONG_MAX, PerlConfig.LONG_MAX, percentileFractions,
                time, perlConfig.totalMaxHashMapSizeMB);
        Printer.log.info("Total Window Latency Store: HashMap, Size: " +
                totalWindow.getMaxMemoryBytes() / PerlConfig.BYTES_PER_MB + " MB");

        if (perlConfig.histogram) {
            totalWindowExtension = new HdrExtendedLatencyRecorder(logger.getMinLatency(), logger.getMaxLatency(),
                    PerlConfig.TOTAL_LATENCY_MAX, PerlConfig.LONG_MAX, PerlConfig.LONG_MAX,
                    percentileFractions, time, totalWindow);
            Printer.log.info(String.format("Total Window Extension: HdrHistogram, Size: %.2f MB",
                    (totalWindowExtension.getMaxMemoryBytes() * 1.0) / PerlConfig.BYTES_PER_MB));
        } else if (perlConfig.csv) {
            totalWindowExtension = new CSVExtendedLatencyRecorder(logger.getMinLatency(), logger.getMaxLatency(),
                    PerlConfig.TOTAL_LATENCY_MAX, PerlConfig.LONG_MAX, PerlConfig.LONG_MAX,
                    percentileFractions, time, totalWindow, perlConfig.csvFileSizeGB,
                    Config.NAME + "-" + String.format("%06d", new Random().nextInt(1000000)) + ".csv");
            Printer.log.info("Total Window Extension: CSV, Size: " +
                    totalWindowExtension.getMaxMemoryBytes() / PerlConfig.BYTES_PER_GB + " GB");
        } else {
            totalWindowExtension = totalWindow;
            Printer.log.info("Total Window Extension: None, Size: 0 MB");
        }

        return new TotalWindowLatencyPeriodicLogger(window, totalWindowExtension, logger, logger::printTotal,
                logger, time);
    }

    /**
     * Start SBK Benchmark.
     *
     * opens the storage device/client , creates the writers/readers.
     * conducts the performance benchmarking for given time in seconds
     * or exits if the input the number of records are written/read.
     * NOTE: This method does NOT invoke parsing of parameters, storage device/client.
     *
     * @throws IOException           If an exception occurred.
     * @throws IllegalStateException If an exception occurred.
     */
    @Override
    @Synchronized
    public CompletableFuture<Void> start() throws IOException, InterruptedException, ExecutionException,
            IllegalStateException {
        if (state != State.BEGIN) {
            if (state == State.RUN) {
                Printer.log.warn("SBK Benchmark is already running..");
            } else {
                Printer.log.warn("SBK Benchmark is already shutdown..");
            }
            return retFuture;
        }
        state = State.RUN;
        Printer.log.info("SBK Benchmark Started");
        logger.open(params, storage.getClass().getSimpleName(), action, time);
        storage.openStorage(params);
        final List<SbkWriter> sbkWriters;
        final List<SbkReader> sbkReaders;
        final List<CompletableFuture<Void>> writeFutures;
        final List<CompletableFuture<Void>> readFutures;
        final CompletableFuture<Void> wStatFuture;
        final CompletableFuture<Void> rStatFuture;
        final CompletableFuture<Void> chainFuture;
        final CompletableFuture<Void> writersCB;
        final CompletableFuture<Void> readersCB;

        for (int i = 0; i < params.getWritersCount(); i++) {
            final DataWriter<Object> writer = storage.createWriter(i, params);
            if (writer != null) {
                writers.add(writer);
            }
        }

        for (int i = 0; i < params.getReadersCount(); i++) {
            final DataReader<Object> reader = storage.createReader(i, params);
            if (reader != null) {
                readers.add(reader);
            }
        }

        if (writers.size() <= 0 && readers.size() <= 0) {
            throw new IllegalStateException("No Writers and/or Readers Created\n");
        }

        if (writers.size() > 0) {
            if (writeStats != null) {
                sbkWriters = IntStream.range(0, params.getWritersCount())
                        .boxed()
                        .map(i -> new SbkWriter(i, maxQs, params, writeStats.getPerlChannel(),
                                dType, time, writers.get(i), logger, executor))
                        .collect(Collectors.toList());
            } else {
                sbkWriters = IntStream.range(0, params.getWritersCount())
                        .boxed()
                        .map(i -> new SbkWriter(i, maxQs, params, null,
                                dType, time, writers.get(i), logger, executor))
                        .collect(Collectors.toList());
            }
        } else {
            sbkWriters = null;
        }

        if (readers.size() > 0) {
            sbkReaders = IntStream.range(0, params.getReadersCount())
                    .boxed()
                    .map(i -> new SbkReader(i, maxQs, params,
                            readStats.getPerlChannel(), dType, time, readers.get(i),
                            logger, executor))
                    .collect(Collectors.toList());
        } else {
            sbkReaders = null;
        }

        if (writeStats != null && !params.isWriteAndRead() && sbkWriters != null) {
            wStatFuture = writeStats.run(params.getTotalSecondsToRun(), params.getTotalRecords());
        } else {
            wStatFuture = null;
        }
        if (readStats != null && sbkReaders != null) {
            rStatFuture = readStats.run(params.getTotalSecondsToRun(), params.getTotalRecords());
        } else {
            rStatFuture = null;
        }
        if (sbkWriters != null) {
            writeFutures = new ArrayList<>();

            final long recordsPerWriter = params.getTotalSecondsToRun() <= 0 ?
                    params.getTotalRecords() / params.getWritersCount() : 0;
            final long delta = recordsPerWriter > 0 ?
                    params.getTotalRecords() - (recordsPerWriter * params.getWritersCount()) : 0;

            writersCB = CompletableFuture.runAsync(() -> {
                long secondsToRun = params.getTotalSecondsToRun();
                boolean doWork = true;
                int i = 0;
                while (i < params.getWritersCount() && doWork) {
                    final int stepCnt = Math.min(params.getWritersStep(), params.getWritersCount() - i);
                    for (int j = 0; j < stepCnt; j++) {
                        try {
                            CompletableFuture<Void> ret = sbkWriters.get(i + j).run(secondsToRun,
                                    i + j + 1 == params.getWritersCount() ?
                                            recordsPerWriter + delta : recordsPerWriter);
                            writeFutures.add(ret);
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                    i += params.getWritersStep();
                    if (params.getWritersStepSeconds() > 0 && i < params.getWritersCount()) {
                        try {
                            Thread.sleep((long) params.getWritersStepSeconds() * Time.MS_PER_SEC);
                            if (params.getTotalSecondsToRun() > 0) {
                                secondsToRun -= params.getWritersStepSeconds();
                                if (secondsToRun <= 0) {
                                    doWork = false;
                                }
                            }
                        } catch (InterruptedException ex) {
                            ex.printStackTrace();
                        }
                    }
                }
            }, executor).thenAccept(d -> {
                try {
                    CompletableFuture.allOf(writeFutures.toArray(new CompletableFuture[0])).get();
                } catch (InterruptedException | ExecutionException e) {
                    e.printStackTrace();
                }
            });
            Printer.log.info("SBK Benchmark initiated Writers");

        } else {
            writersCB = null;
            writeFutures = null;
        }

        if (sbkReaders != null) {
            readFutures = new ArrayList<>();

            final long recordsPerReader = params.getTotalSecondsToRun() <= 0 ?
                    params.getTotalRecords() / params.getReadersCount() : 0;
            final long delta = recordsPerReader > 0 ?
                    params.getTotalRecords() - (recordsPerReader * params.getReadersCount()) : 0;

            readersCB = CompletableFuture.runAsync(() -> {
                long secondsToRun = params.getTotalSecondsToRun();
                boolean doWork = true;
                int i = 0;
                while (i < params.getReadersCount() && doWork) {
                    int stepCnt = Math.min(params.getReadersStep(), params.getReadersCount() - i);
                    for (int j = 0; j < stepCnt; j++) {
                        try {
                            CompletableFuture<Void> ret = sbkReaders.get(i + j).run(secondsToRun, i + j + 1 == params.getReadersCount() ?
                                    recordsPerReader + delta : recordsPerReader);
                            readFutures.add(ret);
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                    i += params.getReadersStep();
                    if (params.getReadersStepSeconds() > 0 && i < params.getReadersCount()) {
                        try {
                            Thread.sleep((long) params.getReadersStepSeconds() * Time.MS_PER_SEC);
                            if (params.getTotalSecondsToRun() > 0) {
                                secondsToRun -= params.getReadersStepSeconds();
                                if (secondsToRun <= 0) {
                                    doWork = false;
                                }
                            }
                        } catch (InterruptedException ex) {
                            ex.printStackTrace();
                        }
                    }
                }
            }, executor).thenAccept(d -> {
                        try {
                            CompletableFuture.allOf(readFutures.toArray(new CompletableFuture[0])).get();
                        } catch (InterruptedException | ExecutionException e) {
                            e.printStackTrace();
                        }
                    }
            );
            Printer.log.info("SBK Benchmark initiated Readers");
        } else {
            readersCB = null;
            readFutures = null;
        }

        if (writersCB != null && readersCB != null) {
            chainFuture = CompletableFuture.allOf(writersCB, readersCB);
        } else if (readFutures != null) {
            chainFuture = readersCB;
        } else {
            chainFuture = writersCB;
        }

        if (params.getTotalSecondsToRun() > 0) {
            timeoutExecutor.schedule(this::stop, params.getTotalSecondsToRun() + 1, TimeUnit.SECONDS);
        }

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
        logger.setExceptionHandler(this::shutdown);
        assert chainFuture != null;
        chainFuture.thenRunAsync(this::stop, executor);

        return retFuture;
    }

    /**
     * Shutdown SBK Benchmark.
     *
     * closes all writers/readers.
     * closes the storage device/client.
     *
     * @param ex Throwable exception
     */
    @Synchronized
    private void shutdown(Throwable ex) {
        if (state == State.END) {
            return;
        }
        state = State.END;
        if (writeStats != null) {
            writeStats.stop();
        }
        if (readStats != null) {
            readStats.stop();
        }
        readers.forEach(c -> {
            try {
                c.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
        writers.forEach(c -> {
            try {
                c.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
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
            Printer.log.warn("SBK Benchmark Shutdown with Exception " + ex);
            retFuture.completeExceptionally(ex);
        } else {
            Printer.log.info("SBK Benchmark Shutdown");
            retFuture.complete(null);
        }

    }


    /**
     * Stop/shutdown SBK Benchmark.
     *
     * closes all writers/readers.
     * closes the storage device/client.
     */
    @Override
    public void stop() {
        shutdown(null);
    }
}
