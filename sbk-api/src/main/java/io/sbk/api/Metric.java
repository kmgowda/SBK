/**
 * Copyright (c) KMG. All Rights Reserved..
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 */

package io.sbk.api;
import io.micrometer.core.instrument.MeterRegistry;

/**
 * Interface for Metrics.
 */
public interface Metric {

    /**
     * Add the Metric type specific command line arguments.
     * @param params Parameters object to be extended.
     */
    void addArgs(final Parameters params);

    /**
     * Parse the Metric specific command line arguments.
     * @param params Parameters object to be parsed for driver specific parameters/arguments.
     * @throws IllegalArgumentException If an exception occurred.
     */
    void parseArgs(final Parameters params) throws IllegalArgumentException;

    /**
     * Create the Metric.
     * @param params Parameters object enclosing all commandline arguments,
     *              see {@link io.sbk.api.Parameters} to get the basic benchmarking parameters.
     * @return return the Metric Registry.
     * @throws IllegalArgumentException If an exception occurred.
     */
    MeterRegistry createMetric(final Parameters params) throws IllegalArgumentException;
}
