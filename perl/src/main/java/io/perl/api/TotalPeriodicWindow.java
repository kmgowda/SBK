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

/**
 * Interface that extends {@link PeriodicWindow} with lifecycle methods for a
 * total aggregation window. Implementations maintain a global view across the
 * entire benchmark run and can be used to print final aggregated results.
 */
public interface TotalPeriodicWindow extends PeriodicWindow {

    /**
     * Start the total aggregation window at the given start time. Called once
     * when the benchmark run begins.
     *
     * @param startTime starting time.
     */
    void start(long startTime);


    /**
     * Stop the total aggregation window and flush final totals.
     *
     * @param endTime current time.
     */
    void stop(long endTime);
}
