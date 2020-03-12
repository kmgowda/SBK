/**
 * Copyright (c) KMG. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 */

package io.sbk.api;

import java.io.IOException;
import java.util.concurrent.CompletableFuture;

public interface Benchmark {

    /**
     * Start the Benchmark.
     * @return CompletableFuture.
     * @throws IllegalStateException If an exception occurred.
     * @throws IOException If an exception occurred.
     */
    CompletableFuture<Void> start() throws IOException, IllegalStateException;

    /**
     * stop/shutdown the Benchmark.
     */
    void stop();
}