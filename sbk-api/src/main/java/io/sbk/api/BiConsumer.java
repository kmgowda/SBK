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

import java.io.EOFException;
import java.io.IOException;

public interface BiConsumer {

    /**
     * Apply the Benchmark.
     *
     * @param secondsToRun number of seconds to Run
     * @param recordsCount Maximum number of records to count.
     *                If this value 0 or less than 0,then run the benchmark till secondsToRun.
     *
     * @throws IllegalStateException If an exception occurred.
     * @throws EOFException End of File exception
     * @throws IOException If an exception occurred.
     */
    void apply(long secondsToRun, long recordsCount) throws IOException, EOFException, IllegalStateException;
}
