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

/**
 * Print is the primary interface used by the PerL framework to emit periodic
 * performance metrics. Implementations of this interface are responsible for
 * rendering or exporting periodic summary information about the benchmark run
 * (for example to the console, a file, or a remote metrics system).
 *
 * <p>Implementations should format the provided values in a human readable
 * form and may include additional contextual information. The method
 * parameters follow the conventions used across the PerL APIs:
 * <ul>
 *     <li><b>seconds</b> - elapsed reporting duration in seconds</li>
 *     <li><b>bytes</b> - total bytes transferred during the interval</li>
 *     <li><b>records</b> - number of records processed</li>
 *     <li><b>recsPerSec</b> / <b>mbPerSec</b> - throughput metrics</li>
 *     <li><b>avgLatency/minLatency/maxLatency</b> - latency metrics in the
 *         {@link io.perl.logger.PerformanceLogger#getTimeUnit() PerformanceLogger time unit}
 *     </li>
 *     <li><b>invalid / lowerDiscard / higherDiscard</b> - counts for discarded
 *         or invalid latency measurements</li>
 *     <li><b>slc1 / slc2</b> - sliding latency coverage (implementation-defined)</li>
 *     <li><b>percentileLatencies</b> - array of latency values for configured percentiles</li>
 *     <li><b>percentileLatencyCounts</b> - array of counts corresponding to each percentile
 *         bucket (may be null or empty if not provided)</li>
 * </ul>
 *
 * <p>Implementation notes / contract:
 * <ul>
 *     <li>The length of <code>percentileLatencies</code> and
 *         <code>percentileLatencyCounts</code> (when provided) should match the
 *         percentiles configured by the {@link io.perl.logger.PerformanceLogger}
 *         implementation (see {@link io.perl.logger.PerformanceLogger#getPercentiles()}).
 *     </li>
 *     <li>Latency values are expressed in the time unit returned by
 *         {@link io.perl.logger.PerformanceLogger#getTimeUnit()} (default: milliseconds).
 *     </li>
 *     <li>The <code>print</code> method is invoked periodically by the PerL
 *         framework; implementations must avoid long blocking operations. If
 *         long-running work is required (for example I/O to a remote system),
 *         perform it asynchronously to avoid delaying PerL's internal timers.
 *     </li>
 *     <li>Thread-safety: a single {@link io.perl.logger.PerformanceLogger}
 *         instance is typically shared by the PerL process; implementations
 *         should be thread-safe or use appropriate synchronization when used
 *         concurrently.</li>
 * </ul>
 *
 * <p>Simple example (console logger):
 * <pre>
 * {@code
 * Print logger = (seconds, bytes, records, recsPerSec, mbPerSec, avgLatency,
 *                    minLatency, maxLatency, invalid, lowerDiscard, higherDiscard,
 *                    slc1, slc2, pctLat, pctCounts) -> {
 *     System.out.printf("%ds: %d records, %.2f rec/s, avg=%fms\n",
 *             (long)seconds, records, recsPerSec, avgLatency);
 * };
 * }
 * </pre>
 */
public interface Print {

    /**
     * Print the periodic performance results.
     *
     * <p>Implementations should interpret the latency values according to the
     * {@link io.perl.logger.PerformanceLogger#getTimeUnit()} provided by the
     * configured logger. The arrays <code>percentileLatencies</code> and
     * <code>percentileLatencyCounts</code> (if non-null) are parallel: the
     * element at index <code>i</code> in <code>percentileLatencyCounts</code>
     * corresponds to the latency value at index <code>i</code> in
     * <code>percentileLatencies</code>.
     *
     * @param seconds                   Reporting duration in seconds
     * @param bytes                     Number of bytes read/write in the interval
     * @param records                   Number of records processed in the interval
     * @param recsPerSec                Records per second measured over the interval
     * @param mbPerSec                  Throughput measured in MB per second
     * @param avgLatency                Average latency (in PerformanceLogger time unit)
     * @param minLatency                Minimum latency (in PerformanceLogger time unit)
     * @param maxLatency                Maximum latency (in PerformanceLogger time unit)
     * @param invalid                   Number of invalid/negative latency measurements
     * @param lowerDiscard              Number of latencies discarded because they were below the min threshold
     * @param higherDiscard             Number of latencies discarded because they were above the max threshold
     * @param slc1                      Sliding Latency Coverage metric 1 (implementation-defined)
     * @param slc2                      Sliding Latency Coverage metric 2 (implementation-defined)
     * @param percentileLatencies       Array of percentile latency values (parallel to percentileLatencyCounts)
     * @param percentileLatencyCounts   Array of counts for each percentile bucket; may be null
     */
    void print(double seconds, long bytes, long records, double recsPerSec, double mbPerSec, double avgLatency,
               long minLatency, long maxLatency, long invalid, long lowerDiscard, long higherDiscard,
               long slc1, long slc2, long[] percentileLatencies, long[] percentileLatencyCounts);
}
