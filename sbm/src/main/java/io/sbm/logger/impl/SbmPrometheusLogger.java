/**
 * Copyright (c) KMG. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 */
package io.sbm.logger.impl;

/**
 * Class for Recoding/Printing benchmark results on micrometer Composite Meter Registry.
 */
public class SbmPrometheusLogger extends AbstractRamLogger {

    @Override
    public void print(int connections, int maxConnections, int writers, int maxWriters, int readers, int maxReaders,
                      long writeRequestBytes, double writeRequestMbPerSec, long writeRequestRecords,
                      double writeRequestRecordsPerSec, long readRequestBytes, double readRequestMbPerSec,
                      long readRequestRecords, double readRequestsRecordsPerSec, long writeResponsePendingRecords,
                      long writeResponsePendingBytes, long readResponsePendingRecords, long readResponsePendingBytes,
                      long writeReadRequestPendingRecords, long writeReadRequestPendingBytes, long writeTimeoutEvents,
                      double writeTimeoutEventsPerSec, long readTimeoutEvents, double readTimeoutEventsPerSec,
                      double seconds, long bytes, long records, double recsPerSec, double mbPerSec, double avgLatency,
                      long minLatency, long maxLatency, long invalid, long lowerDiscard, long higherDiscard,
                      long slc1, long slc2, long[] percentileValues) {
        StringBuilder out = new StringBuilder(SBM_PREFIX);
        appendConnections(out, connections, maxConnections);
        out.append(getPrefix());
        appendResultString(out, writers, maxWriters, readers, maxReaders,
                writeRequestBytes, writeRequestMbPerSec, writeRequestRecords, writeRequestRecordsPerSec,
                readRequestBytes, readRequestMbPerSec, readRequestRecords, readRequestsRecordsPerSec,
                writeResponsePendingRecords, writeResponsePendingBytes, readResponsePendingRecords,
                readResponsePendingBytes, writeReadRequestPendingRecords, writeReadRequestPendingBytes,
                writeTimeoutEvents, writeTimeoutEventsPerSec, readTimeoutEvents, readTimeoutEventsPerSec,
                seconds, bytes, records, recsPerSec, mbPerSec, avgLatency, minLatency, maxLatency,
                invalid, lowerDiscard, higherDiscard, slc1, slc2, percentileValues);
        System.out.println(out);

    }

    @Override
    public void printTotal(int connections, int maxConnections, int writers, int maxWriters,
                           int readers, int maxReaders, long writeRequestBytes, double writeRequestMbPerSec,
                           long writeRequestRecords, double writeRequestRecordsPerSec, long readRequestBytes,
                           double readRequestMbPerSec, long readRequestRecords, double readRequestRecordsPerSec,
                           long writeResponsePendingRecords, long writeResponsePendingBytes,
                           long readResponsePendingRecords, long readResponsePendingBytes,
                           long writeReadRequestPendingRecords, long writeReadRequestPendingBytes,
                           long writeTimeoutEvents, double writeTimeoutEventsPerSec, long readTimeoutEvents,
                           double readTimeoutEventsPerSec, double seconds, long bytes, long records,
                           double recsPerSec, double mbPerSec, double avgLatency, long minLatency,
                           long maxLatency, long invalid, long lowerDiscard, long higherDiscard,
                           long slc1, long slc2, long[] percentileValues) {
        StringBuilder out = new StringBuilder("Total : " + SBM_PREFIX);
        appendConnections(out, connections, maxConnections);
        out.append(getPrefix());
        appendResultString(out, writers, maxWriters, readers, maxReaders,
                writeRequestBytes, writeRequestMbPerSec, writeRequestRecords, writeRequestRecordsPerSec,
                readRequestBytes, readRequestMbPerSec, readRequestRecords, readRequestRecordsPerSec,
                writeResponsePendingRecords, writeResponsePendingBytes, readResponsePendingRecords,
                readResponsePendingBytes, writeReadRequestPendingRecords, writeReadRequestPendingBytes,
                writeTimeoutEvents, writeTimeoutEventsPerSec, readTimeoutEvents, readTimeoutEventsPerSec,
                seconds, bytes, records, recsPerSec, mbPerSec, avgLatency, minLatency, maxLatency,
                invalid, lowerDiscard, higherDiscard, slc1, slc2, percentileValues);
        System.out.println(out);
    }
}
