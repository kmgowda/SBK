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


import io.grpc.Server;
import io.grpc.ServerBuilder;
import io.perl.api.BenchmarkTermination;
import io.perl.data.Bytes;
import io.perl.config.LatencyConfig;
import io.perl.exception.BenchmarkIdleTimeoutException;
import io.perl.api.LatencyRecordWindow;
import io.perl.api.impl.CSVExtendedLatencyRecorder;
import io.perl.api.impl.HybridPagedLatencyRecorder;
import io.perl.api.impl.HybridPagedLatencyRecorder.MemoryLimitPolicy;
import io.perl.api.impl.LongHashMapLatencyRecorder;
import io.perl.api.impl.HdrExtendedLatencyRecorder;
import io.perl.api.impl.PerlBuilder;
import io.sbk.api.Benchmark;
import io.sbk.config.Config;
import io.sbk.config.SbkRuntimeConfig;
import io.sbm.config.SbmConfig;
import io.sbm.logger.RamLogger;
import io.sbm.api.SbmPeriodicRecorder;
import io.sbm.params.RamParameterOptions;
import io.sbk.system.Printer;
import io.sbp.grpc.ClientFailure;
import io.state.State;
import io.time.Time;
import lombok.Synchronized;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import javax.annotation.concurrent.GuardedBy;
import java.io.IOException;
import java.util.List;
import java.util.Random;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;


/**
 * Server-side benchmark that exposes a gRPC endpoint and aggregates latency metrics.
 *
 * <p>Responsibilities:
 * - Build latency windows and periodic/total recorders based on {@link SbmConfig} and logger settings.
 * - Start the gRPC {@link Server} and accept client registrations and records via {@link SbmGrpcService}.
 * - Periodically print results and on shutdown emit totals.
 */
final public class SbmBenchmark implements Benchmark {
    private static final int CSV_RANDOM_BOUND = 1_000_000;
    private static final String CSV_RANDOM_FORMAT = "%06d";
    final SbmConfig sbmConfig;
    final private Time time;
    final private RamLogger logger;
    final private RamParameterOptions params;
    final private SbmPeriodicRecorder latencyRecorder;
    final private Server server;
    final private SbmGrpcService service;
    final private SbmLatencyBenchmark benchmark;
    final private boolean coordinatedStart;
    final private double[] percentileFractions;
    final private CompletableFuture<Void> retFuture;
    final private ScheduledExecutorService deadlineExecutor;


    @GuardedBy("this")
    private State state;
    @GuardedBy("this")
    private boolean serverStarted;

    @GuardedBy("this")
    private CompletableFuture<Void> latencyCompletion;

    @GuardedBy("this")
    private long completionSeconds;

    @GuardedBy("this")
    private long completionRecords;

    /**
     * Create SBK Server Benchmark.
     *
     * @param sbmConfig Configuration parameters
     * @param params    Benchmarking input Parameters
     * @param logger    output logger
     * @param time      time interface
     * @throws IOException If Exception occurs.
     */
    public SbmBenchmark(SbmConfig sbmConfig, RamParameterOptions params,
                        @NotNull RamLogger logger, Time time) throws IOException {
        this(sbmConfig, params, logger, time, false);
    }

