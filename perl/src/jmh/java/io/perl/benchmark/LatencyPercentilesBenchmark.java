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
import org.openjdk.jmh.annotations.Warmup;

import java.util.concurrent.TimeUnit;

/** Measures percentile target preparation once per reporting window. */
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
@Warmup(iterations = 3, time = 1)
@Measurement(iterations = 5, time = 1)
@Fork(2)
public class LatencyPercentilesBenchmark {
    /** Reuses the result arrays across reporting windows. */
    @State(Scope.Thread)
    public static class WindowState {
        /** Exercises both existing percentile lists and lists that include P100. */
        @Param({"false", "true"})
        public boolean includeMaximum;

        private LatencyPercentiles percentiles;

        /** Creates the configured result arrays before measurement. */
        @Setup(Level.Trial)
        public void setUp() {
            final double[] fractions = includeMaximum
                    ? new double[]{0.05, 0.5, 0.9, 0.99, 0.999, 1.0}
                    : new double[]{0.05, 0.5, 0.9, 0.99, 0.999, 0.9999};
            percentiles = new LatencyPercentiles(fractions);
        }
    }

    /**
     * Prepares percentile targets for a one-million-record window.
     *
     * @param state reusable percentile state
     * @return state consumed by JMH
     */
    @Benchmark
    public LatencyPercentiles reset(WindowState state) {
        state.percentiles.reset(1_000_000);
        return state.percentiles;
    }
}
