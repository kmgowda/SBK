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

/**
 * Interface for recoding/printing results.
 */
public interface Logger extends Print {

    /**
     * Add the Metric type specific command line arguments.
     * @param params Parameters object to be extended.
     * @throws IllegalArgumentException If an exception occurred.
     */
    void addArgs(final Parameters params) throws IllegalArgumentException;

    /**
     * Parse the Metric specific command line arguments.
     * @param params Parameters object to be parsed for driver specific parameters/arguments.
     * @throws IllegalArgumentException If an exception occurred.
     */
    void parseArgs(final Parameters params) throws IllegalArgumentException;


    /**
     * Open the Logger.
     * @param params Parameters object to be parsed for driver specific parameters/arguments.
     * @param storageName The Name of the storage.
     * @param action  action to print
     * @throws IOException If an exception occurred.
     */
    void open(final Parameters params, final String storageName, final Action action) throws IOException;

    /**
     * Close the Logger.
     * @param params Parameters object to be parsed for driver specific parameters/arguments.
     * @throws IOException If an exception occurred.
     */
    void close(final Parameters params) throws IOException;


    /**
     * Print the Total Periodic performance results.
     * @param bytes number of bytes read/write
     * @param records data to write.
     * @param recsPerSec  records per second.
     * @param mbPerSec Throughput value in terms of MB (Mega Bytes) per Second.
     * @param avgLatency Average Latency.
     * @param maxLatency Maximum Latency.
     * @param invalid   Number of invalid/negative latencies
     * @param lowerDiscard number of discarded latencies which are less than minimum latency.
     * @param higherDiscard number of discarded latencies which are higher than maximum latency.
     * @param percentiles Array of percentiles.
     */
    void printTotal(long bytes, long records, double recsPerSec, double mbPerSec, double avgLatency,
                    int maxLatency, long invalid, long lowerDiscard, long higherDiscard, int[] percentiles);


    /**
     * Default implementation of Reporting interval.
     * @return reporting time interval in seconds.
     */
    default int getReportingIntervalSeconds() {
        return Config.DEFAULT_REPORTING_INTERVAL_SECONDS;
    }

    /**
     * Default implementation of time Unit.
     * Default time unit is Milliseconds.
     * @return time unit.
     */
    default TimeUnit getTimeUnit() {
        return TimeUnit.ms;
    }

    /**
     * Default implementation of minimum latency.
     * @return minimum latency value.
     */
    default int getMinLatency() {
        return Config.DEFAULT_MIN_LATENCY;
    }

    /**
     * Default implementation of Maximum latency.
     * @return Maximum latency value.
     */
    default int getMaxWindowLatency() {
        return Config.DEFAULT_WINDOW_LATENCY;
    }

    /**
     * Default implementation of Maximum latency.
     * @return Maximum latency value.
     */
    default int getMaxLatency() {
        return Config.DEFAULT_MAX_LATENCY;
    }

    /**
     * Default implementation of percentile Indices.
     * @return array of percentile Indices.
     */
    default double[] getPercentileIndices() {
        return Config.PERCENTILES;
    }
}
