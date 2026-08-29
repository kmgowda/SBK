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
import io.perl.api.impl.HashMapLatencyRecorder;
import io.perl.api.impl.HybridPagedLatencyRecorder;
import io.perl.api.impl.LongHashMapLatencyRecorder;
import io.time.NanoSeconds;
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
import org.eclipse.collections.api.iterator.MutableLongIterator;
import org.eclipse.collections.impl.map.mutable.primitive.LongLongHashMap;

import java.util.Arrays;
import java.util.concurrent.TimeUnit;

/**
 * Compares dense-array, primitive-map, and boxed-map latency recording.
 *
 * <p>The workload cycles over 4,096 exact latency values outside the JVM's
 * small boxed-{@link Long} cache. The GC profiler therefore exposes allocation
 * caused by boxing keys and updated counts in the reference implementation,
 * while throughput measures the complete frequency-update path.</p>
 */
@Fork(value = 3)
@Warmup(iterations = 3, time = 1, timeUnit = TimeUnit.SECONDS)
@Measurement(iterations = 5, time = 1, timeUnit = TimeUnit.SECONDS)
@Timeout(time = 30, timeUnit = TimeUnit.SECONDS)
public class LatencyMapBenchmark {
    private static final long BASE_LATENCY = 1_000;
    private static final long LATENCY_MASK = 4_095;
    private static final double[] PERCENTILES =
            new double[]{0.5, 0.9, 0.99};

    /**
     * Thread-private recorder state.
     */
    @State(Scope.Thread)
    public static class RecorderState {
        private HashMapLatencyRecorder boxed;
        private LongHashMapLatencyRecorder primitive;
        private ArrayLatencyRecorder array;
        private long boxedSequence;
        private long primitiveSequence;
        private long arraySequence;

        /**
         * Creates empty recorders before each measurement iteration.
         */
        @Setup(Level.Iteration)
        public void setUp() {
            boxed = new HashMapLatencyRecorder(0, Long.MAX_VALUE,
                    Long.MAX_VALUE, Long.MAX_VALUE, Long.MAX_VALUE,
                    PERCENTILES, new NanoSeconds(), 64);
            primitive = new LongHashMapLatencyRecorder(0, Long.MAX_VALUE,
                    Long.MAX_VALUE, Long.MAX_VALUE, Long.MAX_VALUE,
                    PERCENTILES, new NanoSeconds(), 64);
            array = new ArrayLatencyRecorder(0,
                    BASE_LATENCY + LATENCY_MASK,
                    Long.MAX_VALUE, Long.MAX_VALUE, Long.MAX_VALUE,
                    PERCENTILES, new NanoSeconds());
            boxedSequence = 0;
            primitiveSequence = 0;
            arraySequence = 0;
        }
    }

    /**
     * Thread-private state comparing transient sorted-list extraction with a
     * reusable primitive sorting array.
     */
    @State(Scope.Thread)
    public static class ExtractionState {
        /**
         * Number of distinct latency keys copied and sorted per extraction.
         */
        @Param({"65536"})
        public int distinctLatencies;

        private LongLongHashMap allocatedMap;
        private LongLongHashMap reusableMap;
        private long[] reusableBuffer;

        /**
         * Populates identical primitive maps and allocates the reusable buffer.
         */
        @Setup(Level.Trial)
        public void setUp() {
            allocatedMap = new LongLongHashMap(distinctLatencies);
            reusableMap = new LongLongHashMap(distinctLatencies);
            reusableBuffer = new long[distinctLatencies];
            for (int index = 0; index < distinctLatencies; index++) {
                final long latency = BASE_LATENCY + index;
                allocatedMap.put(latency, index + 1L);
                reusableMap.put(latency, index + 1L);
            }
        }
    }

    /**
     * Thread-private state for a complete record-and-extract window.
     */
    @State(Scope.Thread)
    public static class WindowState {
        /**
         * Number of distinct exact latency values in each window.
         */
        @Param({"4096"})
        public int distinctLatencies;

        private ArrayLatencyRecorder array;
        private HashMapLatencyRecorder boxed;
        private HybridPagedLatencyRecorder hybridPaged;
        private LongHashMapLatencyRecorder primitive;
        private LatencyPercentiles arrayPercentiles;
        private LatencyPercentiles boxedPercentiles;
        private LatencyPercentiles hybridPagedPercentiles;
        private LatencyPercentiles primitivePercentiles;

