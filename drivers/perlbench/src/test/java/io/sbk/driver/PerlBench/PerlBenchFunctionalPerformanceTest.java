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

import io.sbk.params.impl.SbkParameters;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.management.ManagementFactory;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Properties;
import java.util.SplittableRandom;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Exercises the complete SBK worker, clock, queue, recorder, and logger path.
 *
 * <p>Each measured sample runs in an independently warmed JVM. The comparison
 * randomizes queue order, validates the true shared MPSC topology, and reports
 * confidence intervals. Exact completion and latency validity are correctness
 * gates; environment-sensitive throughput is classified rather than used as
 * an unconditional build gate.</p>
 */
public final class PerlBenchFunctionalPerformanceTest {
    private static final Duration FORK_TIMEOUT = Duration.ofMinutes(5);
    private static final long ORDER_SEED = 0x53424b4d505343L;

    /**
     * Run independently warmed exact-record workloads for one queue mode.
     *
     * @throws Exception if SBK execution or report creation fails
     */
    @Test
    @Tag("perlbench-functional-mode")
    public void measureFullSbkQueuePath() throws Exception {
        verifySharedMpscTopology();
        final boolean mpscQueue = Boolean.parseBoolean(
                requiredProperty("perlbench.mpscQueue"));
        final long records = positiveLong("perlbench.records");
        final long warmupRecords = positiveLong(
                "perlbench.warmupRecords");
        final int runs = sampleCount("perlbench.runs");
        final int writers = positiveInt("perlbench.writers");
        final Path report = Path.of(requiredProperty("perlbench.report"));
        final double[] throughputs = new double[runs];
        final double[] averageLatencies = new double[runs];

        for (int run = 0; run < runs; run++) {
            final ForkResult result = runFork(
                    mpscQueue, writers, warmupRecords, records,
                    report.resolveSibling(report.getFileName() + ".fork-"
                            + run + ".properties"));
            throughputs[run] = result.recordsPerSecond();
            averageLatencies[run] = result.averageLatencyNs();
        }

        writeModeReport(report, mpscQueue, writers, records,
                throughputs, averageLatencies);
        final ThroughputSummary summary =
                ThroughputSummary.from(throughputs);
        System.out.printf(Locale.ROOT,
                "%s full-SBK PerlBench result: median %,.0f records/s, "
                        + "mean 95%% CI [%,.0f, %,.0f]; "
                        + "%,d exact records/fork x %d independent JVM forks; "
                        + "zero invalid latencies%n",
                queueName(mpscQueue), summary.median(), summary.lower(),
                summary.upper(), records, runs);
    }

    /**
     * Compare randomized independently warmed JVM forks for both queue modes.
     *
     * @throws Exception if SBK execution or report creation fails
     */
    @Test
    @Tag("perlbench-functional-comparison")
    public void compareFullSbkQueuePaths() throws Exception {
        verifySharedMpscTopology();
        final long records = positiveLong("perlbench.records");
        final long warmupRecords = positiveLong(
                "perlbench.warmupRecords");
        final int runs = sampleCount("perlbench.runs");
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
        final SplittableRandom order = new SplittableRandom(ORDER_SEED);

        for (int run = 0; run < runs; run++) {
            final boolean mpscFirst = order.nextBoolean();
            final ForkResult first = runFork(
                    mpscFirst, writers, warmupRecords, records,
                    forkReport(comparisonReport, run, mpscFirst));
            final ForkResult second = runFork(
                    !mpscFirst, writers, warmupRecords, records,
                    forkReport(comparisonReport, run, !mpscFirst));
            assignResult(mpscFirst, run, first,
                    mpscThroughputs, mpscLatencies,
                    jdkClqThroughputs, jdkClqLatencies);
            assignResult(!mpscFirst, run, second,
                    mpscThroughputs, mpscLatencies,
                    jdkClqThroughputs, jdkClqLatencies);
        }

        writeModeReport(mpscReport, true, writers, records,
                mpscThroughputs, mpscLatencies);
        writeModeReport(jdkClqReport, false, writers, records,
                jdkClqThroughputs, jdkClqLatencies);
        writeComparisonReport(comparisonReport,
                ThroughputSummary.from(mpscThroughputs),
                ThroughputSummary.from(jdkClqThroughputs));
    }

