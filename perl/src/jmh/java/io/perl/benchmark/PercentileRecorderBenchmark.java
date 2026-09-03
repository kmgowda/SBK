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
import org.openjdk.jmh.annotations.TearDown;
import org.openjdk.jmh.annotations.Timeout;
import org.openjdk.jmh.annotations.Warmup;

import java.util.Random;
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
    private static final int UPDATE_SAMPLE_COUNT = 65_536;
    private static final int UPDATE_SAMPLE_MASK = UPDATE_SAMPLE_COUNT - 1;
    private static final long DATA_SEED = 0x5B6C_7D8E_9F01_2345L;
    private static final double LOGNORMAL_RANGE_DIVISOR = 64.0;
    private static final double LOGNORMAL_SIGMA = 1.5;
    private static final int HDR_SIGNIFICANT_DIGITS =
            LatencyConfig.HDR_SIGNIFICANT_DIGITS;
    private static final double[] PERCENTILE_FRACTIONS =
            new double[]{0.5, 0.9, 0.99, 0.999, 0.9999};
    private static final double[] PERCENTILE_PERCENTAGES =
            new double[]{50.0, 90.0, 99.0, 99.9, 99.99};

    /** Deterministic input shapes used by every recorder. */
    public enum Distribution {
        /** Ordered dense values retained as a prefetch-friendly baseline. */
        SEQUENTIAL,
        /** Lognormal values clustered near the mode with a sparse long tail. */
        CLUSTERED_LOGNORMAL
    }

    /** Thread-private state for isolated frequency updates. */
    @State(Scope.Thread)
    public static class UpdateState {
        /** Number of addressable latency slots in the recorder range. */
        @Param({"4096", "262144", "4194304"})
        public int distinctLatencies;

        /** Shape of the deterministic latency input stream. */
        @Param({"SEQUENTIAL", "CLUSTERED_LOGNORMAL"})
        public Distribution distribution;

        private ArrayLatencyRecorder array;
        private LongHashMapLatencyRecorder primitive;
        private Histogram histogram;
        private LatencyPercentiles arrayPercentiles;
        private LatencyPercentiles primitivePercentiles;
        private final Random inputRandom = new Random(DATA_SEED);
        private long[] latencies;
        private int arrayIndex;
        private int primitiveIndex;
        private int histogramIndex;

        /** Creates reusable recorders and the shared input before each trial. */
        @Setup(Level.Trial)
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
            arrayPercentiles = new LatencyPercentiles(PERCENTILE_FRACTIONS);
            primitivePercentiles = new LatencyPercentiles(
                    PERCENTILE_FRACTIONS);
            latencies = createLatencies(UPDATE_SAMPLE_COUNT,
                    distinctLatencies, distribution, inputRandom);
        }

        /** Restarts each recorder at the beginning of the shared input. */
        @Setup(Level.Iteration)
        public void restartInput() {
            arrayIndex = 0;
            primitiveIndex = 0;
            histogramIndex = 0;
        }

        /** Clears accumulated frequencies outside the measured update loop. */
        @TearDown(Level.Iteration)
        public void clearRecorders() {
            array.copyPercentiles(arrayPercentiles, null);
            array.reset(0);
            primitive.copyPercentiles(primitivePercentiles, null);
            primitive.reset(0);
            histogram.reset();
        }
    }

    /** Thread-private state for complete record-and-percentile windows. */
    @State(Scope.Thread)
    public static class WindowState {
        /** Number of observations recorded in each complete window. */
        @Param({"65536"})
        public int observations;

        /** Number of addressable latency slots in the recorder range. */
        @Param({"4096", "262144", "4194304"})
        public int distinctLatencies;

        /** Shape of the deterministic latency input stream. */
        @Param({"SEQUENTIAL", "CLUSTERED_LOGNORMAL"})
        public Distribution distribution;

        private ArrayLatencyRecorder array;
        private LongHashMapLatencyRecorder primitive;
        private Histogram histogram;
        private LatencyPercentiles arrayPercentiles;
        private LatencyPercentiles primitivePercentiles;
        private final Random inputRandom = new Random(DATA_SEED);
        private long[] latencies;

        /**
         * Creates reusable recorders and the shared deterministic dataset.
         *
         */
        @Setup(Level.Trial)
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
            arrayPercentiles = new LatencyPercentiles(
                    PERCENTILE_FRACTIONS);
            primitivePercentiles = new LatencyPercentiles(
                    PERCENTILE_FRACTIONS);
            latencies = createLatencies(observations, distinctLatencies,
                    distribution, inputRandom);
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
        final long latency = state.latencies[
                state.arrayIndex++ & UPDATE_SAMPLE_MASK];
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
        final long latency = state.latencies[
                state.primitiveIndex++ & UPDATE_SAMPLE_MASK];
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
        final long latency = state.latencies[
                state.histogramIndex++ & UPDATE_SAMPLE_MASK];
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

    private static long[] createLatencies(int observations,
                                          int distinctLatencies,
                                          Distribution distribution,
                                          Random random) {
        if (distinctLatencies <= 0) {
            throw new IllegalArgumentException(
                    "distinctLatencies must be positive");
        }
        final long[] values = new long[observations];
        if (distribution == Distribution.SEQUENTIAL) {
            for (int index = 0; index < observations; index++) {
                values[index] = BASE_LATENCY_NS
                        + index % distinctLatencies;
            }
            return values;
        }

        final double median = Math.max(1.0,
                distinctLatencies / LOGNORMAL_RANGE_DIVISOR);
        final double location = Math.log(median);
        for (int index = 0; index < observations; index++) {
            final double sample = Math.exp(location
                    + LOGNORMAL_SIGMA * random.nextGaussian());
            final long offset = Math.min(distinctLatencies - 1L,
                    Math.max(0L, Math.round(sample) - 1L));
            values[index] = BASE_LATENCY_NS + offset;
        }
        return values;
    }
}
