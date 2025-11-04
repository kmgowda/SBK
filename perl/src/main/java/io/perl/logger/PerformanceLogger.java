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

import io.perl.config.LatencyConfig;
import io.perl.config.PerlConfig;
import io.time.TimeUnit;

/**
 * High-level logger interface used by the PerL framework to report benchmarking
 * metrics. Implementations receive periodic summaries and a final total summary
 * and may also be asked to record per-event latency information via
 * {@link ReportLatency}.
 *
 * <p>Important contract points:
 * <ul>
 *     <li>Latency values are expressed in the time unit returned by
 *     {@link #getTimeUnit()} (default is {@link TimeUnit#ms}).</li>
 *     <li>Implementations should avoid blocking the PerL runtime. If a
 *     logging implementation performs I/O (network or disk), prefer
 *     asynchronous or batched writes.</li>
 *     <li>The arrays passed for percentiles are parallel: values in
 *     <code>percentileLatencies</code> match indices in
 *     <code>percentileLatencyCounts</code> if provided.</li>
 *     <li>Implementations should be thread-safe if shared across threads, or
 *     documented otherwise. For example, {@link io.perl.api.PerlChannel}
 *     instances are not thread-safe and should not be shared between threads.</li>
 * </ul>
 *
 * <p>Typical usage example:
 * <pre>
 * {@code
 * PerformanceLogger logger = new DefaultLogger();
 * // PerlBuilder will accept the logger when building a Perl instance
 * Perl perl = PerlBuilder.build(logger, null, null, null);
 * }
 * </pre>
 */
public interface PerformanceLogger extends Print, ReportLatency {

    /**
     * Print the Periodic performance results.
     *
     * <p>This method is invoked periodically by the PerL framework to present
     * interval-level metrics. Implementations must interpret latency-related
     * parameters in the time unit returned by {@link #getTimeUnit()}.
     *
     * @param seconds                       Reporting duration in seconds
     * @param bytes                         Number of bytes read/write
     * @param records                       Number of records processed in the interval
     * @param recsPerSec                    Records per second
     * @param mbPerSec                      Throughput in MB per second
     * @param avgLatency                    Average latency (in {@link #getTimeUnit()})
     * @param minLatency                    Minimum latency (in {@link #getTimeUnit()})
     * @param maxLatency                    Maximum latency (in {@link #getTimeUnit()})
     * @param invalid                       Count of invalid/negative latency records
     * @param lowerDiscard                  Count of latencies below configured minimum
     * @param higherDiscard                 Count of latencies above configured maximum
     * @param slc1                          Sliding latency coverage metric (implementation-defined)
     * @param slc2                          Sliding latency coverage metric (implementation-defined)
     * @param percentileLatencies           Array holding latency values for configured percentiles
     * @param percentileLatencyCounts       Parallel array of counts for each percentile bucket (may be null)
     */
    void printTotal(double seconds, long bytes, long records, double recsPerSec, double mbPerSec, double avgLatency,
                    long minLatency, long maxLatency, long invalid, long lowerDiscard, long higherDiscard,
                    long slc1, long slc2, long[] percentileLatencies, long[] percentileLatencyCounts);

    /**
     * Default no-op implementation to record per-event latency. Implementations
     * can override to collect more detailed latency information.
     *
     * @param startTime event start time (time unit depends on caller)
     * @param events    number of events included in this record
     * @param bytes     bytes processed for the events
     * @param latency   measured latency for the event(s)
     */
    @Override
    default void recordLatency(long startTime, int events, int bytes, long latency) {

    }

    /**
     * Reporting interval in seconds (default from {@link PerlConfig}).
     *
     * @return reporting interval in seconds
     */
    default int getPrintingIntervalSeconds() {
        return PerlConfig.DEFAULT_PRINTING_INTERVAL_SECONDS;
    }

    /**
     * Time unit used to express latencies by this logger. Default is
     * {@link TimeUnit#ms}.
     *
     * @return time unit for latencies
     */
    default TimeUnit getTimeUnit() {
        return TimeUnit.ms;
    }


    /**
     * Minimum latency value considered valid by the logger (default from
     * {@link LatencyConfig}). Values below this are treated as discarded.
     *
     * @return minimum valid latency
     */
    default long getMinLatency() {
        return LatencyConfig.DEFAULT_MIN_LATENCY;
    }

    /**
     * Maximum latency value considered valid by the logger (default from
     * {@link LatencyConfig}). Values above this are treated as discarded.
     *
     * @return maximum valid latency
     */
    default long getMaxLatency() {
        return LatencyConfig.DEFAULT_MAX_LATENCY;
    }

    /**
     * Percentile fractions (for example {@code 0.5} for the 50th percentile)
     * that this logger expects to receive. The PerL framework will compute
     * percentile latency values using the configured fractions and pass them
     * back in print calls.
     *
     * @return array of percentile fractions
     */
    default double[] getPercentiles() {
        return LatencyConfig.PERCENTILES.clone();
    }
}
