/**
 * Copyright (c) KMG. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 */
package io.perl.api.impl;

import io.perl.data.Bytes;
import io.perl.config.LatencyConfig;
import io.perl.api.LatencyRecordWindow;
import io.perl.logger.PerformanceLogger;
import io.perl.api.PeriodicRecorder;
import io.perl.api.Perl;
import io.perl.config.PerlConfig;
import io.perl.system.PerlPrinter;
import io.time.MicroSeconds;
import io.time.MilliSeconds;
import io.time.NanoSeconds;
import io.time.Time;
import io.time.TimeUnit;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ForkJoinPool;


/**
 * Helper class responsible for constructing configured PerL instances and
 * their supporting components (time implementations, latency windows, and
 * periodic recorders) based on supplied configuration and logger choices.
 */
public final class PerlBuilder {

    /**
     * Build 'Time' object based on Performance logger. The returned Time
     * implementation matches the logger's {@link TimeUnit}.
     *
     * @param logger Performance logger
     * @return Time object appropriate for the logger's time unit
     */
    public static @NotNull Time buildTime(@NotNull PerformanceLogger logger) {
        final TimeUnit timeUnit = logger.getTimeUnit();
        final Time ret = switch (timeUnit) {
            case mcs -> new MicroSeconds();
            case ns -> new NanoSeconds();
            default -> new MilliSeconds();
        };
        PerlPrinter.log.info("Time Unit: " + ret.getTimeUnit().toString());
        PerlPrinter.log.info("Minimum Latency: " + logger.getMinLatency() + " " + ret.getTimeUnit().name());
        PerlPrinter.log.info("Maximum Latency: " + logger.getMaxLatency() + " " + ret.getTimeUnit().name());
        return ret;
    }

    /**
     * Build Latency Record Window using configured strategy (array or hashmap)
     * depending on memory estimation and latency range.
     *
     * @param config              Latency configuration
     * @param time                Time instance used for conversions
     * @param minLatency          Minimum Latency value (inclusive)
     * @param maxLatency          Maximum Latency value (inclusive)
     * @param percentileFractions Array of percentile fractions (e.g. {0.5,0.9})
     * @return a configured LatencyRecordWindow instance
     */
    public static @NotNull LatencyRecordWindow buildLatencyRecordWindow(@NotNull LatencyConfig config, Time time,
                                                                        long minLatency, long maxLatency,
                                                                        double[] percentileFractions) {
        final long latencyRange = maxLatency - minLatency;
        final long memSizeMB = (latencyRange * LatencyConfig.LATENCY_VALUE_SIZE_BYTES) / Bytes.BYTES_PER_MB;
        final LatencyRecordWindow window;

        if (memSizeMB < config.maxArraySizeMB && latencyRange < Integer.MAX_VALUE) {
            window = new ArrayLatencyRecorder(minLatency, maxLatency,
                    LatencyConfig.TOTAL_LATENCY_MAX, LatencyConfig.LONG_MAX, LatencyConfig.LONG_MAX, percentileFractions, time);
            PerlPrinter.log.info("Window Latency Store: Array, Size: " +
                    window.getMaxMemoryBytes() / Bytes.BYTES_PER_MB + " MB");
        } else {
            window = new HashMapLatencyRecorder(minLatency, maxLatency,
                    LatencyConfig.TOTAL_LATENCY_MAX, LatencyConfig.LONG_MAX, LatencyConfig.LONG_MAX, percentileFractions,
                    time, config.maxHashMapSizeMB);
            PerlPrinter.log.info("Window Latency Store: HashMap, Size: " +
                    window.getMaxMemoryBytes() / Bytes.BYTES_PER_MB + " MB");
        }
        return window;
    }

