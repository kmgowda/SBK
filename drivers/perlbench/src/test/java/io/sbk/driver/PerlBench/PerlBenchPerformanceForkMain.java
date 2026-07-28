/**
 * Copyright (c) KMG. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 */
package io.sbk.driver.PerlBench;

import io.sbk.api.impl.Sbk;

import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

/**
 * Executes one warmed PerlBench measurement in an isolated JVM.
 *
 * <p>The parent functional test launches this class once per sample. JVM
 * isolation prevents compilation profiles, heap occupancy, and garbage
 * collection state from one queue implementation from carrying into the
 * other implementation's sample.</p>
 */
public final class PerlBenchPerformanceForkMain {
    private static final int RECORD_SIZE = 8;

    private PerlBenchPerformanceForkMain() {
    }

    /**
     * Run one warmup and one exact-record measurement.
     *
     * @param args queue mode, writers, warmup records, measured records,
     *             and output properties file
     * @throws IllegalArgumentException if the required arguments are missing
     *                                  or malformed
     * @throws IllegalStateException if SBK produces an incomplete or invalid
     *                               result
     * @throws Exception if SBK execution or report creation fails
     */
    public static void main(String[] args) throws Exception {
        if (args.length != 5) {
            throw new IllegalArgumentException(
                    "Expected: <mpscQueue> <writers> <warmupRecords> "
                            + "<records> <report>");
        }
        final boolean mpscQueue = Boolean.parseBoolean(args[0]);
        final int writers = Integer.parseInt(args[1]);
        final long warmupRecords = Long.parseLong(args[2]);
        final long records = Long.parseLong(args[3]);
        final Path report = Path.of(args[4]);

        runSbk(mpscQueue, writers, warmupRecords);
        final PerlBenchPerformanceLogger.Result result =
                runSbk(mpscQueue, writers, records);
        if (result.records() != records) {
            throw new IllegalStateException("Expected " + records
                    + " records but received " + result.records());
        }
        if (result.invalidLatencies() != 0) {
            throw new IllegalStateException("Observed "
                    + result.invalidLatencies() + " invalid latencies");
        }

        final Properties values = new Properties();
        values.setProperty("records", Long.toString(result.records()));
        values.setProperty("recordsPerSecond",
                Double.toString(result.recordsPerSecond()));
        values.setProperty("averageLatencyNs",
                Double.toString(result.averageLatency()));
        values.setProperty("invalidLatencies",
                Long.toString(result.invalidLatencies()));
        Files.createDirectories(report.getParent());
        try (OutputStream output = Files.newOutputStream(report)) {
            values.store(output, "One isolated PerlBench JVM fork");
        }
    }

    private static PerlBenchPerformanceLogger.Result runSbk(
            boolean mpscQueue, int writers, long records) throws Exception {
        PerlBenchPerformanceLogger.reset();
        Sbk.run(new String[]{
                "-class", "perlbench",
                "-writers", Integer.toString(writers),
                "-size", Integer.toString(RECORD_SIZE),
                "-records", Long.toString(records),
                "-time", "ns",
                "-thread", "p",
                "-mpscqueue", Boolean.toString(mpscQueue),
                "-out", "PerlBenchPerformanceLogger"
        }, "sbk-perlbench-performance", "io.sbk.driver.PerlBench",
                "io.sbk.driver.PerlBench");
        final PerlBenchPerformanceLogger.Result result =
                PerlBenchPerformanceLogger.getResult();
        if (result == null) {
            throw new IllegalStateException(
                    "SBK did not publish a final PerlBench result");
        }
        return result;
    }
}