    /**
     * Create an SBM benchmark with optional coordinated client release.
     *
     * @param sbmConfig Configuration parameters
     * @param params Benchmarking input parameters
     * @param logger output logger
     * @param time time interface
     * @param coordinatedStart when true, release client registrations together after all expected clients arrive
     * @throws IOException if initialization fails
     */
    public SbmBenchmark(SbmConfig sbmConfig, RamParameterOptions params,
                        @NotNull RamLogger logger, Time time, boolean coordinatedStart) throws IOException {
        this.sbmConfig = sbmConfig;
        this.params = params;
        this.logger = logger;
        this.time = time;
        this.coordinatedStart = coordinatedStart;
        final double[] percentiles = logger.getPercentiles();
        percentileFractions = new double[percentiles.length];

        for (int i = 0; i < percentiles.length; i++) {
            percentileFractions[i] = percentiles[i] / LatencyConfig.PERCENTAGE_SCALE;
        }

        latencyRecorder = createLatencyRecorder();
        benchmark = new SbmLatencyBenchmark(sbmConfig.maxQueues, params.getIdleSleepMilliSeconds(), time, latencyRecorder,
                logger.getPrintingIntervalSeconds() * Time.MS_PER_SEC, params.getIdleTimeoutSeconds(),
                params.isFixedRecordMode());
        final int maxRecordSizeBytes = Math.multiplyExact(sbmConfig.maxRecordSizeMB, Bytes.BYTES_PER_MB);
        service = new SbmGrpcService(params, time, logger.getMinLatency(), logger.getMaxLatency(), logger, benchmark,
                coordinatedStart, maxRecordSizeBytes);
        server = ServerBuilder.forPort(params.getPort()).maxInboundMessageSize(maxRecordSizeBytes)
                .addService(service).directExecutor().build();
        retFuture = new CompletableFuture<>();
        deadlineExecutor = Executors.newSingleThreadScheduledExecutor(Thread.ofPlatform()
                .name("sbm-benchmark-deadline").daemon(true).factory());
        state = State.BEGIN;
        serverStarted = false;
        latencyCompletion = null;
        completionSeconds = 0;
        completionRecords = 0;
    }

    @Contract(" -> new")
    /**
     * Create the periodic and total latency recorders used during the benchmark run.
     *
     * @return recorder that tracks periodic windows and a backing total window.
     */
    private @NotNull SbmPeriodicRecorder createLatencyRecorder() {
        final boolean exactNanosecondPages = isExactNanoseconds(time);
        final LatencyRecordWindow window = createPeriodicLatencyWindow(sbmConfig, time,
                logger.getMinLatency(), logger.getMaxLatency(), percentileFractions);
        final LatencyRecordWindow totalWindow = createTotalLatencyWindow(sbmConfig, time,
                logger.getMinLatency(), logger.getMaxLatency(), percentileFractions);
        final LatencyRecordWindow totalWindowExtension;
        final Random random = new Random();

        if (exactNanosecondPages) {
            Printer.log.info("Window Latency Store: ExactHybridPages, Retained Cache Target: {} MB; "
                            + "page values: {}; "
                            + "sparse entries/page: {}", window.getMaxMemoryBytes() / Bytes.BYTES_PER_MB,
                    1 << sbmConfig.exactLatencyPageBits, sbmConfig.exactLatencySparsePageEntries);
            Printer.log.info("Total Window Latency Store: ExactHybridPages, Memory Reset Target: {} MB; "
                            + "page values: {}; "
                            + "sparse entries/page: {}", totalWindow.getMaxMemoryBytes() / Bytes.BYTES_PER_MB,
                    1 << sbmConfig.exactLatencyPageBits, sbmConfig.exactLatencySparsePageEntries);
        } else {
            Printer.log.info("Total Window Latency Store: PrimitiveLongMap, Size: "
                    + totalWindow.getMaxMemoryBytes() / Bytes.BYTES_PER_MB + " MB");
        }

        if (sbmConfig.histogram) {
            totalWindowExtension = new HdrExtendedLatencyRecorder(logger.getMinLatency(), logger.getMaxLatency(),
                    LatencyConfig.TOTAL_LATENCY_MAX, LatencyConfig.LONG_MAX, LatencyConfig.LONG_MAX,
                    percentileFractions, time, totalWindow);
            Printer.log.info(String.format("Total Window Extension: HdrHistogram, Size: %.2f MB",
                    (totalWindowExtension.getMaxMemoryBytes() * 1.0) / Bytes.BYTES_PER_MB));
        } else if (sbmConfig.csv) {
            totalWindowExtension = new CSVExtendedLatencyRecorder(logger.getMinLatency(), logger.getMaxLatency(),
                    LatencyConfig.TOTAL_LATENCY_MAX, LatencyConfig.LONG_MAX, LatencyConfig.LONG_MAX,
                    percentileFractions, time, totalWindow, sbmConfig.csvFileSizeGB,
                    Config.NAME + "-" + String.format(CSV_RANDOM_FORMAT, random.nextInt(CSV_RANDOM_BOUND)) + ".csv");
            Printer.log.info("Total Window Extension: CSV, Size: " +
                    totalWindowExtension.getMaxMemoryBytes() / Bytes.BYTES_PER_GB + " GB");
        } else {
            totalWindowExtension = totalWindow;
            Printer.log.info("Total Window Extension: None, Size: 0 MB");
        }

        return new SbmTotalWindowLatencyPeriodicRecorder(window, totalWindowExtension, logger, logger::printTotal,
                logger, logger, logger, logger, params.getMaxConnections());
    }

