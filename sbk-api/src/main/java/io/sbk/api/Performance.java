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

import java.util.concurrent.ExecutionException;

/**
 * Interface for performance Statistics.
 */
public interface Performance extends RecordTime {
    /**
     * Start the performance statistics.
     *
     * @param startTime start time time
     */
    void start(long startTime);

    /**
     * End the final performance statistics.
     *
     * @param endTime End time
     * @throws ExecutionException   If an exception occurred.
     * @throws InterruptedException If an exception occurred.
     */
    void shutdown(long endTime) throws ExecutionException, InterruptedException;
}