    /**
     * Build Periodic Logger and total window, configuring optional extensions
     * such as HdrHistogram or CSV output based on the provided config.
     *
     * @param time   Time instance used for window timing
     * @param config Latency configuration driving window choices
     * @param logger Performance logger used to obtain percentile and thresholds
     * @return a new PeriodicRecorder instance
     */
    @Contract("_, _, _ -> new")
    private static @NotNull PeriodicRecorder buildPeriodicLogger(Time time,
                                                                 LatencyConfig config,
                                                                 @NotNull PerformanceLogger logger) {
        final long minLatency = logger.getMinLatency();
        final long maxLatency = logger.getMaxLatency();
        final double[] percentiles = logger.getPercentiles();
        final LatencyRecordWindow window;
        final LatencyRecordWindow totalWindow;
        final LatencyRecordWindow totalWindowExtension;
        final Random randomNum = new Random();

        final double[] percentileFractions = new double[percentiles.length];
        for (int i = 0; i < percentiles.length; i++) {
            percentileFractions[i] = percentiles[i] / 100.0;
        }

        window = buildLatencyRecordWindow(config, time, minLatency, maxLatency, percentileFractions);

        totalWindow = new HashMapLatencyRecorder(minLatency, maxLatency,
                LatencyConfig.TOTAL_LATENCY_MAX, LatencyConfig.LONG_MAX, LatencyConfig.LONG_MAX, percentileFractions,
                time, config.totalMaxHashMapSizeMB);
        PerlPrinter.log.info("Total Window Latency Store: HashMap, Size: " +
                totalWindow.getMaxMemoryBytes() / Bytes.BYTES_PER_MB + " MB");

        if (config.histogram) {
            totalWindowExtension = new HdrExtendedLatencyRecorder(minLatency, maxLatency,
                    LatencyConfig.TOTAL_LATENCY_MAX, LatencyConfig.LONG_MAX, LatencyConfig.LONG_MAX,
                    percentileFractions, time, totalWindow);
            PerlPrinter.log.info(String.format("Total Window Extension: HdrHistogram, Size: %.2f MB",
                    (totalWindowExtension.getMaxMemoryBytes() * 1.0) / Bytes.BYTES_PER_MB));
        } else if (config.csv) {
            totalWindowExtension = new CSVExtendedLatencyRecorder(minLatency, maxLatency,
                    LatencyConfig.TOTAL_LATENCY_MAX, LatencyConfig.LONG_MAX, LatencyConfig.LONG_MAX,
                    percentileFractions, time, totalWindow, config.csvFileSizeGB,
                    PerlConfig.NAME + "-" + String.format("%06d", randomNum.nextInt(1000000)) + ".csv");
            PerlPrinter.log.info("Total Window Extension: CSV, Size: " +
                    totalWindowExtension.getMaxMemoryBytes() / Bytes.BYTES_PER_GB + " GB");
        } else {
            totalWindowExtension = totalWindow;
            PerlPrinter.log.info("Total Window Extension: None, Size: 0 MB");
        }

        return new TotalWindowLatencyPeriodicRecorder(window, totalWindowExtension, logger, logger::printTotal,
                logger::recordLatency, time);
    }

    /**
     * Build CQ (Concurrent Queue) based Perl.
     *
     * @param logger   Performance Logger
     * @param time     time interface
     * @param config   Perl configuration
     * @param executor Executor Service
     * @return Perl Object
     * @throws IllegalArgumentException in case logger and latency reporter are missing and time unit of 'Time' and
     *                                  performance logger is not matching
     * @throws IOException              if the CQ perl creation failed.
     */
    @Contract("null, _, _, _ -> fail; !null, null, _ -> fail")
    public static @NotNull Perl build(PerformanceLogger logger, Time time,
                                      PerlConfig config, ExecutorService executor)
                                throws IllegalArgumentException, IOException {
        if (logger == null ) {
            throw new IllegalArgumentException("Performance logger and ReportLatency are missing");
        }
        if (config == null) {
            config = PerlConfig.build();
        }
        if (time == null) {
            time = buildTime(logger);
        }
        if (time.getTimeUnit() != logger.getTimeUnit()) {
            throw new IllegalArgumentException("Time units are not matching; logger time unit: " +
                    logger.getTimeUnit() + ", Time time unit: " + time.getTimeUnit());
        }
        if (executor == null) {
            // Use common pool to avoid creating a short-lived pool here.
            executor = ForkJoinPool.commonPool();
        }
        return new CQueuePerl(config, buildPeriodicLogger(time, config, logger),
                logger.getPrintingIntervalSeconds() * Time.MS_PER_SEC, time, executor);
    }

}
