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

/**
 * Read/Write logger interface that combines request logging and printing hooks.
 *
 * Extends {@link Logger}, {@link CountRW}, {@link WriteRequestsLogger}, {@link ReadRequestsLogger},
 * and {@link RWPrint} to provide a unified contract for SBK loggers capable of handling
 * both read and write workloads. Default methods return neutral values or no-ops so that
 * implementations can selectively override features.
 */
public non-sealed interface RWLogger extends Logger, CountRW, WriteRequestsLogger, ReadRequestsLogger, RWPrint {

    /**
     * Default method to record every/multiple write event(s).
     * No-op by default.
     */
    @Override
    default void recordWriteRequests(int writerId, long startTime, long bytes, long events) {

    }

    /**
     * Default method to record every/multiple read event(s).
     * No-op by default.
     */
    @Override
    default void recordReadRequests(int readerId, long startTime, long bytes, long events) {

    }

    /**
     * Default method to indicate to record write requests or not.
     * Returning 0 disables per-writer request logging.
     */
    @Override
    default int getMaxWriterIDs() {
        return 0;
    }

    /**
     * Default method to indicate to record read requests or not.
     * Returning 0 disables per-reader request logging.
     */
    @Override
    default int getMaxReaderIDs() {
        return 0;
    }


    /**
     * Print the "Total" roll-up, typically at the end of a benchmark run.
     * Parameters mirror {@link RWPrint#print(int, int, int, int, long, double, long, double, long, double, long, double, long, long, long, long, long, long, long, double, long, double, double, long, long, double, double, double, long, long, long, long, long, long, long, long[], long[])} 
     * @param writers                         number of active writers
     * @param maxWriters                      maximum writers seen
     * @param readers                         number of active readers
     * @param maxReaders                      maximum readers seen
     * @param writeRequestBytes               total write request bytes
     * @param writeRequestMbPerSec            write request throughput in MB/sec
     * @param writeRequestRecords             total write request count
     * @param writeRequestRecordsPerSec       write requests per second
     * @param readRequestBytes                total read request bytes
     * @param readRequestMBPerSec             read request throughput in MB/sec
     * @param readRequestRecords              total read request count
     * @param readRequestRecordsPerSec        read requests per second
     * @param writeResponsePendingRecords     pending write response records
     * @param writeResponsePendingBytes       pending write response bytes
     * @param readResponsePendingRecords      pending read response records
     * @param readResponsePendingBytes        pending read response bytes
     * @param writeReadRequestPendingRecords  write-read pending records delta
     * @param writeReadRequestPendingBytes    write-read pending bytes delta
     * @param writeTimeoutEvents              write timeout events count
     * @param writeTimeoutEventsPerSec        write timeout events per second
     * @param readTimeoutEvents               read timeout events count
     * @param readTimeoutEventsPerSec         read timeout events per second
     * @param seconds                         total time in seconds
     * @param bytes                           total bytes processed
     * @param records                         total records processed
     * @param recsPerSec                      records per second
     * @param mbPerSec                        MB per second
     * @param avgLatency                      average latency
     * @param minLatency                      minimum latency
     * @param maxLatency                      maximum latency
     * @param invalid                         invalid/negative latency count
     * @param lowerDiscard                    discarded below min latency
     * @param higherDiscard                   discarded above max latency
     * @param slc1                            sliding latency coverage 1
     * @param slc2                            sliding latency coverage 2
     * @param percentileLatencies             percentile latency values
     * @param percentileLatencyCounts         percentile latency counts
     */
    void printTotal(int writers, int maxWriters, int readers, int maxReaders,
                    long writeRequestBytes, double writeRequestMbPerSec, long writeRequestRecords,
                    double writeRequestRecordsPerSec, long readRequestBytes, double readRequestMBPerSec,
                    long readRequestRecords, double readRequestRecordsPerSec, long writeResponsePendingRecords,
                    long writeResponsePendingBytes, long readResponsePendingRecords, long readResponsePendingBytes,
                    long writeReadRequestPendingRecords, long writeReadRequestPendingBytes,
                    long writeTimeoutEvents, double writeTimeoutEventsPerSec,
                    long readTimeoutEvents, double readTimeoutEventsPerSec,
                    double seconds, long bytes, long records, double recsPerSec, double mbPerSec,
                    double avgLatency, long minLatency, long maxLatency, long invalid, long lowerDiscard,
                    long higherDiscard, long slc1, long slc2, long[] percentileLatencies, long[] percentileLatencyCounts);
}