    static LatencyRecordWindow createPeriodicLatencyWindow(SbmConfig config, Time selectedTime,
                                                           long minimumLatency, long maximumLatency,
                                                           double[] selectedPercentiles) {
        if (isExactNanoseconds(selectedTime)) {
            return createHybridPagedRecorder(config, selectedTime, minimumLatency, maximumLatency,
                    selectedPercentiles, config.exactLatencyMaxMemoryMB,
                    MemoryLimitPolicy.RELEASE_AFTER_WINDOW);
        }
        return PerlBuilder.buildLatencyRecordWindow(config, selectedTime, minimumLatency, maximumLatency,
                selectedPercentiles);
    }

    static LatencyRecordWindow createTotalLatencyWindow(SbmConfig config, Time selectedTime,
                                                        long minimumLatency, long maximumLatency,
                                                        double[] selectedPercentiles) {
        if (isExactNanoseconds(selectedTime)) {
            return createHybridPagedRecorder(config, selectedTime, minimumLatency, maximumLatency,
                    selectedPercentiles, config.exactTotalLatencyMaxMemoryMB,
                    MemoryLimitPolicy.RESET_WINDOW_WHEN_FULL);
        }
        return new LongHashMapLatencyRecorder(minimumLatency, maximumLatency,
                LatencyConfig.TOTAL_LATENCY_MAX, LatencyConfig.LONG_MAX, LatencyConfig.LONG_MAX,
                selectedPercentiles, selectedTime, config.totalMaxHashMapSizeMB);
    }

    private static LatencyRecordWindow createHybridPagedRecorder(SbmConfig config, Time selectedTime,
                                                                 long minimumLatency, long maximumLatency,
                                                                 double[] selectedPercentiles,
                                                                 int maximumMemorySizeMB,
                                                                 MemoryLimitPolicy memoryLimitPolicy) {
        return new HybridPagedLatencyRecorder(minimumLatency, maximumLatency,
                LatencyConfig.TOTAL_LATENCY_MAX, LatencyConfig.LONG_MAX, LatencyConfig.LONG_MAX,
                selectedPercentiles, selectedTime, maximumMemorySizeMB, config.exactLatencyPageBits,
                config.exactLatencySparsePageEntries, memoryLimitPolicy);
    }

    private static boolean isExactNanoseconds(Time selectedTime) {
        return selectedTime.getTimeUnit() == io.time.TimeUnit.ns;
    }

