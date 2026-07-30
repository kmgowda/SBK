/**
 * Copyright (c) KMG. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 */
package io.sbk.logger.impl;

import io.perl.data.Bytes;
import io.sbp.grpc.MessageLatenciesRecord;
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
 * Compares primitive latency accumulation and packed encoding with the legacy
 * protobuf map implementation.
 */
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
@Warmup(iterations = 3, time = 1)
@Measurement(iterations = 5, time = 1)
@Fork(2)
@Timeout(time = 30)
public class GrpcLatencyAccumulatorBenchmark {
    private static final int DISTINCT_LATENCIES = 4096;

    /**
     * Long-lived per-measurement accumulation state.
     */
    @State(Scope.Thread)
    public static class RecordState {
        private GrpcLatencyAccumulator primitive;
        private MessageLatenciesRecord.Builder protobufMap;
        private long sequence;

        /** Initializes both exact-frequency implementations. */
        @Setup(Level.Trial)
        public void setup() {
            primitive = new GrpcLatencyAccumulator(4L * Bytes.BYTES_PER_MB);
            protobufMap = MessageLatenciesRecord.newBuilder();
            sequence = 0;
        }

        private long nextLatency() {
            return 1_000 + sequence++ % DISTINCT_LATENCIES;
        }
    }

    /**
     * Complete-window encoding state rebuilt before every invocation.
     */
    @State(Scope.Thread)
    public static class EncodeState {
        private GrpcLatencyAccumulator accumulator;
        private MessageLatenciesRecord.Builder builder;

        /** Creates the same deterministic exact distribution for each encoding. */
        @Setup(Level.Invocation)
        public void setup() {
            accumulator = new GrpcLatencyAccumulator(4L * Bytes.BYTES_PER_MB);
            builder = MessageLatenciesRecord.newBuilder();
            for (int index = 0; index < DISTINCT_LATENCIES; index++) {
                accumulator.record(1_000L + index, index + 1L);
            }
        }
    }

    /**
     * Records one measurement into the primitive accumulator.
     *
     * @param state benchmark state
     * @return number of distinct latency values
     */
    @Benchmark
    public int recordPrimitive(RecordState state) {
        state.primitive.record(state.nextLatency(), 1);
        return state.primitive.size();
    }

    /**
     * Records one measurement through the legacy boxed protobuf map.
     *
     * @param state benchmark state
     * @return number of distinct latency values
     */
    @Benchmark
    public int recordProtobufMap(RecordState state) {
        final long latency = state.nextLatency();
        final long count = state.protobufMap.getLatencyMap().getOrDefault(latency, 0L);
        state.protobufMap.putLatency(latency, count + 1);
        return state.protobufMap.getLatencyCount();
    }

    /**
     * Encodes a complete exact distribution into packed primitive fields.
     *
     * @param state benchmark state
     * @return immutable protobuf message
     */
    @Benchmark
    public MessageLatenciesRecord encodePacked(EncodeState state) {
        state.accumulator.writePacked(state.builder);
        return state.builder.build();
    }

    /**
     * Encodes a complete exact distribution into the legacy protobuf map.
     *
     * @param state benchmark state
     * @return immutable protobuf message
     */
    @Benchmark
    public MessageLatenciesRecord encodeLegacyMap(EncodeState state) {
        state.accumulator.writeLegacy(state.builder);
        return state.builder.build();
    }
}