    private static Path forkReport(
            Path comparisonReport, int run, boolean mpscQueue) {
        return comparisonReport.resolveSibling(
                comparisonReport.getFileName() + ".fork-" + run + "-"
                        + (mpscQueue ? "mpsc" : "jdk-clq")
                        + ".properties");
    }

    private static void assignResult(
            boolean mpscQueue, int run, ForkResult result,
            double[] mpscThroughputs, double[] mpscLatencies,
            double[] jdkClqThroughputs, double[] jdkClqLatencies) {
        if (mpscQueue) {
            mpscThroughputs[run] = result.recordsPerSecond();
            mpscLatencies[run] = result.averageLatencyNs();
        } else {
            jdkClqThroughputs[run] = result.recordsPerSecond();
            jdkClqLatencies[run] = result.averageLatencyNs();
        }
    }

    private static ForkResult runFork(
            boolean mpscQueue, int writers, long warmupRecords,
            long records, Path report) throws Exception {
        Files.createDirectories(report.getParent());
        Files.deleteIfExists(report);
        final Path log = report.resolveSibling(report.getFileName() + ".log");
        Files.deleteIfExists(log);

        final List<String> command = new ArrayList<>();
        command.add(requiredProperty("perlbench.javaExecutable"));
        for (String argument : ManagementFactory.getRuntimeMXBean()
                .getInputArguments()) {
            if (argument.startsWith("-X")
                    || argument.startsWith("--enable-native-access")) {
                command.add(argument);
            }
        }
        command.add("-cp");
        command.add(requiredProperty("perlbench.testClasspath"));
        command.add(PerlBenchPerformanceForkMain.class.getName());
        command.add(Boolean.toString(mpscQueue));
        command.add(Integer.toString(writers));
        command.add(Long.toString(warmupRecords));
        command.add(Long.toString(records));
        command.add(report.toAbsolutePath().toString());

        final Process process = new ProcessBuilder(command)
                .redirectErrorStream(true)
                .redirectOutput(log.toFile())
                .start();
        final boolean exited = process.waitFor(
                FORK_TIMEOUT.toMillis(), TimeUnit.MILLISECONDS);
        if (!exited) {
            process.destroyForcibly();
            process.waitFor();
            throw new AssertionError(queueName(mpscQueue)
                    + " fork exceeded " + FORK_TIMEOUT);
        }
        if (process.exitValue() != 0) {
            throw new AssertionError(queueName(mpscQueue)
                    + " fork exited with " + process.exitValue() + ":\n"
                    + Files.readString(log));
        }

        final Properties values = loadProperties(report);
        assertEquals(records,
                Long.parseLong(values.getProperty("records")),
                "SBK must drain every timestamp in exact-record mode");
        assertEquals(0,
                Long.parseLong(values.getProperty("invalidLatencies")),
                "the functional queue path produced invalid latencies");
        final double throughput = Double.parseDouble(
                values.getProperty("recordsPerSecond"));
        assertTrue(throughput > 0.0,
                "SBK must report positive functional throughput");
        return new ForkResult(throughput, Double.parseDouble(
                values.getProperty("averageLatencyNs")));
    }

    private static void writeModeReport(
            Path report, boolean mpscQueue, int writers, long records,
            double[] throughputs, double[] averageLatencies)
            throws IOException {
        final ThroughputSummary summary =
                ThroughputSummary.from(throughputs);
        final Properties values = new Properties();
        values.setProperty("queue", queueName(mpscQueue));
        values.setProperty("mpscQueue", Boolean.toString(mpscQueue));
        values.setProperty("topology", "shared-mpsc");
        values.setProperty("maxQs", "1");
        values.setProperty("writers", Integer.toString(writers));
        values.setProperty("recordsPerRun", Long.toString(records));
        values.setProperty("measuredRuns",
                Integer.toString(throughputs.length));
        values.setProperty("medianRecordsPerSecond",
                Double.toString(summary.median()));
        values.setProperty("meanRecordsPerSecond",
                Double.toString(summary.mean()));
        values.setProperty("mean95ConfidenceLower",
                Double.toString(summary.lower()));
        values.setProperty("mean95ConfidenceUpper",
                Double.toString(summary.upper()));
        values.setProperty("medianAverageLatencyNs",
                Double.toString(median(averageLatencies)));
        values.setProperty("recordsPerSecondSamples",
                formatSamples(throughputs));
        storeProperties(report, values,
                "SBK PerlBench isolated-JVM functional performance");
    }

