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

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.perl.api.BenchmarkTermination;
import io.perl.api.Perl;
import io.perl.config.PerlConfig;
import io.perl.exception.BenchmarkIdleTimeoutException;
import io.perl.api.impl.PerlBuilder;
import io.sbk.action.Action;
import io.sbk.config.SbkRuntimeConfig;
import io.sbk.api.Benchmark;
import io.sbk.api.DataReader;
import io.sbk.api.DataWriter;
import io.sbk.logger.ReadRequestsLogger;
import io.sbk.logger.WriteRequestsLogger;
import io.sbk.params.ParameterOptions;
import io.sbk.params.impl.SbkParameters;
import io.sbk.api.Storage;
import io.sbk.data.DataType;
import io.sbk.logger.RWLogger;
import io.sbk.system.Printer;
import io.sbk.thread.ThreadType;
import io.state.State;
import io.time.Time;
import lombok.Synchronized;
import org.jetbrains.annotations.NotNull;

import javax.annotation.concurrent.GuardedBy;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * Class for performing the benchmark.
 *
 * <p>This class orchestrates the full lifecycle of an SBK benchmark run:
 * it opens the storage client, constructs writer and reader instances,
 * wires PerL-based metric collectors, schedules timeouts and executes
 * the writers/readers concurrently using an executor pool.
 *
 * <p>Responsibilities include:
 * <ul>
 *   <li>Opening and closing the storage device lifecycle via {@link io.sbk.api.Storage}.</li>
 *   <li>Creating and managing multiple {@link io.sbk.api.DataWriter} and {@link io.sbk.api.DataReader} instances.</li>
 *   <li>Coordinating PerL metric collectors for writers and readers when enabled.</li>
 *   <li>Providing a fault-tolerant shutdown path and reporting via the configured {@link io.sbk.logger.RWLogger}.</li>
 * </ul>
 *
 * <p>Notes for maintainers:
 * <ul>
 *   <li>The class reserves executor threads based on configured writers/readers; adjust the thread accounting carefully.</li>
 *   <li>Perl-based metrics and timeouts are configured conditionally depending on writer/reader counts.</li>
 * </ul>
 */
final public class SbkBenchmark implements Benchmark {
    final private static SbkRuntimeConfig RUNTIME_CONFIG = SbkRuntimeConfig.get();
    final private Storage<Object> storage;
    final private DataType<Object> dType;
    final private Time time;
    final private RWLogger rwLogger;
    final private ExecutorService executor;
    final private ExecutorService perlExecutor;
    final private ExecutorService lifecycleExecutor;
    final private ParameterOptions params;
    final private Perl writePerl;
    final private Perl readPerl;
    final private ScheduledExecutorService timeoutExecutor;
    final private CompletableFuture<Void> retFuture;
    final private List<DataWriter<Object>> writers;
    final private List<DataReader<Object>> readers;

    @GuardedBy("this")
    private State state;

    @GuardedBy("this")
    private boolean shutdownRequested;

    @GuardedBy("this")
    private CompletableFuture<Void> writePerlCompletion;

    @GuardedBy("this")
    private CompletableFuture<Void> readPerlCompletion;

