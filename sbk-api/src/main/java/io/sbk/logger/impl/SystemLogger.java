/**
 * Copyright (c) KMG. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.sbk.logger.impl;

import javax.annotation.concurrent.NotThreadSafe;


/**
 * Logger implementation that prints periodic and total benchmark results to {@code System.out}.
 *
 * <p>This implementation relies on the aggregation utilities provided by
 * {@link AbstractRWLogger} and formats the final output line using
 * {@link AbstractRWLogger#appendResultString(StringBuilder, int, int, int, int, long, double, long, double, long, double, long, double, long, long, long, long, long, long, long, double, long, double, double, long, long, double, double, double, long, long, long, long, long, long, long, long[], long[])} 
 * <p>Note: Marked {@link NotThreadSafe} because printing occurs from a single
 * scheduling context while internal counters are updated by the base class.
 */
@NotThreadSafe
public class SystemLogger extends AbstractRWLogger {

    public SystemLogger() {
        super();
    }

    /**
     * Print the periodic results to {@code System.out} at the end of each interval.
     *
     * @param writers                        number of active writers
     * @param maxWriters                     maximum writers seen
     * @param readers                        number of active readers
     * @param maxReaders                     maximum readers seen
     * @param writeRequestBytes              write request bytes in this interval
     * @param writeRequestMbPerSec           write request throughput in MB/sec
     * @param writeRequestRecords            write request count
     * @param writeRequestRecordsPerSec      write requests per second
     * @param readRequestBytes               read request bytes in this interval
     * @param readRequestMbPerSec            read request throughput in MB/sec
     * @param readRequestRecords             read request count
     * @param readRequestRecordsPerSec       read requests per second
     * @param writeResponsePendingRecords    pending write response records
     * @param writeResponsePendingBytes      pending write response bytes
     * @param readResponsePendingRecords     pending read response records
     * @param readResponsePendingBytes       pending read response bytes
     * @param writeReadRequestPendingRecords write-read pending records delta
     * @param writeReadRequestPendingBytes   write-read pending bytes delta
     * @param writeTimeoutEvents             write timeout events count
     * @param writeTimeoutEventsPerSec       write timeout events per second
     * @param readTimeoutEvents              read timeout events count
     * @param readTimeoutEventsPerSec        read timeout events per second
     * @param seconds                        reporting interval seconds
     * @param bytes                          total bytes processed in interval
     * @param records                        total records processed in interval
     * @param recsPerSec                     records per second
     * @param mbPerSec                       MB per second
     * @param avgLatency                     average latency
     * @param minLatency                     minimum latency
     * @param maxLatency                     maximum latency
     * @param invalid                        invalid/negative latency count
     * @param lowerDiscard                   discarded below min latency
     * @param higherDiscard                  discarded above max latency
     * @param slc1                           sliding latency coverage 1
     * @param slc2                           sliding latency coverage 2
     * @param percentileLatencies            percentile latency values
     * @param percentileLatencyCounts        percentile latency counts
     */
    @Override
    public void print(int writers, int maxWriters, int readers, int maxReaders,
                      long writeRequestBytes, double writeRequestMbPerSec, long writeRequestRecords,
                      double writeRequestRecordsPerSec, long readRequestBytes, double readRequestMbPerSec,
                      long readRequestRecords, double readRequestRecordsPerSec, long writeResponsePendingRecords,
                      long writeResponsePendingBytes, long readResponsePendingRecords, long readResponsePendingBytes,
                      long writeReadRequestPendingRecords, long writeReadRequestPendingBytes,
                      long writeTimeoutEvents, double writeTimeoutEventsPerSec,
                      long readTimeoutEvents, double readTimeoutEventsPerSec,
                      double seconds, long bytes, long records, double recsPerSec, double mbPerSec,
                      double avgLatency, long minLatency, long maxLatency, long invalid, long lowerDiscard,
                      long higherDiscard, long slc1, long slc2, long[] percentileLatencies,
                      long[] percentileLatencyCounts) {
        StringBuilder out = new StringBuilder(getPrefix());
        appendResultString(out, writers, maxWriters, readers, maxReaders,
                writeRequestBytes, writeRequestMbPerSec, writeRequestRecords, writeRequestRecordsPerSec,
                readRequestBytes, readRequestMbPerSec, readRequestRecords, readRequestRecordsPerSec,
                writeResponsePendingRecords, writeResponsePendingBytes, readResponsePendingRecords,
                readResponsePendingBytes, writeReadRequestPendingRecords, writeReadRequestPendingBytes,
                writeTimeoutEvents, writeTimeoutEventsPerSec, readTimeoutEvents, readTimeoutEventsPerSec,
                seconds, bytes, records, recsPerSec, mbPerSec, avgLatency, minLatency, maxLatency, invalid, lowerDiscard,
                higherDiscard, slc1, slc2, percentileLatencies, percentileLatencyCounts);
        System.out.println(out);
    }