    /**
     * Start SBM server benchmark and gRPC service.
     *
     * <p>Initializes logger and latency benchmark, then starts the gRPC server. Returns a
     * future that completes when the benchmark is shutdown.
     *
     * @return future that completes on shutdown
     * @throws IOException              if the gRPC server cannot be started
     * @throws InterruptedException     if interrupted while starting
     * @throws ExecutionException       if async initialization fails
     * @throws IllegalStateException    if called in an invalid state
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
        logger.open(params, params.getStorageName(), params.getAction(), time);
        if (!coordinatedStart) {
            startLatencyAggregation();
        }
        if (state == State.RUN) {
            try {
                server.start();
                serverStarted = true;
                SbmListenerDetails.localDetails(server.getPort()).forEach(detail ->
                        Printer.log.info("SBM gRPC Performance Data Endpoint ({}): {}",
                                detail.label(), detail.endpoint()));
            } catch (IOException exception) {
                shutdown(exception, BenchmarkTermination.INTERNAL_FAILURE);
                throw exception;
            }
        }
        return retFuture.toCompletableFuture();
    }

    /**
     * Start latency aggregation after an orchestrated client-registration barrier.
     *
     * <p>Standalone SBM starts aggregation from {@link #start()}. SBK-GEM uses this
     * method only after every prepared remote SBK client has registered, so deployment
     * and remote storage preparation are excluded from the benchmark reporting clock.
     *
     * @throws ExecutionException if latency aggregation fails during startup
     * @throws InterruptedException if interrupted while inspecting startup completion
     * @throws IllegalStateException if SBM is not running
     */
    @Synchronized
    public void startLatencyAggregation() throws ExecutionException, InterruptedException, IllegalStateException {
        if (state != State.RUN) {
            throw new IllegalStateException("SBM latency aggregation cannot start after shutdown");
        }
        if (latencyCompletion != null) {
            return;
        }
        final CompletableFuture<Void> latencyFuture = benchmark.start();
        latencyCompletion = latencyFuture;
        latencyFuture.whenComplete((ignored, failure) -> {
            if (failure != null) {
                final BenchmarkIdleTimeoutException idleTimeout = BenchmarkIdleTimeoutException.find(failure);
                if (idleTimeout != null) {
                    deadlineExecutor.schedule(() -> forceIdleCompletion(idleTimeout),
                            SbkRuntimeConfig.get().forcedShutdownGraceSeconds, TimeUnit.SECONDS);
                }
                shutdown(failure, BenchmarkTermination.INTERNAL_FAILURE);
            }
        });
        if (latencyFuture.isCompletedExceptionally()) {
            try {
                latencyFuture.get();
            } catch (ExecutionException exception) {
                shutdown(exception.getCause(), BenchmarkTermination.INTERNAL_FAILURE);
                throw exception;
            }
        }
    }

    /**
     * Shutdown SBM benchmark: stop gRPC server, stop latency benchmark, and close logger.
     *
     * @param failure terminal failure, or {@code null} for an orderly completion
     * @param requestedTermination lifecycle completion expected by the caller
     */
    @Synchronized
    private void shutdown(Throwable failure, BenchmarkTermination requestedTermination) {
        if (state != State.END) {
            state = State.END;
            Throwable lifecycleFailure = unwrapCompletionFailure(failure);
            if (serverStarted) {
                server.shutdown();
                serverStarted = false;
            }
            try {
                benchmark.stop();
            } catch (RuntimeException e) {
                lifecycleFailure = retainFailure(lifecycleFailure, e);
            }
            try {
                logger.close(params);
            } catch (IOException | RuntimeException e) {
                lifecycleFailure = retainFailure(lifecycleFailure, e);
            }
            lifecycleFailure = retainFailure(lifecycleFailure, completedFutureFailure(latencyCompletion));
            final Throwable terminalFailure = terminalFailure(lifecycleFailure, service.getClientFailures());
            final BenchmarkTermination termination = BenchmarkTermination.resolve(
                    requestedTermination, terminalFailure);
            if (terminalFailure == null) {
                Printer.log.info("SBM Shutdown: {}", termination.describe(
                        completionSeconds, completionRecords, params.getIdleTimeoutSeconds(), null));
                retFuture.complete(null);
            } else {
                final BenchmarkIdleTimeoutException idleTimeout = BenchmarkIdleTimeoutException.find(terminalFailure);
                if (idleTimeout != null) {
                    Printer.log.warn("SBM Shutdown: {}", termination.describe(
                            completionSeconds, completionRecords, params.getIdleTimeoutSeconds(), terminalFailure));
                } else {
                    Printer.log.error("SBM Shutdown: {}", termination.describe(
                            completionSeconds, completionRecords, params.getIdleTimeoutSeconds(), terminalFailure),
                            terminalFailure);
                }
                retFuture.completeExceptionally(terminalFailure);
            }
            deadlineExecutor.shutdownNow();
        }
    }