        /**
         * Creates reusable recorders and percentile result holders.
         */
        @Setup(Level.Trial)
        public void setUp() {
            final long highLatency =
                    BASE_LATENCY + distinctLatencies - 1L;
            array = new ArrayLatencyRecorder(0, highLatency,
                    Long.MAX_VALUE, Long.MAX_VALUE, Long.MAX_VALUE,
                    PERCENTILES, new NanoSeconds());
            boxed = new HashMapLatencyRecorder(0, highLatency,
                    Long.MAX_VALUE, Long.MAX_VALUE, Long.MAX_VALUE,
                    PERCENTILES, new NanoSeconds(), 64);
            primitive = new LongHashMapLatencyRecorder(0, highLatency,
                    Long.MAX_VALUE, Long.MAX_VALUE, Long.MAX_VALUE,
                    PERCENTILES, new NanoSeconds(), 64);
            hybridPaged = new HybridPagedLatencyRecorder(0, highLatency,
                    Long.MAX_VALUE, Long.MAX_VALUE, Long.MAX_VALUE,
                    PERCENTILES, new NanoSeconds(), 64, 8, 32);
            arrayPercentiles = new LatencyPercentiles(PERCENTILES);
            boxedPercentiles = new LatencyPercentiles(PERCENTILES);
            hybridPagedPercentiles = new LatencyPercentiles(PERCENTILES);
            primitivePercentiles = new LatencyPercentiles(PERCENTILES);
        }
    }

    /** Thread-private state for exact values distributed one per page. */
    @State(Scope.Thread)
    public static class SparseWindowState {
        /** Number of distinct sparse latency values in each window. */
        @Param({"4096"})
        public int distinctLatencies;

        private HybridPagedLatencyRecorder hybridPaged;
        private LongHashMapLatencyRecorder primitive;
        private LatencyPercentiles hybridPagedPercentiles;
        private LatencyPercentiles primitivePercentiles;

        /** Creates exact sparse recorders and percentile holders. */
        @Setup(Level.Trial)
        public void setUp() {
            final long highLatency = BASE_LATENCY
                    + ((long) distinctLatencies << 8);
            hybridPaged = new HybridPagedLatencyRecorder(0, highLatency,
                    Long.MAX_VALUE, Long.MAX_VALUE, Long.MAX_VALUE,
                    PERCENTILES, new NanoSeconds(), 64, 8, 32);
            primitive = new LongHashMapLatencyRecorder(0, highLatency,
                    Long.MAX_VALUE, Long.MAX_VALUE, Long.MAX_VALUE,
                    PERCENTILES, new NanoSeconds(), 64);
            hybridPagedPercentiles = new LatencyPercentiles(PERCENTILES);
            primitivePercentiles = new LatencyPercentiles(PERCENTILES);
        }
    }

    /**
     * Measures the primitive frequency-update path.
     *
     * @param state thread-private recorder state
     */
    @Benchmark
    @BenchmarkMode(Mode.Throughput)
    @OutputTimeUnit(TimeUnit.SECONDS)
    public void primitiveRecordLatency(RecorderState state) {
        final long latency = BASE_LATENCY
                + (state.primitiveSequence++ & LATENCY_MASK);
        state.primitive.reportLatency(latency, 1);
    }

    /**
     * Measures the boxed frequency-update reference path.
     *
     * @param state thread-private recorder state
     */
    @Benchmark
    @BenchmarkMode(Mode.Throughput)
    @OutputTimeUnit(TimeUnit.SECONDS)
    public void boxedRecordLatency(RecorderState state) {
        final long latency = BASE_LATENCY
                + (state.boxedSequence++ & LATENCY_MASK);
        state.boxed.reportLatency(latency, 1);
    }

    /**
     * Measures the dense-array frequency-update path.
     *
     * @param state thread-private recorder state
     */
    @Benchmark
    @BenchmarkMode(Mode.Throughput)
    @OutputTimeUnit(TimeUnit.SECONDS)
    public void arrayRecordLatency(RecorderState state) {
        final long latency = BASE_LATENCY
                + (state.arraySequence++ & LATENCY_MASK);
        state.array.reportLatency(latency, 1);
    }

    /**
     * Measures one complete dense-array window: record every distinct value,
     * calculate percentiles, and clear the used counters.
     *
     * @param state thread-private window state
     */
    @Benchmark
    @BenchmarkMode(Mode.AverageTime)
    @OutputTimeUnit(TimeUnit.MICROSECONDS)
    public void arrayWindow(WindowState state) {
        for (int index = 0; index < state.distinctLatencies; index++) {
            state.array.reportLatency(BASE_LATENCY + index, 1);
        }
        state.array.copyPercentiles(state.arrayPercentiles, null);
    }

