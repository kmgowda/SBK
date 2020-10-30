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
 * Interface for recoding/printing results.
 */
public interface ResultLogger extends Logger {

    /**
     * Print the Total Periodic performance results.
     * @param bytes number of bytes read/write
     * @param records data to write.
     * @param recsPerSec  records per second.
     * @param mbPerSec Throughput value in terms of MB (Mega Bytes) per Second.
     * @param avgLatency Average Latency.
     * @param maxLatency Maximum Latency.
     * @param lowerDiscard number of discarded latencies which are less than minimum latency.
     * @param higherDiscard number of discarded latencies which are higher than maximum latency.
     * @param percentiles Array of percentiles.
     */
    void printTotal(long bytes, long records, double recsPerSec, double mbPerSec, double avgLatency,
               int maxLatency, long lowerDiscard, long higherDiscard, int[] percentiles);
}
