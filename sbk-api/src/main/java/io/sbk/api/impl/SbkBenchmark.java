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

import io.perl.api.Perl;
import io.perl.config.PerlConfig;
import io.perl.api.impl.PerlBuilder;
import io.sbk.action.Action;
import io.sbk.api.Benchmark;
import io.sbk.api.DataReader;
import io.sbk.api.DataWriter;
import io.sbk.params.ParameterOptions;
import io.sbk.api.Storage;
import io.sbk.config.Config;
import io.sbk.data.DataType;
import io.sbk.logger.RWLogger;
import io.sbk.system.Printer;
import io.state.State;
import io.time.Time;
import lombok.Synchronized;
import org.apache.commons.lang.StringUtils;
import org.jetbrains.annotations.NotNull;

import javax.annotation.concurrent.GuardedBy;
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

/**
 * Class for performing the benchmark.
 */
final public class SbkBenchmark implements Benchmark {
    final private static String CONFIGFILE = "sbk.properties";
    final private Storage<Object> storage;
    final private DataType<Object> dType;
    final private Time time;
    final private RWLogger rwLogger;
    final private ExecutorService executor;
    final private ParameterOptions params;
    final private Perl writePerl;
    final private Perl readPerl;
    final private ScheduledExecutorService timeoutExecutor;
    final private CompletableFuture<Void> retFuture;
    final private List<DataWriter<Object>> writers;
    final private List<DataReader<Object>> readers;

    @GuardedBy("this")
    private State state;

    /**
     * Create SBK Benchmark.
     *
     * @param params  Benchmarking input Parameters
     * @param storage Storage device/client/driver for benchmarking
     * @param dType   Data Type.
     * @param rwLogger  output logger
     * @param time    time interface
     * @throws IOException If Exception occurs.
     */
    public SbkBenchmark(ParameterOptions params, Storage<Object> storage,
                        DataType<Object> dType, @NotNull RWLogger rwLogger, Time time) throws IOException {
        this.dType = dType;
        this.params = params;
        this.storage = storage;
        this.rwLogger = rwLogger;
        this.time = time;

        final int threadCount = params.getWritersCount() + params.getReadersCount() + 23;
        executor = Config.FORK ? new ForkJoinPool(threadCount) : Executors.newFixedThreadPool(threadCount);

        if (params.getWritersCount() > 0 && params.getAction() == Action.Writing) {
            PerlConfig wConfig = PerlConfig.build(SbkBenchmark.class.getClassLoader().getResourceAsStream(CONFIGFILE));
            wConfig.workers = params.getWritersCount();
            wConfig.csv = false;
            writePerl = PerlBuilder.build(rwLogger, rwLogger, this.time, wConfig, executor);
        } else {
            writePerl = null;
        }

        if (params.getReadersCount() > 0) {
            PerlConfig rConfig = PerlConfig.build(SbkBenchmark.class.getClassLoader().getResourceAsStream(CONFIGFILE));
            rConfig.workers = params.getReadersCount();
            rConfig.csv = false;
            readPerl = PerlBuilder.build(rwLogger, rwLogger, this.time, rConfig, executor);
        } else {
            readPerl = null;
        }

        timeoutExecutor = Executors.newScheduledThreadPool(1);
        retFuture = new CompletableFuture<>();
        writers = new ArrayList<>();
        readers = new ArrayList<>();
        state = State.BEGIN;
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
            return retFuture.toCompletableFuture();
        }
        state = State.RUN;
        Printer.log.info("SBK Benchmark Started");
        rwLogger.open(params, StringUtils.capitalize(storage.getClass().getSimpleName().toLowerCase()),
                params.getAction(), time);
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
            if (writePerl != null) {
                sbkWriters = IntStream.range(0, params.getWritersCount())
                        .boxed()
                        .map(i -> new SbkWriter(i, params, writePerl.getPerlChannel(),
                                dType, time, writers.get(i), rwLogger, executor))
                        .collect(Collectors.toList());
            } else {
                sbkWriters = IntStream.range(0, params.getWritersCount())
                        .boxed()
                        .map(i -> new SbkWriter(i, params, null,
                                dType, time, writers.get(i), rwLogger, executor))
                        .collect(Collectors.toList());
            }
        } else {
            sbkWriters = null;
        }

        if (readers.size() > 0) {
            sbkReaders = IntStream.range(0, params.getReadersCount())
                    .boxed()
                    .map(i -> new SbkReader(i, params,
                            readPerl.getPerlChannel(), dType, time, readers.get(i),
                            rwLogger, executor))
                    .collect(Collectors.toList());
        } else {
            sbkReaders = null;
        }

        if (writePerl != null && params.getAction() == Action.Writing && sbkWriters != null) {
            wStatFuture = writePerl.run(params.getTotalSecondsToRun(), params.getTotalRecords());
        } else {
            wStatFuture = null;
        }
        if (readPerl != null && sbkReaders != null) {
            rStatFuture = readPerl.run(params.getTotalSecondsToRun(), params.getTotalRecords());
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
        rwLogger.setExceptionHandler(this::shutdown);
        assert chainFuture != null;
        chainFuture.thenRunAsync(this::stop, executor);

        return retFuture.toCompletableFuture();
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
        if (writePerl != null) {
            writePerl.stop();
        }
        if (readPerl != null) {
            readPerl.stop();
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
            rwLogger.close(params);
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
