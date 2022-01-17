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

import io.perl.LatencyRecordWindow;
import io.perl.PeriodicLogger;
import io.perl.Perl;
import io.perl.PerlConfig;
import io.perl.PerlPrinter;
import io.perl.Print;
import io.perl.ReportLatency;
import io.time.Time;
import java.util.Random;
import java.util.concurrent.ExecutorService;

public final class PerlBuilder {

    private static LatencyRecordWindow createLatencyWindow(PerlConfig config, Time time,
                                                    long minLatency, long maxLatency,
                                                    double[] percentileFractions) {
        final long latencyRange = maxLatency - minLatency;
        final long memSizeMB = (latencyRange * PerlConfig.LATENCY_VALUE_SIZE_BYTES) / PerlConfig.BYTES_PER_MB;
        final LatencyRecordWindow window;

        if (memSizeMB < config.maxArraySizeMB && latencyRange < Integer.MAX_VALUE) {
            window = new ArrayLatencyRecorder(minLatency, maxLatency,
                    PerlConfig.TOTAL_LATENCY_MAX, PerlConfig.LONG_MAX, PerlConfig.LONG_MAX, percentileFractions, time);
            PerlPrinter.log.info("Window Latency Store: Array, Size: " +
                    window.getMaxMemoryBytes() / PerlConfig.BYTES_PER_MB + " MB");
        } else {
            window = new HashMapLatencyRecorder(minLatency, maxLatency,
                    PerlConfig.TOTAL_LATENCY_MAX, PerlConfig.LONG_MAX, PerlConfig.LONG_MAX, percentileFractions,
                    time, config.maxHashMapSizeMB);
            PerlPrinter.log.info("Window Latency Store: HashMap, Size: " +
                    window.getMaxMemoryBytes() / PerlConfig.BYTES_PER_MB + " MB");
        }
        return window;
    }


    private static PeriodicLogger createLatencyRecorder(PerlConfig config, Time time,
                                                          long minLatency, long maxLatency,
                                                          double[] percentileFractions,
                                                          Print windowLogger, Print totalLogger,
                                                          ReportLatency reportLatency) {
        final LatencyRecordWindow window;
        final LatencyRecordWindow totalWindow;
        final LatencyRecordWindow totalWindowExtension;

        window = createLatencyWindow(config, time,
                minLatency, maxLatency, percentileFractions);

        totalWindow = new HashMapLatencyRecorder(minLatency, maxLatency,
                PerlConfig.TOTAL_LATENCY_MAX, PerlConfig.LONG_MAX, PerlConfig.LONG_MAX, percentileFractions,
                time, config.totalMaxHashMapSizeMB);
        PerlPrinter.log.info("Total Window Latency Store: HashMap, Size: " +
                totalWindow.getMaxMemoryBytes() / PerlConfig.BYTES_PER_MB + " MB");

        if (config.histogram) {
            totalWindowExtension = new HdrExtendedLatencyRecorder(minLatency, maxLatency,
                    PerlConfig.TOTAL_LATENCY_MAX, PerlConfig.LONG_MAX, PerlConfig.LONG_MAX,
                    percentileFractions, time, totalWindow);
            PerlPrinter.log.info(String.format("Total Window Extension: HdrHistogram, Size: %.2f MB",
                    (totalWindowExtension.getMaxMemoryBytes() * 1.0) / PerlConfig.BYTES_PER_MB));
        } else if (config.csv) {
            totalWindowExtension = new CSVExtendedLatencyRecorder(minLatency, maxLatency,
                    PerlConfig.TOTAL_LATENCY_MAX, PerlConfig.LONG_MAX, PerlConfig.LONG_MAX,
                    percentileFractions, time, totalWindow, config.csvFileSizeGB,
                    PerlConfig.NAME + "-" + String.format("%06d", new Random().nextInt(1000000)) + ".csv");
            PerlPrinter.log.info("Total Window Extension: CSV, Size: " +
                    totalWindowExtension.getMaxMemoryBytes() / PerlConfig.BYTES_PER_GB + " GB");
        } else {
            totalWindowExtension = totalWindow;
            PerlPrinter.log.info("Total Window Extension: None, Size: 0 MB");
        }

        return new TotalWindowLatencyPeriodicLogger(window, totalWindowExtension, windowLogger, totalLogger,
                reportLatency, time);
    }

    public static Perl build(int maxWorkers, int  reportingMS, int timeoutMS,  ExecutorService executor,
                      PerlConfig config, Time time, long minLatency, long maxLatency, double[] percentileFractions,
                      Print windowLogger, Print totalLogger,
                      ReportLatency reportLatency) {
        return new CQueuePerl(config, maxWorkers, createLatencyRecorder(config, time, minLatency, maxLatency,
                percentileFractions, windowLogger, totalLogger, reportLatency),
                reportingMS, timeoutMS,
               time, executor);
    }
}