    @GuardedBy("this")
    private CompletableFuture<Void> workerCompletion;

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
    @SuppressFBWarnings("EI_EXPOSE_REP2")
    public SbkBenchmark(ParameterOptions params, Storage<Object> storage,
                        DataType<Object> dType, @NotNull RWLogger rwLogger, Time time) throws IOException {
        this.dType = dType;
        this.params = params;
        this.storage = storage;
        this.rwLogger = rwLogger;
        this.time = time;

        final int threadCount = params.getWritersCount() + params.getReadersCount()
                + RUNTIME_CONFIG.workerExecutorReserve;

        this.executor = switch (params.getThreadType()) {
            case ThreadType.ForkJoin -> new ForkJoinPool(threadCount);
            case ThreadType.Virtual -> Executors.newFixedThreadPool(threadCount, Thread.ofVirtual().factory());
            default -> Executors.newFixedThreadPool(threadCount);
        };

        this.perlExecutor = new ForkJoinPool(RUNTIME_CONFIG.perlExecutorParallelism);
        this.lifecycleExecutor = Executors.newSingleThreadExecutor(Thread.ofPlatform()
                .name("sbk-benchmark-lifecycle").factory());

        if (params.getWritersCount() > 0 && params.getAction() == Action.Writing) {
            PerlConfig wConfig = buildPerlConfig(params);
            wConfig.workers = params.getWritersCount();
            wConfig.sleepMS = params.getIdleSleepMilliSeconds();
            wConfig.csv = false;
            writePerl = PerlBuilder.build(rwLogger, this.time, wConfig, this.perlExecutor);
        } else {
            writePerl = null;
        }

        if (params.getReadersCount() > 0) {
            PerlConfig rConfig = buildPerlConfig(params);
            rConfig.workers = params.getReadersCount();
            rConfig.sleepMS = params.getIdleSleepMilliSeconds();
            rConfig.csv = false;
            readPerl = PerlBuilder.build(rwLogger, this.time, rConfig, this.perlExecutor);
        } else {
            readPerl = null;
        }

        timeoutExecutor = Executors.newSingleThreadScheduledExecutor(Thread.ofPlatform()
                .name("sbk-benchmark-deadline").daemon(true).factory());
        retFuture = new CompletableFuture<>();
        writers = new ArrayList<>();
        readers = new ArrayList<>();
        state = State.BEGIN;
        shutdownRequested = false;
        writePerlCompletion = null;
        readPerlCompletion = null;
        workerCompletion = null;
    }

    /**
     * Build the bundled PerL configuration and apply command-line queue
     * overrides.
     *
     * @param params parsed SBK parameters
     * @return effective PerL configuration
     * @throws IOException if the bundled configuration cannot be loaded
     */
    static PerlConfig buildPerlConfig(ParameterOptions params)
            throws IOException {
        final PerlConfig config = SbkParameters.loadPerlConfig();
        applyMpscQueueOption(params, config);
        return config;
    }

    /**
     * Apply the optional timestamp queue implementation override.
     *
     * @param params parsed SBK parameters
     * @param config PerL configuration to update
     */
    static void applyMpscQueueOption(ParameterOptions params,
                                     PerlConfig config) {
        config.mpscQueueEnable = params.isMpscQueueEnabled();
        config.idleTimeoutSeconds = params.getIdleTimeoutSeconds();
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
        storage.openStorage(params);
        final List<SbkWriter> sbkWriters;
        final List<SbkReader> sbkReaders;
        final List<CompletableFuture<Void>> writeFutures;
        final List<CompletableFuture<Void>> readFutures;
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

        /*
         * Prepare storage workers before a distributed logger registers with
         * SBM. Registration is the coordinated-start readiness boundary; a
         * slow driver open must not make an unready client appear runnable.
         */
        rwLogger.open(params, storage.getClass().getSimpleName(), params.getAction(), time);
        final WriteRequestsLogger writeRequestsLogger = rwLogger.getMaxWriterIDs() > 0 ? rwLogger : null;
        final ReadRequestsLogger readRequestsLogger = rwLogger.getMaxReaderIDs() > 0 ? rwLogger : null;

        if (writers.size() > 0) {
            if (writePerl != null) {
                sbkWriters = IntStream.range(0, params.getWritersCount())
                        .boxed()
                        .map(i -> new SbkWriter(i, params, writePerl.getPerlChannel(),
                                dType, time, writers.get(i), rwLogger, writeRequestsLogger, executor))
                        .collect(Collectors.toList());
            } else {
                sbkWriters = IntStream.range(0, params.getWritersCount())
                        .boxed()
                        .map(i -> new SbkWriter(i, params, null,
                                dType, time, writers.get(i), rwLogger, writeRequestsLogger, executor))
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
                            rwLogger, readRequestsLogger, executor))
                    .collect(Collectors.toList());
        } else {
            sbkReaders = null;
        }

