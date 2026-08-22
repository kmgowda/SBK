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
import io.time.NanoSeconds;
import io.time.Time;
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
import org.openjdk.jmh.annotations.Warmup;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Measures native duration checks and callback-reader mode specialization.
 */
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
@Warmup(iterations = 3, time = 1)
@Measurement(iterations = 5, time = 1)
@Fork(2)
public class CoreHotPathBenchmark {

    /** State for native and millisecond duration checks. */
    @State(Scope.Thread)
    public static class DurationState {
        private final Time time = new NanoSeconds();
        private final long startTime = time.getCurrentTime();
        private final long endTime = startTime + Time.NS_PER_SEC;
        private final long timeUnitsToRun = time.secondsToTimeUnits(60);
        private final long millisecondsToRun = 60L * Time.MS_PER_SEC;
    }

    /** State for optimized and legacy callback completion accounting. */
    @State(Scope.Thread)
    public static class CallbackState {
        private final Time time = new NanoSeconds();
        private final AtomicLong legacyCount = new AtomicLong();
        private final PerlChannel channel = new NoOpChannel();
        private BenchmarkCallbackReader reader;
        private long beginTime;
        private long endTime;
        private long timeUnitsToRun;

        /**
         * Initializes a duration-mode callback reader.
         *
         * @throws Exception if callback-reader initialization fails
         */
        @Setup(Level.Trial)
        public void setup() throws Exception {
            reader = new BenchmarkCallbackReader();
            reader.initialize(new BenchmarkWorker(channel), 60, 0,
                    new ByteArray(), time, data -> { });
            beginTime = time.getCurrentTime();
            endTime = beginTime + 1;
            timeUnitsToRun = time.secondsToTimeUnits(60);
        }
    }

    /**
     * Measures the former floating-point millisecond duration comparison.
     *
     * @param state duration state
     * @return comparison result
     */
    @Benchmark
    public boolean millisecondDurationCheck(DurationState state) {
        return state.time.elapsedMilliSeconds(state.endTime, state.startTime)
                < state.millisecondsToRun;
    }

    /**
     * Measures the native integer duration comparison.
     *
     * @param state duration state
     * @return comparison result
     */
    @Benchmark
    public boolean nativeDurationCheck(DurationState state) {
        return state.time.elapsed(state.endTime, state.startTime) < state.timeUnitsToRun;
    }

    /**
     * Measures the duration-specialized callback path.
     *
     * @param state callback state
     */
    @Benchmark
    public void durationCallback(CallbackState state) {
        state.reader.recordBenchmark(state.beginTime, state.endTime, 100, 1);
    }

    /**
     * Measures the previous callback path's unconditional atomic counter.
     *
     * @param state callback state
     * @return cumulative callback count
     */
    @Benchmark
    public long legacyDurationCallback(CallbackState state) {
        final long count = state.legacyCount.addAndGet(1);
        state.channel.send(state.beginTime, state.endTime, 1, 100);
        if (state.time.elapsed(state.endTime, state.beginTime) >= state.timeUnitsToRun) {
            state.reader.complete();
        }
        return count;
    }

    private static final class BenchmarkCallbackReader extends AbstractCallbackReader<byte[]> {
        @Override
        public void start(Callback<byte[]> callback) {
        }

        @Override
        public void stop() {
        }
    }

    private static final class BenchmarkWorker extends Worker {
        private BenchmarkWorker(PerlChannel channel) {
            super(0, null, channel);
        }
    }

    private static final class NoOpChannel implements PerlChannel {
        @Override
        public void send(long startTime, long endTime, int records, int bytes) {
        }

        @Override
        public void throwException(Throwable ex) {
            throw new IllegalStateException(ex);
        }
    }
}
