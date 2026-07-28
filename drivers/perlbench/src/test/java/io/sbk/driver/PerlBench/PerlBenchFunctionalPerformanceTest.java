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
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Locale;
import java.util.Properties;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Exercises the complete SBK worker, clock, queue, recorder, and logger path.
 *
 * <p>This deliberately opt-in performance test is excluded from the normal
 * {@code check} task. The mode tasks measure one queue implementation. The
 * comparison task warms both implementations and measures alternating
 * execution pairs in the same JVM.</p>
 */
public final class PerlBenchFunctionalPerformanceTest {
    private static final int RECORD_SIZE = 8;

    /**
     * Run warmup and measured exact-record workloads for one queue mode.
     *
     * @throws Exception if SBK execution or report creation fails
     */
    @Test
    @Tag("perlbench-functional-mode")
    public void measureFullSbkQueuePath() throws Exception {
        final boolean mpscQueue = Boolean.parseBoolean(
                requiredProperty("perlbench.mpscQueue"));
        final long records = positiveLong("perlbench.records");
        final long warmupRecords = positiveLong(
                "perlbench.warmupRecords");
        final int runs = positiveInt("perlbench.runs");
        final int writers = positiveInt("perlbench.writers");
        final Path report = Path.of(requiredProperty("perlbench.report"));
        final double[] throughputs = new double[runs];
        final double[] averageLatencies = new double[runs];

        runSbk(mpscQueue, writers, warmupRecords);
        for (int run = 0; run < runs; run++) {
            final PerlBenchPerformanceLogger.Result result =
                    validatedRun(mpscQueue, writers, records);
            throughputs[run] = result.recordsPerSecond();
            averageLatencies[run] = result.averageLatency();
        }

        final double medianThroughput = median(throughputs);
        final double medianLatency = median(averageLatencies);
        writeReport(report, mpscQueue, writers, records, runs,
                throughputs, medianThroughput, medianLatency);
        System.out.printf(
                "%s full-SBK PerlBench result: median %,.0f records/s, "
                        + "%.1f ns measured operation latency; "
                        + "%,d exact records/run x %d runs; "
                        + "zero invalid latencies%n",
                queueName(mpscQueue), medianThroughput, medianLatency,
                records, runs);
    }

    /**
     * Compare both queue modes as alternating pairs in the same warmed JVM.
     *
     * @throws Exception if SBK execution or report creation fails
     */
    @Test
    @Tag("perlbench-functional-comparison")
    public void compareFullSbkQueuePaths() throws Exception {
        final long records = positiveLong("perlbench.records");
        final long warmupRecords = positiveLong(
                "perlbench.warmupRecords");
        final int runs = positiveInt("perlbench.runs");
        final int writers = positiveInt("perlbench.writers");
        final Path mpscReport = Path.of(
                requiredProperty("perlbench.mpscReport"));
        final Path jdkClqReport = Path.of(
                requiredProperty("perlbench.jdkClqReport"));
        final Path comparisonReport = Path.of(
                requiredProperty("perlbench.comparisonReport"));
        final double[] mpscThroughputs = new double[runs];
        final double[] jdkClqThroughputs = new double[runs];
        final double[] mpscLatencies = new double[runs];
        final double[] jdkClqLatencies = new double[runs];
        final double[] pairedGains = new double[runs];

        runSbk(false, writers, warmupRecords);
        runSbk(true, writers, warmupRecords);
        for (int run = 0; run < runs; run++) {
            final PerlBenchPerformanceLogger.Result mpsc;
            final PerlBenchPerformanceLogger.Result jdkClq;
            if ((run & 1) == 0) {
                mpsc = validatedRun(true, writers, records);
                jdkClq = validatedRun(false, writers, records);
            } else {
                jdkClq = validatedRun(false, writers, records);
                mpsc = validatedRun(true, writers, records);
            }
            mpscThroughputs[run] = mpsc.recordsPerSecond();
            jdkClqThroughputs[run] = jdkClq.recordsPerSecond();
            mpscLatencies[run] = mpsc.averageLatency();
            jdkClqLatencies[run] = jdkClq.averageLatency();
            pairedGains[run] =
                    (mpsc.recordsPerSecond()
                            - jdkClq.recordsPerSecond()) * 100.0
                            / jdkClq.recordsPerSecond();
        }

        writeReport(mpscReport, true, writers, records, runs,
                mpscThroughputs, median(mpscThroughputs),
                median(mpscLatencies));
        writeReport(jdkClqReport, false, writers, records, runs,
                jdkClqThroughputs, median(jdkClqThroughputs),
                median(jdkClqLatencies));
        writeComparisonReport(comparisonReport, pairedGains);
    }

