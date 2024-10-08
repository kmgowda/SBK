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
import io.perl.data.Bytes;
import io.perl.config.LatencyConfig;
import io.perl.api.LatencyRecordWindow;
import io.perl.api.impl.CSVExtendedLatencyRecorder;
import io.perl.api.impl.HashMapLatencyRecorder;
import io.perl.api.impl.HdrExtendedLatencyRecorder;
import io.perl.api.impl.PerlBuilder;
import io.sbk.api.Benchmark;
import io.sbk.config.Config;
import io.sbm.config.SbmConfig;
import io.sbm.logger.RamLogger;
import io.sbm.api.SbmPeriodicRecorder;
import io.sbm.params.RamParameterOptions;
import io.sbk.system.Printer;
import io.state.State;
import io.time.Time;
import lombok.Synchronized;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import javax.annotation.concurrent.GuardedBy;
import java.io.IOException;
import java.util.Random;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;


/**
 * Class for performing the benchmark.
 */
final public class SbmBenchmark implements Benchmark {
    final SbmConfig sbmConfig;
    final private Time time;
    final private RamLogger logger;
    final private RamParameterOptions params;
    final private SbmPeriodicRecorder latencyRecorder;
    final private Server server;
    final private SbmGrpcService service;
    final private SbmLatencyBenchmark benchmark;
    final private double[] percentileFractions;
    final private CompletableFuture<Void> retFuture;


    @GuardedBy("this")
    private State state;

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
        this.sbmConfig = sbmConfig;
        this.params = params;
        this.logger = logger;
        this.time = time;
        final double[] percentiles = logger.getPercentiles();
        percentileFractions = new double[percentiles.length];

        for (int i = 0; i < percentiles.length; i++) {
            percentileFractions[i] = percentiles[i] / 100.0;
        }

        latencyRecorder = createLatencyRecorder();
        benchmark = new SbmLatencyBenchmark(sbmConfig.maxQueues, params.getIdleSleepMilliSeconds(), time, latencyRecorder,
                logger.getPrintingIntervalSeconds() * Time.MS_PER_SEC);
        service = new SbmGrpcService(params, time, logger.getMinLatency(), logger.getMaxLatency(), logger, benchmark);
        server = ServerBuilder.forPort(params.getPort()).addService(service).directExecutor().build();
        retFuture = new CompletableFuture<>();
        state = State.BEGIN;
    }

    @Contract(" -> new")
    private @NotNull SbmPeriodicRecorder createLatencyRecorder() {
        final LatencyRecordWindow window = PerlBuilder.buildLatencyRecordWindow(sbmConfig, time,
                logger.getMinLatency(), logger.getMaxLatency(), percentileFractions);
        final LatencyRecordWindow totalWindow;
        final LatencyRecordWindow totalWindowExtension;
        final Random random = new Random();

        totalWindow = new HashMapLatencyRecorder(logger.getMinLatency(), logger.getMaxLatency(),
                LatencyConfig.TOTAL_LATENCY_MAX, LatencyConfig.LONG_MAX, LatencyConfig.LONG_MAX, percentileFractions,
                time, sbmConfig.totalMaxHashMapSizeMB);
        Printer.log.info("Total Window Latency Store: HashMap, Size: " +
                totalWindow.getMaxMemoryBytes() / Bytes.BYTES_PER_MB + " MB");

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
                    Config.NAME + "-" + String.format("%06d", random.nextInt(1000000)) + ".csv");
            Printer.log.info("Total Window Extension: CSV, Size: " +
                    totalWindowExtension.getMaxMemoryBytes() / Bytes.BYTES_PER_GB + " GB");
        } else {
            totalWindowExtension = totalWindow;
            Printer.log.info("Total Window Extension: None, Size: 0 MB");
        }

        return new SbmTotalWindowLatencyPeriodicRecorder(window, totalWindowExtension, logger, logger::printTotal,
                logger, logger, logger, logger);
    }

    /**
     * Start SBK Server Benchmark.
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
        Printer.log.info("SBM Started");
        logger.open(params, params.getStorageName(), params.getAction(), time);
        benchmark.start();
        server.start();
        return retFuture.toCompletableFuture();
    }

    /**
     * Shutdown SBK Benchmark.
     *
     * closes all writers/readers.
     * closes the storage device/client.
     */
    @Synchronized
    private void shutdown() {
        if (state != State.END) {
            state = State.END;
            try {
                server.shutdown();
                benchmark.stop();
                logger.close(params);
            } catch (IOException e) {
                e.printStackTrace();
            }
            Printer.log.info("SBM Shutdown");
            retFuture.complete(null);
        }
    }


    /**
     * Stop/shutdown SBK Server Benchmark.
     *
     * closes all writers/readers.
     * closes the storage device/client.
     */
    @Override
    public void stop() {
        shutdown();
    }
}