    private static Throwable completedFutureFailure(CompletableFuture<?> future) {
        if (future == null) {
            return null;
        }
        if (!future.isDone()) {
            return new IllegalStateException("SBM latency completion remained pending during shutdown");
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

    private void forceIdleCompletion(BenchmarkIdleTimeoutException idleTimeout) {
        if (retFuture.completeExceptionally(idleTimeout)) {
            Printer.log.warn("SBM idle-timeout cleanup exceeded {} seconds; forcing application exit",
                    SbkRuntimeConfig.get().forcedShutdownGraceSeconds);
        }
    }

    /**
     * Combines local aggregation failure with terminal failures reported by SBK clients.
     *
     * <p>This runs after latency ingestion has stopped and the aggregate has drained. A local
     * SBM failure remains authoritative. Otherwise, the first client report is primary and
     * subsequent reports are attached in report order.
     *
     * @param localFailure local SBM aggregation failure, or {@code null}
     * @param clientFailures terminal client reports in receipt order
     * @return combined terminal failure, or {@code null} when the run succeeded
     */
    static Throwable terminalFailure(Throwable localFailure, List<ClientFailure> clientFailures) {
        Throwable result = localFailure;
        for (ClientFailure report : clientFailures) {
            final IOException clientFailure = new IOException("SBM client " + report.getClientID()
                    + " (" + report.getComponent() + ") reported terminal failure: " + report.getMessage());
            if (result == null) {
                result = clientFailure;
            } else {
                result.addSuppressed(clientFailure);
            }
        }
        return result;
    }


    /**
     * Stop/shutdown SBK Server Benchmark.
     *
     * closes all writers/readers.
     * closes the storage device/client.
     */
    @Override
    public void stop() {
        shutdown(null, BenchmarkTermination.STOP_REQUESTED);
    }

    /**
     * Completes an orchestrated SBM run after every remote benchmark exits successfully.
     *
     * @param secondsToRun completed remote benchmark duration
     * @param recordsCount completed remote benchmark record target
     * @throws IllegalArgumentException when neither duration nor records identify successful completion
     */
    @Synchronized
    public void completeSuccessfully(long secondsToRun, long recordsCount) {
        final BenchmarkTermination termination = BenchmarkTermination.configured(secondsToRun, recordsCount);
        if (!termination.isSuccessfulCompletion()) {
            throw new IllegalArgumentException("SBM successful completion requires a duration or record reason");
        }
        completionSeconds = secondsToRun;
        completionRecords = recordsCount;
        shutdown(null, termination);
    }

    /**
     * Fail clients waiting for a coordinated distributed start.
     *
     * @param reason host-tagged remote execution failure
     * @return number of pending clients released with an error
     */
    public int abortPendingRegistrations(String reason) {
        return service.abortPendingRegistrations(reason);
    }

    /**
     * Return the maximum number of remote SBK clients registered with SBM.
     *
     * @return maximum registered client count
     */
    public int getMaximumRegisteredClients() {
        return service.getMaximumRegisteredClients();
    }

    /**
     * Return the coordinated-start failure reported by the controller.
     *
     * @return failure description, or {@code null} when no failure was reported
     */
    public String getRegistrationFailure() {
        return service.getRegistrationFailure();
    }

    /**
     * Wait for the distributed coordinated-start barrier.
     *
     * @param timeout maximum wait duration
     * @param unit timeout unit
     * @return true when all expected remote clients registered
     * @throws InterruptedException if the controller thread is interrupted
     */
    public boolean awaitCoordinatedStart(long timeout, TimeUnit unit) throws InterruptedException {
        return service.awaitCoordinatedStart(timeout, unit);
    }

    /**
     * Release prepared remote clients after latency aggregation has started.
     *
     * @return number of coordinated clients released
     * @throws IllegalStateException when the registration barrier is not ready
     */
    public int releaseCoordinatedStart() {
        return service.releaseCoordinatedStart();
    }
}
