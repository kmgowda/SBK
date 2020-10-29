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

/**
 * Interface for Rate or Throughput Controller.
 */
public interface RateController {

    /**
     * Start the Rate Controller.
     *
     * @param recordsPerSec Records Per Second.
     */
    void start(int recordsPerSec);

    /**
     * Blocks for small amounts of time to achieve target Throughput/events per sec.
     *
     * @param records current cumulative records
     * @param elapsedSec   Elapsed Seconds
     */
    void control(long records, float elapsedSec);

}