    /**
     * Measures one complete boxed-map window.
     *
     * @param state thread-private window state
     */
    @Benchmark
    @BenchmarkMode(Mode.AverageTime)
    @OutputTimeUnit(TimeUnit.MICROSECONDS)
    public void boxedWindow(WindowState state) {
        for (int index = 0; index < state.distinctLatencies; index++) {
            state.boxed.reportLatency(BASE_LATENCY + index, 1);
        }
        state.boxed.copyPercentiles(state.boxedPercentiles, null);
    }

    /**
     * Measures one complete primitive-map window.
     *
     * @param state thread-private window state
     */
    @Benchmark
    @BenchmarkMode(Mode.AverageTime)
    @OutputTimeUnit(TimeUnit.MICROSECONDS)
    public void primitiveWindow(WindowState state) {
        for (int index = 0; index < state.distinctLatencies; index++) {
            state.primitive.reportLatency(BASE_LATENCY + index, 1);
        }
        state.primitive.copyPercentiles(
                state.primitivePercentiles, null);
    }

    /**
     * Measures one complete exact hybrid-page window.
     *
     * @param state thread-private window state
     */
    @Benchmark
    @BenchmarkMode(Mode.AverageTime)
    @OutputTimeUnit(TimeUnit.MICROSECONDS)
    public void hybridPagedWindow(WindowState state) {
        for (int index = 0; index < state.distinctLatencies; index++) {
            state.hybridPaged.reportLatency(BASE_LATENCY + index, 1);
        }
        state.hybridPaged.copyPercentiles(
                state.hybridPagedPercentiles, null);
    }

    /**
     * Measures one primitive-map window with one exact value per hybrid page.
     *
     * @param state thread-private sparse-window state
     */
    @Benchmark
    @BenchmarkMode(Mode.AverageTime)
    @OutputTimeUnit(TimeUnit.MICROSECONDS)
    public void primitiveSparseWindow(SparseWindowState state) {
        for (int index = 0; index < state.distinctLatencies; index++) {
            state.primitive.reportLatency(BASE_LATENCY + ((long) index << 8), 1);
        }
        state.primitive.copyPercentiles(state.primitivePercentiles, null);
    }

    /**
     * Measures one hybrid-page window with one exact value per page.
     *
     * @param state thread-private sparse-window state
     */
    @Benchmark
    @BenchmarkMode(Mode.AverageTime)
    @OutputTimeUnit(TimeUnit.MICROSECONDS)
    public void hybridPagedSparseWindow(SparseWindowState state) {
        for (int index = 0; index < state.distinctLatencies; index++) {
            state.hybridPaged.reportLatency(BASE_LATENCY + ((long) index << 8), 1);
        }
        state.hybridPaged.copyPercentiles(
                state.hybridPagedPercentiles, null);
    }

    /**
     * Measures the former extraction mechanism that allocates a sorted list.
     *
     * @param state thread-private extraction state
     * @return checksum that consumes all sorted keys and counts
     */
    @Benchmark
    @BenchmarkMode(Mode.AverageTime)
    @OutputTimeUnit(TimeUnit.MICROSECONDS)
    public long allocatedSortedListExtraction(ExtractionState state) {
        final MutableLongIterator keys =
                state.allocatedMap.keySet().toSortedList().longIterator();
        long checksum = 0;
        while (keys.hasNext()) {
            final long latency = keys.next();
            checksum += latency ^ state.allocatedMap.get(latency);
        }
        return checksum;
    }

    /**
     * Measures extraction through a retained primitive sorting array.
     *
     * @param state thread-private extraction state
     * @return checksum that consumes all sorted keys and counts
     */
    @Benchmark
    @BenchmarkMode(Mode.AverageTime)
    @OutputTimeUnit(TimeUnit.MICROSECONDS)
    public long reusableArrayExtraction(ExtractionState state) {
        final int size = state.reusableMap.size();
        state.reusableBuffer =
                state.reusableMap.keySet().toArray(state.reusableBuffer);
        Arrays.sort(state.reusableBuffer, 0, size);
        long checksum = 0;
        for (int index = 0; index < size; index++) {
            final long latency = state.reusableBuffer[index];
            checksum += latency ^ state.reusableMap.get(latency);
        }
        return checksum;
    }
}
