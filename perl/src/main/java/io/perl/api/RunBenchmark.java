/**
 * Copyright (c) KMG. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.perl.api;

import java.io.IOException;
import java.util.concurrent.CompletableFuture;

/**
 * Contract for starting a benchmark run. The implementation should start any
 * required background tasks and return a {@link CompletableFuture} that
 * completes when the benchmark run finishes (either naturally or due to an
 * error).
 */
public interface RunBenchmark {

    /**
     * Start the performance benchmark.
     *
     * <p>If {@code secondsToRun} &gt; 0 the benchmark should run for the given
     * number of seconds and then stop; if {@code secondsToRun} == 0 the
     * benchmark should run until {@code recordsCount} records are processed.
     *
     * The returned {@link CompletableFuture} completes when the benchmark
     * finishes. The future completes exceptionally if an error occurs.
     *
     * @param secondsToRun Number of seconds to run; when > 0 this takes precedence
     *                     over {@code recordsCount}
     * @param recordsCount When {@code secondsToRun} == 0, this indicates the total
     *                     number of records to process before completing
     * @return a CompletableFuture that completes when the run finishes
     * @throws IllegalStateException If the benchmark cannot be started due to
     *                               incorrect state
     * @throws IOException           if an I/O error occurs while starting
     */
    CompletableFuture<Void> run(long secondsToRun, long recordsCount) throws IOException,
            IllegalStateException;
}
