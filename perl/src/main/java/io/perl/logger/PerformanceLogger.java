/**
 * Copyright (c) KMG. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.perl.logger;

import io.perl.api.LatencyConfig;
import io.perl.api.PerlConfig;
import io.time.TimeUnit;

public interface PerformanceLogger extends Print {

    /**
     * Print the Total Periodic performance results.
     *
     * @param seconds       reporting seconds
     * @param bytes         number of bytes read/write
     * @param records       data to write.
     * @param recsPerSec    records per second.
     * @param mbPerSec      Throughput value in terms of MB (Mega Bytes) per Second.
     * @param avgLatency    Average Latency.
     * @param maxLatency    Maximum Latency.
     * @param invalid       Number of invalid/negative latencies
     * @param lowerDiscard  number of discarded latencies which are less than minimum latency.
     * @param higherDiscard number of discarded latencies which are higher than maximum latency.
     * @param slc1          Sliding Latency Coverage factor
     * @param slc2          Sliding Latency Coverage factor
     * @param percentiles   Array of percentiles.
     */
    void printTotal(double seconds, long bytes, long records, double recsPerSec, double mbPerSec, double avgLatency,
                    long maxLatency, long invalid, long lowerDiscard, long higherDiscard,
                    long slc1, long slc2, long[] percentiles);

    /**
     * Default implementation of Reporting interval.
     *
     * @return reporting time interval in seconds.
     */
    default int getPrintingIntervalSeconds() {
        return PerlConfig.DEFAULT_PRINTING_INTERVAL_SECONDS;
    }

    /**
     * Default implementation of time Unit.
     * Default time unit is Milliseconds.
     *
     * @return time unit.
     */
    default TimeUnit getTimeUnit() {
        return TimeUnit.ms;
    }

    /**
     * Default implementation of minimum latency.
     *
     * @return minimum latency value.
     */
    default long getMinLatency() {
        return LatencyConfig.DEFAULT_MIN_LATENCY;
    }

    /**
     * Default implementation of Maximum latency.
     *
     * @return Maximum latency value.
     */
    default long getMaxLatency() {
        return LatencyConfig.DEFAULT_MAX_LATENCY;
    }

    /**
     * Default implementation of percentile Indices.
     *
     * @return array of percentile Indices.
     */
    default double[] getPercentiles() {
        return LatencyConfig.PERCENTILES;
    }
}