    private static void writeComparisonReport(
            Path report, ThroughputSummary mpsc,
            ThroughputSummary jdkClq) throws IOException {
        final Properties values = new Properties();
        final String verdict = classify(mpsc, jdkClq);
        values.setProperty("verdict", verdict);
        values.setProperty("classificationRule",
                "non-overlapping-95-percent-mean-confidence-intervals");
        values.setProperty("mpscMedianRecordsPerSecond",
                Double.toString(mpsc.median()));
        values.setProperty("mpscMean95ConfidenceLower",
                Double.toString(mpsc.lower()));
        values.setProperty("mpscMean95ConfidenceUpper",
                Double.toString(mpsc.upper()));
        values.setProperty("jdkClqMedianRecordsPerSecond",
                Double.toString(jdkClq.median()));
        values.setProperty("jdkClqMean95ConfidenceLower",
                Double.toString(jdkClq.lower()));
        values.setProperty("jdkClqMean95ConfidenceUpper",
                Double.toString(jdkClq.upper()));
        values.setProperty("medianThroughputDifferencePercent",
                Double.toString((mpsc.median() - jdkClq.median())
                        * 100.0 / jdkClq.median()));
        storeProperties(report, values,
                "SBK PerlBench statistical queue comparison");
    }

    private static String classify(
            ThroughputSummary mpsc, ThroughputSummary jdkClq) {
        if (mpsc.lower() > jdkClq.upper()) {
            return "MPSC_FASTER";
        }
        if (jdkClq.lower() > mpsc.upper()) {
            return "JDK_CLQ_FASTER";
        }
        return "INCONCLUSIVE";
    }

    private static void verifySharedMpscTopology() throws IOException {
        assertEquals(1, SbkParameters.loadPerlConfig().maxQs,
                "PerlBench performance tests must use one shared queue");
    }

    private static Properties loadProperties(Path report)
            throws IOException {
        final Properties values = new Properties();
        try (InputStream input = Files.newInputStream(report)) {
            values.load(input);
        }
        return values;
    }

    private static void storeProperties(
            Path report, Properties values, String comment)
            throws IOException {
        Files.createDirectories(report.getParent());
        try (OutputStream output = Files.newOutputStream(report)) {
            values.store(output, comment);
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

    private static double tCritical95(int degreesOfFreedom) {
        final double[] values = {
                0.0, 12.706, 4.303, 3.182, 2.776, 2.571, 2.447,
                2.365, 2.306, 2.262, 2.228, 2.201, 2.179, 2.160,
                2.145, 2.131, 2.120, 2.110, 2.101, 2.093, 2.086,
                2.080, 2.074, 2.069, 2.064, 2.060, 2.056, 2.052,
                2.048, 2.045, 2.042
        };
        return degreesOfFreedom < values.length
                ? values[degreesOfFreedom] : 1.960;
    }

    private static int positiveInt(String property) {
        final int value = Integer.parseInt(requiredProperty(property));
        if (value <= 0) {
            throw new IllegalArgumentException(
                    property + " must be greater than zero");
        }
        return value;
    }

    private static int sampleCount(String property) {
        final int value = positiveInt(property);
        if (value < 2) {
            throw new IllegalArgumentException(
                    property + " must be at least two");
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

    private record ForkResult(
            double recordsPerSecond, double averageLatencyNs) {
    }

    private record ThroughputSummary(
            double mean, double median, double lower, double upper) {
        private static ThroughputSummary from(double[] samples) {
            final double mean = Arrays.stream(samples).average()
                    .orElseThrow();
            double sumSquared = 0.0;
            for (double sample : samples) {
                final double difference = sample - mean;
                sumSquared += difference * difference;
            }
            final double standardDeviation = Math.sqrt(
                    sumSquared / (samples.length - 1));
            final double margin = tCritical95(samples.length - 1)
                    * standardDeviation / Math.sqrt(samples.length);
            return new ThroughputSummary(mean,
                    PerlBenchFunctionalPerformanceTest.median(samples),
                    Math.max(0.0, mean - margin), mean + margin);
        }
    }
}
