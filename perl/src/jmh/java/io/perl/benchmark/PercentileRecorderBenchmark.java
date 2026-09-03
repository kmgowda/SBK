/**
 * Copyright (c) KMG. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 */
package io.perl.benchmark;

import io.perl.api.LatencyPercentiles;
import io.perl.api.impl.ArrayLatencyRecorder;
import io.perl.api.impl.LongHashMapLatencyRecorder;
import io.perl.config.LatencyConfig;
import io.time.NanoSeconds;
import org.HdrHistogram.Histogram;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Level;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Param;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.Timeout;
import org.openjdk.jmh.annotations.Warmup;

import java.util.concurrent.TimeUnit;

/**
 * Compares PerL's exact percentile recorders with a three-significant-digit
 * HdrHistogram over the same deterministic nanosecond observations.
 *
 * <p>The frequency-update benchmarks isolate the storage update. The window
 * benchmarks include validation and summary accounting, recording all values,
 * percentile extraction, and clearing for reuse. They therefore represent the
 * complete production lifecycle rather than percentile lookup alone.</p>
 */
@Fork(value = 3)
@Warmup(iterations = 3, time = 1, timeUnit = TimeUnit.SECONDS)
@Measurement(iterations = 5, time = 1, timeUnit = TimeUnit.SECONDS)
@Timeout(time = 30, timeUnit = TimeUnit.SECONDS)
public class PercentileRecorderBenchmark {
    private static final long BASE_LATENCY_NS = 1_000_000L;
    private static final long MAX_TRACKABLE_LATENCY_NS = 180_000_000_000L;
    private static final int HDR_SIGNIFICANT_DIGITS =
            LatencyConfig.HDR_SIGNIFICANT_DIGITS;
    private static final double[] PERCENTILE_FRACTIONS =
            new double[]{0.5, 0.9, 0.99, 0.999, 0.9999};
    private static final double[] PERCENTILE_PERCENTAGES =
            new double[]{50.0, 90.0, 99.0, 99.9, 99.99};

    /** Thread-private state for isolated frequency updates. */
    @State(Scope.Thread)
    public static class UpdateState {
        /** Number of distinct latency values repeatedly recorded. */
        @Param({"4096"})
        public int distinctLatencies;

        private ArrayLatencyRecorder array;
        private LongHashMapLatencyRecorder primitive;
        private Histogram histogram;
        private long arraySequence;
        private long primitiveSequence;
        private long histogramSequence;

        /** Creates empty recorders before each measurement iteration. */
        @Setup(Level.Iteration)
        public void setUp() {
            final long highLatency = BASE_LATENCY_NS
                    + distinctLatencies - 1L;
            array = new ArrayLatencyRecorder(BASE_LATENCY_NS, highLatency,
                    Long.MAX_VALUE, Long.MAX_VALUE, Long.MAX_VALUE,
                    PERCENTILE_FRACTIONS, new NanoSeconds());
            primitive = new LongHashMapLatencyRecorder(BASE_LATENCY_NS,
                    highLatency, Long.MAX_VALUE, Long.MAX_VALUE,
                    Long.MAX_VALUE, PERCENTILE_FRACTIONS, new NanoSeconds(),
                    64);
            histogram = new Histogram(MAX_TRACKABLE_LATENCY_NS,
                    HDR_SIGNIFICANT_DIGITS);
            arraySequence = 0;
            primitiveSequence = 0;
            histogramSequence = 0;
        }
    }

    /** Thread-private state for complete record-and-percentile windows. */
    @State(Scope.Thread)
    public static class WindowState {
        /** Number of observations recorded in each complete window. */
        @Param({"65536"})
        public int observations;

        /** Number of distinct latency values represented by the observations. */
        @Param({"4096"})
        public int distinctLatencies;

        private ArrayLatencyRecorder array;
        private LongHashMapLatencyRecorder primitive;
        private Histogram histogram;
        private LatencyPercentiles arrayPercentiles;
        private LatencyPercentiles primitivePercentiles;
        private long[] latencies;

