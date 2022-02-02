/**
 * Copyright (c) KMG. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 */
package io.perl.impl;

import io.perl.Bytes;
import io.perl.LatencyConfig;
import io.perl.LatencyRecordWindow;
import io.perl.PerformanceLogger;
import io.perl.PeriodicLogger;
import io.perl.Perl;
import io.perl.PerlConfig;
import io.perl.PerlPrinter;
import io.perl.ReportLatency;
import io.time.MicroSeconds;
import io.time.MilliSeconds;
import io.time.NanoSeconds;
import io.time.Time;
import io.time.TimeUnit;
import org.jetbrains.annotations.NotNull;

import java.util.Random;
import java.util.concurrent.ExecutorService;

public final class PerlBuilder {

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

    public static LatencyRecordWindow buildLatencyRecordWindow(LatencyConfig config, Time time,
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


    private static PeriodicLogger buildPeriodicLogger(Time time,
                                                      LatencyConfig config,
                                                      PerformanceLogger logger,
                                                      ReportLatency reportLatency) {
        final long minLatency = logger.getMinLatency();
        final long maxLatency = logger.getMaxLatency();
        final double[] percentiles = logger.getPercentiles();
        final LatencyRecordWindow window;
        final LatencyRecordWindow totalWindow;
        final LatencyRecordWindow totalWindowExtension;

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
                    PerlConfig.NAME + "-" + String.format("%06d", new Random().nextInt(1000000)) + ".csv");
            PerlPrinter.log.info("Total Window Extension: CSV, Size: " +
                    totalWindowExtension.getMaxMemoryBytes() / Bytes.BYTES_PER_GB + " GB");
        } else {
            totalWindowExtension = totalWindow;
            PerlPrinter.log.info("Total Window Extension: None, Size: 0 MB");
        }

        return new TotalWindowLatencyPeriodicLogger(window, totalWindowExtension, logger, logger::printTotal,
                reportLatency, time);
    }

    public static Perl build(PerlConfig config, PerformanceLogger logger, ReportLatency reportLatency,
                             ExecutorService executor, Time time) throws IllegalArgumentException {
        if (time == null) {
            time = buildTime(logger);
        }
        if (time.getTimeUnit() != logger.getTimeUnit()) {
            throw new IllegalArgumentException("Time units are not matching");
        }
        return new CQueuePerl(config, buildPeriodicLogger(time, config, logger, reportLatency),
                logger.getPrintingIntervalSeconds() * Time.MS_PER_SEC, time, executor);
    }

}
