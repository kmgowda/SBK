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
public interface ResultLogger {

    /**
     * Print the Periodic performance results.
     * @param action  Name of the action/operation.
     * @param bytes number of bytes read/write
     * @param records data to write.
     * @param recsPerSec  records per second.
     * @param mbPerSec Throughput value in terms of MB (Mega Bytes) per Second.
     * @param avgLatency Average Latency.
     * @param maxLatency Maximum Latency.
     * @param discard number of discarded latencies.
     * @param one 50th Percentile.
     * @param two  75th Percentile.
     * @param three 95th Percentile.
     * @param four 99th Percentile
     * @param five 99.9th Percentile
     * @param six  99.99th Percentile     *
     */
    void print(String action, long bytes, long records, double recsPerSec, double mbPerSec, double avgLatency,
               int maxLatency, long discard, int one, int two, int three, int four, int five, int six);

}
