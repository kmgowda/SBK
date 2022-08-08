/**
 * Copyright (c) KMG. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 */
package io.sbk.logger;

public interface RWPrint {

    /**
     * Print the Periodic performance results.
     *
     * @param writers               number of active writers
     * @param maxWriters            Max writers
     * @param readers               number of active readers
     * @param maxReaders            Max Readers
     * @param writeRequestBytes     Write requests Bytes
     * @param writeRequestsMbPerSec Write requests MB/sec
     * @param writeRequests         Write Requests
     * @param writeRequestsPerSec   Write Requests/sec
     * @param readRequestBytes      Read requests Bytes
     * @param readRequestsMbPerSec  Read requests MB/sec
     * @param readRequests          Read requests
     * @param readRequestsPerSec    Read Requests/sec
     * @param seconds               reporting duration in seconds
     * @param bytes                 number of bytes read/write
     * @param records               data to write.
     * @param recsPerSec            records per second.
     * @param mbPerSec              Throughput value in terms of MB (Mega Bytes) per Second.
     * @param avgLatency            Average Latency.
     * @param minLatency            Minimum Latency.
     * @param maxLatency            Maximum Latency.
     * @param invalid               Number of invalid/negative latencies.
     * @param lowerDiscard          number of discarded latencies which are less than minimum latency.
     * @param higherDiscard         number of discarded latencies which are higher than maximum latency.
     * @param slc1                  Sliding Latency Coverage factor
     * @param slc2                  Sliding Latency Coverage factor
     * @param percentileValues      Array of percentile Values.
     */
    void print(int writers, int maxWriters, int readers, int maxReaders,
               long writeRequestBytes, double writeRequestsMbPerSec, long writeRequests,
               double writeRequestsPerSec, long readRequestBytes, double readRequestsMbPerSec,
               long readRequests, double readRequestsPerSec, double seconds, long bytes,
               long records, double recsPerSec, double mbPerSec,
               double avgLatency, long minLatency, long maxLatency, long invalid, long lowerDiscard,
               long higherDiscard, long slc1, long slc2, long[] percentileValues);
}