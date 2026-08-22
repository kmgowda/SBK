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

import io.perl.api.PeriodicRecorder;
import io.perl.api.impl.ArrayLatencyRecorder;
import io.perl.api.impl.ArrayWindowLatencyPeriodicRecorder;
import io.perl.api.impl.ArrayWindowPeriodicRecorder;
import io.perl.api.impl.TotalWindowLatencyPeriodicRecorder;
import io.perl.api.impl.TotalWindowPeriodicRecorder;
import io.perl.logger.impl.DefaultLogger;
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

/** Measures periodic recording with and without an individual-latency callback. */
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
@Warmup(iterations = 3, time = 1)
@Measurement(iterations = 5, time = 1)
@Fork(2)
public class PeriodicRecorderCallbackBenchmark {

    /** Thread-confined recorder state. */
    @State(Scope.Thread)
    public static class RecorderState {
        private static final long MAX_LATENCY = 4_095;
        private final Time time = new NanoSeconds();
        private PeriodicRecorder callbackRecorder;
        private PeriodicRecorder arrayCallbackRecorder;
        private PeriodicRecorder noCallbackRecorder;
        private PeriodicRecorder arrayRecorder;
        private long timestamp;

        /** Creates equivalent callback and callback-free recorders. */
        @Setup(Level.Trial)
        public void setup() {
            final DefaultLogger logger = new DefaultLogger();
            callbackRecorder = new TotalWindowLatencyPeriodicRecorder(
                    newWindow(), newWindow(), logger, logger::printTotal,
                    logger::recordLatency, time);
            arrayCallbackRecorder = new ArrayWindowLatencyPeriodicRecorder(
                    newWindow(), newWindow(), logger, logger::printTotal,
                    logger::recordLatency, time);
            noCallbackRecorder = new TotalWindowPeriodicRecorder(
                    newWindow(), newWindow(), logger, logger::printTotal, time);
            arrayRecorder = new ArrayWindowPeriodicRecorder(
                    newWindow(), newWindow(), logger, logger::printTotal, time);
            timestamp = 1;
        }

        private ArrayLatencyRecorder newWindow() {
            return new ArrayLatencyRecorder(0, MAX_LATENCY, Long.MAX_VALUE,
                    Long.MAX_VALUE, Long.MAX_VALUE, new double[]{0.5, 0.99}, time);
        }

        private long nextTimestamp() {
            return timestamp++;
        }
    }

    /**
     * Measures the legacy recorder callback path.
     *
     * @param state recorder state
     */
    @Benchmark
    public void withNoOpCallback(RecorderState state) {
        final long startTime = state.nextTimestamp();
        state.callbackRecorder.record(startTime, startTime + 100, 1, 100);
    }

    /**
     * Measures the specialized array recorder with a no-op callback.
     *
     * @param state recorder state
     */
    @Benchmark
    public void arrayWithNoOpCallback(RecorderState state) {
        final long startTime = state.nextTimestamp();
        state.arrayCallbackRecorder.record(startTime, startTime + 100, 1, 100);
    }

    /**
     * Measures the callback-free recorder selected for standard loggers.
     *
     * @param state recorder state
     */
    @Benchmark
    public void withoutCallback(RecorderState state) {
        final long startTime = state.nextTimestamp();
        state.noCallbackRecorder.record(startTime, startTime + 100, 1, 100);
    }

    /**
     * Measures the array-window write without the periodic overflow check.
     *
     * @param state recorder state
     */
    @Benchmark
    public void arrayWithoutCallback(RecorderState state) {
        final long startTime = state.nextTimestamp();
        state.arrayRecorder.record(startTime, startTime + 100, 1, 100);
    }
}
