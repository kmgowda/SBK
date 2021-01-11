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

/**
 * Interface for performance Statistics.
 */
public interface Performance {

     /**
      * Start the performance Benchmark.
      *
      * @param secondsToRun number of seconds to Run
      * @param records Maximum number of records to count.If this value 0 or less than 0,then runs till secondsToRun.
      * @return CompletableFuture.
      * @throws IllegalStateException If an exception occurred.
      * @throws IOException If an exception occurred.
      */
     CompletableFuture<Void> start(int secondsToRun, int records) throws IOException, IllegalStateException;

     /**
      * stop/shutdown the Benchmark.
      *
      */
     void stop();

     /**
     * Get the Time recorder for benchmarking.
     *
     * @return RecordTime Interface if its available ; returns null in case of failure.
     */
     SendChannel get();
}