    /**
     * Print the final accumulated totals to {@code System.out} at the end of the run.
     *
     * @param writers                        number of active writers
     * @param maxWriters                     maximum writers seen
     * @param readers                        number of active readers
     * @param maxReaders                     maximum readers seen
     * @param writeRequestBytes              write request bytes total
     * @param writeRequestMbPerSec           write request throughput in MB/sec
     * @param writeRequestRecords            write request count total
     * @param writeRequestRecordsPerSec      write requests per second
     * @param readRequestBytes               read request bytes total
     * @param readRequestsMbPerSec           read request throughput in MB/sec
     * @param readRequestRecords             read request count total
     * @param readRequestRecordsPerSec       read requests per second
     * @param writeResponsePendingRecords    pending write response records
     * @param writeResponsePendingBytes      pending write response bytes
     * @param readResponsePendingRecords     pending read response records
     * @param readResponsePendingBytes       pending read response bytes
     * @param writeReadRequestPendingRecords write-read pending records delta
     * @param writeReadRequestPendingBytes   write-read pending bytes delta
     * @param writeTimeoutEvents             write timeout events count
     * @param writeTimeoutEventsPerSec       write timeout events per second
     * @param readTimeoutEvents              read timeout events count
     * @param readTimeoutEventsPerSec        read timeout events per second
     * @param seconds                        reporting seconds
     * @param bytes                          total bytes processed
     * @param records                        total records processed
     * @param recsPerSec                     records per second
     * @param mbPerSec                       MB per second
     * @param avgLatency                     average latency
     * @param minLatency                     minimum latency
     * @param maxLatency                     maximum latency
     * @param invalid                        invalid/negative latency count
     * @param lowerDiscard                   discarded below min latency
     * @param higherDiscard                  discarded above max latency
     * @param slc1                           sliding latency coverage 1
     * @param slc2                           sliding latency coverage 2
     * @param percentileLatencies            percentile latency values
     * @param percentileLatencyCounts        percentile latency counts
     */
    public void printTotal(int writers, int maxWriters, int readers, int maxReaders,
                           long writeRequestBytes, double writeRequestMbPerSec, long writeRequestRecords,
                           double writeRequestRecordsPerSec, long readRequestBytes, double readRequestsMbPerSec,
                           long readRequestRecords, double readRequestRecordsPerSec, long writeResponsePendingRecords,
                           long writeResponsePendingBytes, long readResponsePendingRecords,
                           long readResponsePendingBytes, long writeReadRequestPendingRecords, long writeReadRequestPendingBytes,
                           long writeTimeoutEvents, double writeTimeoutEventsPerSec,
                           long readTimeoutEvents, double readTimeoutEventsPerSec,
                           double seconds, long bytes, long records, double recsPerSec, double mbPerSec,
                           double avgLatency, long minLatency, long maxLatency, long invalid, long lowerDiscard,
                           long higherDiscard, long slc1, long slc2, long[] percentileLatencies, long[] percentileLatencyCounts) {
        StringBuilder out = new StringBuilder("Total " + getPrefix());
        appendResultString(out, writers, maxWriters, readers, maxReaders,
                writeRequestBytes, writeRequestMbPerSec, writeRequestRecords, writeRequestRecordsPerSec,
                readRequestBytes, readRequestsMbPerSec, readRequestRecords, readRequestRecordsPerSec,
                writeResponsePendingRecords, writeResponsePendingBytes, readResponsePendingRecords,
                readResponsePendingBytes, writeReadRequestPendingRecords, writeReadRequestPendingBytes,
                writeTimeoutEvents, writeTimeoutEventsPerSec, readTimeoutEvents, readTimeoutEventsPerSec,
                seconds, bytes, records, recsPerSec, mbPerSec, avgLatency, minLatency, maxLatency, invalid, lowerDiscard,
                higherDiscard, slc1, slc2, percentileLatencies, percentileLatencyCounts);
        System.out.println(out);
    }

}
