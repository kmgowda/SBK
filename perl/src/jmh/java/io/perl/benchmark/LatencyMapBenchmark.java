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

import io.perl.api.impl.HashMapLatencyRecorder;
import io.perl.api.impl.LongHashMapLatencyRecorder;
import io.time.NanoSeconds;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Level;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.Timeout;
import org.openjdk.jmh.annotations.Warmup;

import java.util.concurrent.TimeUnit;

/**
 * Compares sparse-latency recording through primitive and boxed maps.
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
        private long boxedSequence;
        private long primitiveSequence;

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
            boxedSequence = 0;
            primitiveSequence = 0;
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
}