        /**
         * Creates reusable recorders and the shared deterministic dataset.
         *
         * @throws IllegalArgumentException when the distinct-value count is
         *                                  not a power of two
         */
        @Setup(Level.Trial)
        public void setUp() {
            if (Integer.bitCount(distinctLatencies) != 1) {
                throw new IllegalArgumentException(
                        "distinctLatencies must be a power of two");
            }
            final long highLatency = BASE_LATENCY_NS
                    + distinctLatencies - 1L;
            array = new ArrayLatencyRecorder(BASE_LATENCY_NS, highLatency,
                    Long.MAX_VALUE, Long.MAX_VALUE, Long.MAX_VALUE,
                    PERCENTILE_FRACTIONS, new NanoSeconds());
            primitive = new LongHashMapLatencyRecorder(BASE_LATENCY_NS,
                    highLatency, Long.MAX_VALUE, Long.MAX_VALUE,
                    Long.MAX_VALUE, PERCENTILE_FRACTIONS, new NanoSeconds(),
                    64);
            histogram = new Histogram(MAX_TRACKABLE_LATENCY_NS,
                    HDR_SIGNIFICANT_DIGITS);
            arrayPercentiles = new LatencyPercentiles(
                    PERCENTILE_FRACTIONS);
            primitivePercentiles = new LatencyPercentiles(
                    PERCENTILE_FRACTIONS);
            latencies = new long[observations];
            final int mask = distinctLatencies - 1;
            for (int index = 0; index < observations; index++) {
                // Multiplication by an odd number permutes every power-of-two
                // range before repeating and avoids an already sorted input.
                latencies[index] = BASE_LATENCY_NS
                        + ((index * 2_653) & mask);
            }
        }
    }

    /**
     * Measures one exact dense-array frequency update.
     *
     * @param state thread-private update state
     */
    @Benchmark
    @BenchmarkMode(Mode.Throughput)
    @OutputTimeUnit(TimeUnit.SECONDS)
    public void arrayFrequencyUpdate(UpdateState state) {
        final long latency = BASE_LATENCY_NS
                + state.arraySequence++ % state.distinctLatencies;
        state.array.reportLatency(latency, 1);
    }

    /**
     * Measures one exact primitive-map frequency update.
     *
     * @param state thread-private update state
     */
    @Benchmark
    @BenchmarkMode(Mode.Throughput)
    @OutputTimeUnit(TimeUnit.SECONDS)
    public void primitiveFrequencyUpdate(UpdateState state) {
        final long latency = BASE_LATENCY_NS
                + state.primitiveSequence++ % state.distinctLatencies;
        state.primitive.reportLatency(latency, 1);
    }

    /**
     * Measures one three-digit HdrHistogram frequency update.
     *
     * @param state thread-private update state
     */
    @Benchmark
    @BenchmarkMode(Mode.Throughput)
    @OutputTimeUnit(TimeUnit.SECONDS)
    public void hdrFrequencyUpdate(UpdateState state) {
        final long latency = BASE_LATENCY_NS
                + state.histogramSequence++ % state.distinctLatencies;
        state.histogram.recordValue(latency);
    }

    /**
     * Measures an exact array window including percentile extraction.
     *
     * @param state thread-private complete-window state
     * @return checksum consuming the calculated percentile values
     */
    @Benchmark
    @BenchmarkMode(Mode.AverageTime)
    @OutputTimeUnit(TimeUnit.MICROSECONDS)
    public long arrayPercentileWindow(WindowState state) {
        for (long latency : state.latencies) {
            state.array.recordLatency(0, 1, 0, latency);
        }
        state.array.copyPercentiles(state.arrayPercentiles, null);
        final long result = checksum(state.arrayPercentiles.latencies);
        state.array.reset(0);
        return result;
    }

    /**
     * Measures an exact primitive-map window including percentile extraction.
     *
     * @param state thread-private complete-window state
     * @return checksum consuming the calculated percentile values
     */
    @Benchmark
    @BenchmarkMode(Mode.AverageTime)
    @OutputTimeUnit(TimeUnit.MICROSECONDS)
    public long primitivePercentileWindow(WindowState state) {
        for (long latency : state.latencies) {
            state.primitive.recordLatency(0, 1, 0, latency);
        }
        state.primitive.copyPercentiles(state.primitivePercentiles, null);
        final long result = checksum(state.primitivePercentiles.latencies);
        state.primitive.reset(0);
        return result;
    }

    /**
     * Measures a three-digit HdrHistogram window and percentile extraction.
     *
     * @param state thread-private complete-window state
     * @return checksum consuming the calculated percentile values
     */
    @Benchmark
    @BenchmarkMode(Mode.AverageTime)
    @OutputTimeUnit(TimeUnit.MICROSECONDS)
    public long hdrPercentileWindow(WindowState state) {
        for (long latency : state.latencies) {
            state.histogram.recordValue(latency);
        }
        long result = 0;
        for (double percentile : PERCENTILE_PERCENTAGES) {
            result = 31 * result
                    + state.histogram.getValueAtPercentile(percentile);
        }
        state.histogram.reset();
        return result;
    }

    private static long checksum(long[] values) {
        long result = 0;
        for (long value : values) {
            result = 31 * result + value;
        }
        return result;
    }
}
