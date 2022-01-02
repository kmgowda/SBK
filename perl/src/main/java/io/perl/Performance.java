/**
 * Copyright (c) KMG. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 */
package io.perl;

/**
 * Interface for performance Statistics.
 */
public interface Performance extends RunBenchmark {

    /**
     * stop/shutdown the Benchmark.
     */
    void stop();

    /**
     * Get the SendChannel to get the benchmark results.
     *
     * @return SendChannel to get the benchmark results.
     */
    SendChannel getSendChannel();
}
