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
     * @param writers                        number of active writers
     * @param maxWriters                     Max writers
     * @param readers                        number of active readers
     * @param maxReaders                     Max Readers
     * @param writeRequestBytes              Write requests Bytes
     * @param writeRequestMbPerSec           Write requests MB/sec
     * @param writeRequestRecords            Write Requests
     * @param writeRequestRecordsPerSec      Write Requests/sec
     * @param readRequestBytes               Read requests Bytes
     * @param readRequestMbPerSec            Read requests MB/sec
     * @param readRequestRecords             Read requests
     * @param readRequestRecordsPerSec      Read Requests/sec
     * @param writeResponsePendingRecords    Write response pending records
     * @param writeResponsePendingBytes      Write response pending bytes
     * @param readResponsePendingRecords     Read response pending records
     * @param readResponsePendingBytes       Read response pending bytes
     * @param writeReadRequestPendingRecords Write read pending records
     * @param writeReadRequestPendingBytes   Write read pending bytes
     * @param writeTimeoutEvents             Timeout Write Events
     * @param writeTimeoutEventsPerSec       Timeout Write Events/sec
     * @param readTimeoutEvents              Timeout Read Events
     * @param readTimeoutEventsPerSec        Timeout Write Events/sec
     * @param seconds                        reporting duration in seconds
     * @param bytes                          number of bytes read/write
     * @param records                        data to write.
     * @param recsPerSec                     records per second.
     * @param mbPerSec                       Throughput value in terms of MB (Mega Bytes) per Second.
     * @param avgLatency                     Average Latency.
     * @param minLatency                     Minimum Latency.
     * @param maxLatency                     Maximum Latency.
     * @param invalid                        Number of invalid/negative latencies.
     * @param lowerDiscard                   number of discarded latencies which are less than minimum latency.
     * @param higherDiscard                  number of discarded latencies which are higher than maximum latency.
     * @param slc1                           Sliding Latency Coverage factor
     * @param slc2                           Sliding Latency Coverage factor
     * @param percentileValues               Array of percentile Values.
     */
    void print(int writers, int maxWriters, int readers, int maxReaders,
               long writeRequestBytes, double writeRequestMbPerSec, long writeRequestRecords,
               double writeRequestRecordsPerSec, long readRequestBytes, double readRequestMbPerSec,
               long readRequestRecords, double readRequestRecordsPerSec, long writeResponsePendingRecords,
               long writeResponsePendingBytes, long readResponsePendingRecords, long readResponsePendingBytes,
               long writeReadRequestPendingRecords, long writeReadRequestPendingBytes,
               long writeTimeoutEvents, double writeTimeoutEventsPerSec,
               long readTimeoutEvents, double readTimeoutEventsPerSec,
               double seconds, long bytes, long records, double recsPerSec, double mbPerSec,
               double avgLatency, long minLatency, long maxLatency, long invalid, long lowerDiscard,
               long higherDiscard, long slc1, long slc2, long[] percentileValues);
}
