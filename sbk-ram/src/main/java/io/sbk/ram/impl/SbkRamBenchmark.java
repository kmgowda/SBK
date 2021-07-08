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


import io.grpc.Server;
import io.grpc.ServerBuilder;
import io.sbk.api.Benchmark;
import io.sbk.perl.State;
import io.sbk.ram.RamConfig;
import io.sbk.ram.RamLogger;
import io.sbk.ram.RamParameterOptions;
import io.sbk.grpc.LatenciesRecord;
import io.sbk.perl.PerlConfig;
import io.sbk.perl.Time;
import io.sbk.perl.impl.ArrayLatencyRecorder;
import io.sbk.perl.impl.HashMapLatencyRecorder;
import io.sbk.perl.LatencyRecordWindow;
import io.sbk.system.Printer;
import lombok.Synchronized;

import javax.annotation.concurrent.GuardedBy;
import java.io.IOException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.LinkedBlockingQueue;


/**
 * Class for performing the benchmark.
 */
public class SbkRamBenchmark implements Benchmark {
    final RamConfig ramConfig;
    final private Time time;
    final private RamLogger logger;
    final private RamParameterOptions params;
    final private LinkedBlockingQueue<LatenciesRecord> queue;
    final private LatencyRecordWindow window;
    final private Server server;
    final private SbkGrpcService service;
    final private RamBenchmark benchmark;
    final private double[] percentileFractions;
    final private CompletableFuture<Void> retFuture;


    @GuardedBy("this")
    private State state;

    /**
     * Create SBK Server Benchmark.
     *
     * @param  ramConfig         Configuration parameters
     * @param  params               Benchmarking input Parameters
     * @param  logger               output logger
     * @param  time                 time interface
     * @throws IOException          If Exception occurs.
     */
    public SbkRamBenchmark(RamConfig ramConfig, RamParameterOptions params,
                           RamLogger logger, Time time) throws IOException {
        this.ramConfig = ramConfig;
        this.params = params;
        this.logger = logger;
        this.time = time;
        final double[] percentiles = logger.getPercentiles();
        percentileFractions = new double[percentiles.length];

        for (int i = 0; i < percentiles.length; i++) {
            percentileFractions[i] = percentiles[i] / 100.0;
        }

        queue = new LinkedBlockingQueue<>();
        window = createLatencyWindow();
        benchmark = new RamBenchmark(ramConfig.maxQueues, ramConfig.idleMS, window, time,
                logger.getReportingIntervalSeconds() * PerlConfig.MS_PER_SEC,
                logger, logger, logger);
        service = new SbkGrpcService(params, time, logger.getMinLatency(), logger.getMaxLatency(), logger, benchmark);
        server = ServerBuilder.forPort(params.getRamPort()).directExecutor().addService(service).build();
        retFuture = new CompletableFuture<>();
        state = State.BEGIN;
    }


    private LatencyRecordWindow createLatencyWindow() {
        final long latencyRange = logger.getMaxLatency() - logger.getMinLatency();
        final long memSizeMB = (latencyRange * PerlConfig.LATENCY_VALUE_SIZE_BYTES) / PerlConfig.BYTES_PER_MB;
        final LatencyRecordWindow window;

        if (memSizeMB < ramConfig.maxArraySizeMB && latencyRange < Integer.MAX_VALUE) {
            window = new ArrayLatencyRecorder(logger.getMinLatency(), logger.getMaxLatency(),
                    PerlConfig.LONG_MAX, PerlConfig.LONG_MAX, PerlConfig.LONG_MAX, percentileFractions, time);
            Printer.log.info("Window Latency Store: Array");
        } else {
            window = new HashMapLatencyRecorder(logger.getMinLatency(), logger.getMaxLatency(),
                    PerlConfig.LONG_MAX, PerlConfig.LONG_MAX, PerlConfig.LONG_MAX, percentileFractions, time,
                    ramConfig.maxHashMapSizeMB);
            Printer.log.info("Window Latency Store: HashMap");

        }
        return window;
    }

    /**
     * Start SBK Server Benchmark.
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
        Printer.log.info("SBK RAM Benchmark Started");
        logger.open(params, params.getStorageName(), params.getAction(), time);
        benchmark.start();
        server.start();
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
            Printer.log.info("SBK RAM Benchmark Shutdown");
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