        if (writePerl != null && params.getAction() == Action.Writing && sbkWriters != null) {
            writePerlCompletion = writePerl.run(params.getTotalSecondsToRun(), params.getTotalRecords());
        } else {
            writePerlCompletion = null;
        }
        if (readPerl != null && sbkReaders != null) {
            readPerlCompletion = readPerl.run(params.getTotalSecondsToRun(), params.getTotalRecords());
        } else {
            readPerlCompletion = null;
        }
        if (sbkWriters != null) {
            writeFutures = new ArrayList<>();

            writersCB = CompletableFuture.runAsync(() -> {
                long secondsToRun = params.getTotalSecondsToRun();
                boolean doWork = true;
                int i = 0;
                while (i < params.getWritersCount() && doWork) {
                    final int stepCnt = Math.min(params.getWritersStep(), params.getWritersCount() - i);
                    for (int j = 0; j < stepCnt; j++) {
                        final int workerIndex = i + j;
                        final long workerSecondsToRun = secondsToRun;
                        CompletableFuture<Void> ret = startWorker(
                                () -> sbkWriters.get(workerIndex).run(workerSecondsToRun,
                                    recordsForWorker(params.getTotalRecords(),
                                            params.getWritersCount(), workerIndex)),
                                this::requestShutdown);
                        ret.whenComplete((ignored, failure) -> {
                            if (failure != null) {
                                requestShutdown(failure);
                            }
                        });
                        writeFutures.add(ret);
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
                            Thread.currentThread().interrupt();
                            Printer.log.info("Writer ramp-up interrupted at benchmark deadline");
                            return;
                        }
                    }
                }
            }, executor).thenCompose(d ->
                    CompletableFuture.allOf(writeFutures.toArray(new CompletableFuture[0])));
            Printer.log.info("SBK Benchmark initiated Writers");

        } else {
            writersCB = null;
            writeFutures = null;
        }

        if (sbkReaders != null) {
            readFutures = new ArrayList<>();

            readersCB = CompletableFuture.runAsync(() -> {
                long secondsToRun = params.getTotalSecondsToRun();
                boolean doWork = true;
                int i = 0;
                while (i < params.getReadersCount() && doWork) {
                    int stepCnt = Math.min(params.getReadersStep(), params.getReadersCount() - i);
                    for (int j = 0; j < stepCnt; j++) {
                        final int workerIndex = i + j;
                        final long workerSecondsToRun = secondsToRun;
                        CompletableFuture<Void> ret = startWorker(
                                () -> sbkReaders.get(workerIndex).run(workerSecondsToRun,
                                    recordsForWorker(params.getTotalRecords(),
                                            params.getReadersCount(), workerIndex)),
                                this::requestShutdown);
                        ret.whenComplete((ignored, failure) -> {
                            if (failure != null) {
                                requestShutdown(failure);
                            }
                        });
                        readFutures.add(ret);
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
                            Thread.currentThread().interrupt();
                            Printer.log.info("Reader ramp-up interrupted at benchmark deadline");
                            return;
                        }
                    }
                }
            }, executor).thenCompose(d ->
                    CompletableFuture.allOf(readFutures.toArray(new CompletableFuture[0])));
            Printer.log.info("SBK Benchmark initiated Readers");
        } else {
            readersCB = null;
            readFutures = null;
        }

        chainFuture = allWorkers(writersCB, readersCB);
        workerCompletion = chainFuture;

        if (params.getTotalSecondsToRun() > 0) {
            timeoutExecutor.schedule(this::requestTimedShutdown,
                    params.getTotalSecondsToRun(), TimeUnit.SECONDS);
        }

        if (writePerlCompletion != null) {
            writePerlCompletion.exceptionally(ex -> {
                requestShutdown(ex);
                return null;
            });
        }

        if (readPerlCompletion != null) {
            readPerlCompletion.exceptionally(ex -> {
                requestShutdown(ex);
                return null;
            });
        }
        rwLogger.setExceptionHandler(this::requestShutdown);
        assert chainFuture != null;
        chainFuture.whenComplete((ignored, ex) -> requestShutdown(ex,
                BenchmarkTermination.configured(params.getTotalSecondsToRun(), params.getTotalRecords())));

        return retFuture.toCompletableFuture();
    }

    /**
     * Completes after every configured writer and reader worker has exited.
     *
     * @param writerWorkers completion of all writers, or {@code null}
     * @param readerWorkers completion of all readers, or {@code null}
     * @return completion of every configured worker group
     * @throws IllegalArgumentException when neither worker group exists
     */
    static CompletableFuture<Void> allWorkers(CompletableFuture<Void> writerWorkers,
                                              CompletableFuture<Void> readerWorkers) {
        if (writerWorkers != null && readerWorkers != null) {
            return CompletableFuture.allOf(writerWorkers, readerWorkers);
        }
        if (readerWorkers != null) {
            return readerWorkers;
        }
        if (writerWorkers != null) {
            return writerWorkers;
        }
        throw new IllegalArgumentException("No writer or reader workers");
    }

    /**
     * Starts one worker and requests benchmark shutdown if admission fails synchronously.
     *
     * <p>This is a worker-lifecycle boundary and is not invoked for each record.
     *
     * @param starter worker-start operation
     * @param shutdownRequest shutdown callback
     * @return asynchronous worker completion
     * @throws CompletionException when worker admission fails
     */
    static CompletableFuture<Void> startWorker(WorkerStarter starter,
                                               Consumer<Throwable> shutdownRequest) {
        try {
            return starter.start();
        } catch (IOException exception) {
            shutdownRequest.accept(exception);
            throw new CompletionException(exception);
        }
    }

    /**
     * Starts a worker whose admission can fail with an I/O exception.
     */
    @FunctionalInterface
    interface WorkerStarter {
        CompletableFuture<Void> start() throws IOException;
    }

    /**
     * Schedules automatic benchmark shutdown away from worker, timeout, PerL, and logger threads.
     *
     * <p>The shutdown path interrupts and awaits the worker executor. Running that path on a worker
     * would interrupt the shutdown thread itself and produce a false shutdown warning.
     *
     * @param ex failure that initiated shutdown, or {@code null} for normal completion
     */
    private void requestShutdown(Throwable ex) {
        requestShutdown(ex, BenchmarkTermination.INTERNAL_FAILURE);
    }

    @Synchronized
    private void requestShutdown(Throwable ex, BenchmarkTermination requestedTermination) {
        if (state == State.END || shutdownRequested || lifecycleExecutor.isShutdown()) {
            return;
        }
        shutdownRequested = true;
        final BenchmarkIdleTimeoutException idleTimeout = BenchmarkIdleTimeoutException.find(ex);
        if (idleTimeout != null) {
            Printer.log.warn("SBK benchmark idle timeout: {}", idleTimeout.getMessage());
        }
        timeoutExecutor.schedule(() -> forceShutdownCompletion(ex),
                RUNTIME_CONFIG.forcedShutdownGraceSeconds, TimeUnit.SECONDS);
        lifecycleExecutor.execute(() -> shutdown(ex, requestedTermination));
    }

    /**
     * Stops worker admission at the configured deadline and starts bounded cleanup.
     */
    private void requestTimedShutdown() {
        executor.shutdownNow();
        requestShutdown(null, BenchmarkTermination.SECONDS_COMPLETED);
    }

    /**
     * Releases the application after the bounded cleanup grace period.
     *
     * <p>The executable main methods call {@code System.exit} after this future completes,
     * so a driver or SDK blocked in close cannot extend a timed run indefinitely.
     *
     * @param failure failure that initiated shutdown, or {@code null} for an orderly shutdown
     */
    private void forceShutdownCompletion(Throwable failure) {
        final Throwable terminalFailure = unwrapCompletionFailure(failure);
        final boolean completed = terminalFailure == null
                ? retFuture.complete(null) : retFuture.completeExceptionally(terminalFailure);
        if (completed) {
            Printer.log.warn("SBK benchmark cleanup exceeded "
                    + RUNTIME_CONFIG.forcedShutdownGraceSeconds
                    + " seconds; forcing application exit");
            executor.shutdownNow();
            perlExecutor.shutdownNow();
            lifecycleExecutor.shutdownNow();
        }
    }

    /**
     * Shutdown SBK Benchmark.
     *
     * closes all writers/readers.
     * closes the storage device/client.
     *
     * @param ex Throwable exception
     * @param requestedTermination lifecycle completion expected by the caller
     */
    @Synchronized
    private void shutdown(Throwable ex, BenchmarkTermination requestedTermination) {
        if (state == State.END) {
            return;
        }
        state = State.END;
        Throwable terminalFailure = unwrapCompletionFailure(ex);
        if (terminalFailure != null) {
            try {
                rwLogger.reportFailure(terminalFailure);
            } catch (RuntimeException reportFailure) {
                terminalFailure = retainFailure(terminalFailure, reportFailure);
            }
        }
        executor.shutdownNow();
        boolean workersClosed = false;
        boolean storageClosed = false;
        WorkerCompletion workers = awaitWorkers();
        terminalFailure = retainFailure(terminalFailure, workers.failure());
        if (!workers.completed()) {
            Printer.log.warn("SBK workers did not stop within {} second(s); "
                    + "closing driver resources to unblock pending operations",
                    RUNTIME_CONFIG.workerTerminationSeconds);
            terminalFailure = closeReaders(terminalFailure);
            terminalFailure = closeWriters(terminalFailure);
            workersClosed = true;
            terminalFailure = closeStorage(terminalFailure);
            storageClosed = true;
            workers = awaitWorkers();
            terminalFailure = retainFailure(terminalFailure, workers.failure());
            if (!workers.completed()) {
                terminalFailure = retainFailure(terminalFailure,
                        new IllegalStateException("SBK workers remained active after forced driver close"));
            }
        }
        stopPerformanceRecorders(requestedTermination);
        terminalFailure = retainFailure(terminalFailure, completedFutureFailure(writePerlCompletion));
        terminalFailure = retainFailure(terminalFailure, completedFutureFailure(readPerlCompletion));
        if (!workersClosed) {
            terminalFailure = closeReaders(terminalFailure);
            terminalFailure = closeWriters(terminalFailure);
        }
        if (!storageClosed) {
            terminalFailure = closeStorage(terminalFailure);
        }
        try {
            rwLogger.close(params);
        } catch (IOException e) {
            terminalFailure = retainFailure(terminalFailure, e);
        }
        final BenchmarkTermination termination = BenchmarkTermination.resolve(requestedTermination, terminalFailure);
        if (terminalFailure != null) {
            Printer.log.warn("SBK Benchmark Shutdown: {}", termination.describe(
                    params.getTotalSecondsToRun(), params.getTotalRecords(),
                    params.getIdleTimeoutSeconds(), terminalFailure), terminalFailure);
            retFuture.completeExceptionally(terminalFailure);
        } else {
            Printer.log.info("SBK Benchmark Shutdown: {}", termination.describe(
                    params.getTotalSecondsToRun(), params.getTotalRecords(),
                    params.getIdleTimeoutSeconds(), null));
            retFuture.complete(null);
        }
        timeoutExecutor.shutdownNow();
        lifecycleExecutor.shutdown();

    }

    private WorkerCompletion awaitWorkers() {
        if (workerCompletion == null) {
            return new WorkerCompletion(true, null);
        }
        try {
            workerCompletion.get(RUNTIME_CONFIG.workerTerminationSeconds, TimeUnit.SECONDS);
            return new WorkerCompletion(true, null);
        } catch (ExecutionException | CancellationException exception) {
            return new WorkerCompletion(true, unwrapCompletionFailure(exception));
        } catch (TimeoutException timeout) {
            return new WorkerCompletion(false, null);
        } catch (InterruptedException interrupted) {
            Thread.currentThread().interrupt();
            return new WorkerCompletion(false, interrupted);
        }
    }

    private record WorkerCompletion(boolean completed, Throwable failure) {
    }

    private Throwable closeReaders(Throwable terminalFailure) {
        for (DataReader<Object> reader : readers) {
            try {
                reader.close();
            } catch (IOException e) {
                terminalFailure = retainFailure(terminalFailure, e);
            }
        }
        return terminalFailure;
    }

    private Throwable closeWriters(Throwable terminalFailure) {
        for (DataWriter<Object> writer : writers) {
            try {
                writer.close();
            } catch (IOException e) {
                terminalFailure = retainFailure(terminalFailure, e);
            }
        }
        return terminalFailure;
    }

    private Throwable closeStorage(Throwable terminalFailure) {
        try {
            storage.closeStorage(params);
        } catch (IOException e) {
            terminalFailure = retainFailure(terminalFailure, e);
        }
        return terminalFailure;
    }

    private void stopPerformanceRecorders(BenchmarkTermination requestedTermination) {
        final BenchmarkTermination recorderTermination = requestedTermination.isSuccessfulCompletion()
                ? requestedTermination : BenchmarkTermination.STOP_REQUESTED;
        if (writePerl != null) {
            writePerl.stop(recorderTermination);
        }
        if (readPerl != null) {
            readPerl.stop(recorderTermination);
        }
    }

    private static Throwable completedFutureFailure(CompletableFuture<?> future) {
        if (future == null) {
            return null;
        }
        if (!future.isDone()) {
            return new IllegalStateException("PerL completion remained pending during SBK shutdown");
        }
        try {
            future.join();
            return null;
        } catch (CompletionException | CancellationException exception) {
            return unwrapCompletionFailure(exception);
        }
    }

    private static Throwable retainFailure(Throwable primary, Throwable additional) {
        primary = unwrapCompletionFailure(primary);
        additional = unwrapCompletionFailure(additional);
        if (primary == null) {
            return additional;
        }
        if (additional != null && additional != primary) {
            primary.addSuppressed(additional);
        }
        return primary;
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
     * Returns the fixed-count share assigned to one worker.
     *
     * <p>The first {@code totalRecords % workersCount} workers receive one additional
     * record, so every requested record is assigned exactly once, including when there
     * are fewer records than workers.
     *
     * @param totalRecords total records requested by the user
     * @param workersCount number of workers
     * @param workerIndex zero-based worker index
     * @return this worker's record count
     * @throws IllegalArgumentException if the worker count or index is invalid
     */
    static long recordsForWorker(long totalRecords, int workersCount, int workerIndex) {
        if (workersCount <= 0 || workerIndex < 0 || workerIndex >= workersCount) {
            throw new IllegalArgumentException("Invalid worker count or index");
        }
        long recordsPerWorker = totalRecords / workersCount;
        long remainder = totalRecords % workersCount;
        return recordsPerWorker + (workerIndex < remainder ? 1 : 0);
    }


    /**
     * Stop/shutdown SBK Benchmark.
     *
     * closes all writers/readers.
     * closes the storage device/client.
     */
    @Override
    public void stop() {
        requestShutdown(null, BenchmarkTermination.STOP_REQUESTED);
        try {
            retFuture.get(RUNTIME_CONFIG.forcedShutdownGraceSeconds, TimeUnit.SECONDS);
        } catch (ExecutionException ignored) {
            // The start future remains the authoritative carrier of benchmark failures.
        } catch (TimeoutException timeout) {
            forceShutdownCompletion(null);
        } catch (InterruptedException interrupted) {
            Thread.currentThread().interrupt();
            forceShutdownCompletion(interrupted);
        }
    }
}
