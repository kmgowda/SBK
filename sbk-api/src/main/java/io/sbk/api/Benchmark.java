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

import java.io.IOException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

/**
 * Interface for Benchmark.
 */
public interface Benchmark {

    /**
     * Start the Benchmark.
     *
     * @return CompletableFuture.
     * @throws IOException           If an exception occurred.
     * @throws InterruptedException  If an exception occurred
     * @throws ExecutionException    If an exception occurred
     * @throws IllegalStateException If an exception occurred.
     */
    CompletableFuture<Void> start() throws IOException, InterruptedException, ExecutionException, IllegalStateException;

    /**
     * stop/shutdown the Benchmark.
     */
    void stop();
}