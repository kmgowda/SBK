/**
 * Copyright (c) KMG. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 */
package io.sbk.api;

import io.perl.api.PerlChannel;
import io.sbk.data.impl.ByteArray;
import io.sbk.data.impl.SbkString;
import io.sbk.logger.ReadRequestsLogger;
import io.time.NanoSeconds;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Param;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.Warmup;

import java.io.IOException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

/** Diagnostic before/after benchmarks for async completion and string timestamps. */
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
@Warmup(iterations = 3, time = 1)
@Measurement(iterations = 5, time = 1)
@Fork(3)
public class ReaderDataBenchmark {
    /** Async callback state, including an observable measurement sink. */
    @State(Scope.Thread)
    public static class ReadState implements PerlChannel, ReadRequestsLogger {
        /** Selects ordinary or embedded-timestamp reads with optional request logging. */
        @Param({"read", "readLogged", "timestamp", "timestampLogged"})
        public String mode;
        /** Selects completion before or after callback registration. */
        @Param({"true", "false"})
        public boolean immediate;
        private final ByteArray dataType = new ByteArray();
        private final byte[] payload = dataType.setTime(new byte[100], 1);
        private final NanoSeconds time = new NanoSeconds();
        private final Status status = new Status();
        private CompletableFuture<byte[]> completion;
        private final AsyncReader<byte[]> reader = size -> completion;
        private long result;
        private long requests;

        @Override
        public void recordReadRequests(int id, long start, long bytes, long events) {
            requests += events;
        }

        @Override
        public void recordReadTimeoutEvents(int id, long start, long events) {
            throw new IllegalStateException("Unexpected timeout in successful-read benchmark");
        }

        @Override
        public int getMaxReaderIDs() {
            return 1;
        }

        @Override
        public void send(long startTime, long endTime, int records, int bytes) {
            result = endTime - startTime + records + bytes;
        }

        @Override
        public void throwException(Throwable ex) {
            throw new IllegalStateException(ex);
        }
    }

    /** String data with a changing timestamp and a stable payload suffix. */
    @State(Scope.Thread)
    public static class StringState {
        private final SbkString dataType = new SbkString();
        private final String payload = "x".repeat(100);
        private long timestamp = 1_000_000_000_000_000L;
        private String encoded;

        /** Creates a valid timestamped record using the implementation under test. */
        @Setup
        public void setup() {
            encoded = dataType.setTime(payload, timestamp);
        }
    }

    /**
     * Measures successful async submission and completion without a backend.
     * @param state reader state
     * @return observable measurement
     * @throws IOException if the reader fails
     * @throws IllegalArgumentException if the benchmark mode is invalid
     */
    @Benchmark
    public long asyncCompletion(ReadState state) throws IOException {
        state.completion = state.immediate ? CompletableFuture.completedFuture(state.payload)
                : new CompletableFuture<>();
        switch (state.mode) {
            case "read" -> state.reader.recordRead(state.dataType, 100, state.time, state.status, state);
            case "readLogged" -> state.reader.recordRead(state.dataType, 100, state.time, state.status,
                    state, 0, state);
            case "timestamp" -> state.reader.recordReadTime(state.dataType, 100, state.time, state.status, state);
            case "timestampLogged" -> state.reader.recordReadTime(state.dataType, 100, state.time, state.status,
                    state, 0, state);
            default -> throw new IllegalArgumentException(state.mode);
        }
        if (!state.immediate) {
            state.completion.complete(state.payload);
        }
        return state.result + state.requests;
    }

    /**
     * Measures timestamp encoding and its allocations.
     * @param state string state
     * @return timestamped payload
     */
    @Benchmark
    public String stringSetTime(StringState state) {
        return state.dataType.setTime(state.payload, state.timestamp++);
    }

    /**
     * Measures timestamp decoding and its allocations.
     * @param state string state
     * @return decoded timestamp
     */
    @Benchmark
    public long stringGetTime(StringState state) {
        return state.dataType.getTime(state.encoded);
    }
}