    private static PerlBenchPerformanceLogger.Result validatedRun(
            boolean mpscQueue, int writers, long records) throws Exception {
        final PerlBenchPerformanceLogger.Result result =
                runSbk(mpscQueue, writers, records);
        assertEquals(records, result.records(),
                "SBK must drain every timestamp in exact-record mode");
        assertEquals(0, result.invalidLatencies(),
                "the functional queue path produced invalid latencies");
        assertTrue(result.recordsPerSecond() > 0.0,
                "SBK must report positive functional throughput");
        return result;
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
        assertNotNull(result, "SBK did not publish a final PerlBench result");
        return result;
    }

    private static void writeReport(
            Path report, boolean mpscQueue, int writers, long records,
            int runs, double[] throughputs, double medianThroughput,
            double medianLatency) throws IOException {
        final Properties values = new Properties();
        values.setProperty("queue", queueName(mpscQueue));
        values.setProperty("mpscQueue", Boolean.toString(mpscQueue));
        values.setProperty("writers", Integer.toString(writers));
        values.setProperty("recordsPerRun", Long.toString(records));
        values.setProperty("measuredRuns", Integer.toString(runs));
        values.setProperty("medianRecordsPerSecond",
                Double.toString(medianThroughput));
        values.setProperty("medianAverageLatencyNs",
                Double.toString(medianLatency));
        values.setProperty("recordsPerSecondSamples",
                formatSamples(throughputs));
        Files.createDirectories(report.getParent());
        try (OutputStream output = Files.newOutputStream(report)) {
            values.store(output,
                    "SBK PerlBench functional queue performance");
        }
    }

    private static void writeComparisonReport(
            Path report, double[] pairedGains) throws IOException {
        final Properties values = new Properties();
        values.setProperty("medianPairedThroughputGainPercent",
                Double.toString(median(pairedGains)));
        values.setProperty("pairedGainSamples",
                formatPercentSamples(pairedGains));
        Files.createDirectories(report.getParent());
        try (OutputStream output = Files.newOutputStream(report)) {
            values.store(output,
                    "SBK PerlBench paired queue comparison");
        }
    }

    private static String formatSamples(double[] samples) {
        final StringBuilder formatted = new StringBuilder();
        for (int index = 0; index < samples.length; index++) {
            if (index > 0) {
                formatted.append(", ");
            }
            formatted.append(Math.round(samples[index]));
        }
        return formatted.toString();
    }

    private static String formatPercentSamples(double[] samples) {
        final StringBuilder formatted = new StringBuilder();
        for (int index = 0; index < samples.length; index++) {
            if (index > 0) {
                formatted.append(", ");
            }
            formatted.append(String.format(
                    Locale.ROOT, "%.2f%%", samples[index]));
        }
        return formatted.toString();
    }

    private static double median(double[] samples) {
        final double[] sorted = samples.clone();
        Arrays.sort(sorted);
        final int middle = sorted.length / 2;
        return sorted.length % 2 == 0
                ? (sorted[middle - 1] + sorted[middle]) / 2.0
                : sorted[middle];
    }

    private static String queueName(boolean mpscQueue) {
        return mpscQueue
                ? "TimeStampMpscQueue"
                : "JDK ConcurrentLinkedQueue";
    }

    private static int positiveInt(String property) {
        final int value = Integer.parseInt(requiredProperty(property));
        if (value <= 0) {
            throw new IllegalArgumentException(
                    property + " must be greater than zero");
        }
        return value;
    }

    private static long positiveLong(String property) {
        final long value = Long.parseLong(requiredProperty(property));
        if (value <= 0) {
            throw new IllegalArgumentException(
                    property + " must be greater than zero");
        }
        return value;
    }

    private static String requiredProperty(String property) {
        final String value = System.getProperty(property);
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(
                    "Missing system property: " + property);
        }
        return value;
    }
}
