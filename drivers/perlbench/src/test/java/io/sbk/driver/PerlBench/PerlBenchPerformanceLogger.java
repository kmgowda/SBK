/**
 * Copyright (c) KMG. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 */
package io.sbk.driver.PerlBench;

import io.sbk.logger.impl.SystemLogger;

import java.util.concurrent.atomic.AtomicReference;

/**
 * Captures the final result of an SBK-level PerlBench performance run.
 */
public final class PerlBenchPerformanceLogger extends SystemLogger {
    private static final AtomicReference<Result> RESULT =
            new AtomicReference<>();

    /**
     * Clear the result before starting another benchmark run.
     */
    public static void reset() {
        RESULT.set(null);
    }

    /**
     * Return the largest accumulated result reported by PerL.
     *
     * @return captured result, or {@code null} before a total is reported
     */
    public static Result getResult() {
        return RESULT.get();
    }

    /**
     * Capture the accumulated SBK result.
     *
     * @param reportTime report time in milliseconds
     * @param writers active writers
     * @param maxWriters maximum writers
     * @param readers active readers
     * @param maxReaders maximum readers
     * @param writeRequestBytes write-request bytes
     * @param writeRequestMbPerSec write-request MB/s
     * @param writeRequestRecords write-request records
     * @param writeRequestRecordsPerSec write requests/s
     * @param readRequestBytes read-request bytes
     * @param readRequestsMbPerSec read-request MB/s
     * @param readRequestRecords read-request records
     * @param readRequestRecordsPerSec read requests/s
     * @param writeResponsePendingRecords pending write records
     * @param writeResponsePendingBytes pending write bytes
     * @param readResponsePendingRecords pending read records
     * @param readResponsePendingBytes pending read bytes
     * @param writeReadRequestPendingRecords write-read pending records
     * @param writeReadRequestPendingBytes write-read pending bytes
     * @param writeTimeoutEvents write timeout events
     * @param writeTimeoutEventsPerSec write timeout events/s
     * @param readTimeoutEvents read timeout events
     * @param readTimeoutEventsPerSec read timeout events/s
     * @param seconds elapsed seconds
     * @param bytes completed bytes
     * @param records completed records
     * @param recsPerSec completed records/s
     * @param mbPerSec completed MB/s
     * @param avgLatency average operation latency
     * @param minLatency minimum operation latency
     * @param maxLatency maximum operation latency
     * @param invalid invalid latencies
     * @param lowerDiscard low discarded latencies
     * @param higherDiscard high discarded latencies
     * @param slc1 first sliding latency count
     * @param slc2 second sliding latency count
     * @param percentileLatencies percentile latency values
     * @param percentileLatencyCounts percentile latency counts
     */
    @Override
    public void printTotal(long reportTime, int writers, int maxWriters,
                           int readers, int maxReaders,
                           long writeRequestBytes,
                           double writeRequestMbPerSec,
                           long writeRequestRecords,
                           double writeRequestRecordsPerSec,
                           long readRequestBytes,
                           double readRequestsMbPerSec,
                           long readRequestRecords,
                           double readRequestRecordsPerSec,
                           long writeResponsePendingRecords,
                           long writeResponsePendingBytes,
                           long readResponsePendingRecords,
                           long readResponsePendingBytes,
                           long writeReadRequestPendingRecords,
                           long writeReadRequestPendingBytes,
                           long writeTimeoutEvents,
                           double writeTimeoutEventsPerSec,
                           long readTimeoutEvents,
                           double readTimeoutEventsPerSec,
                           double seconds, long bytes, long records,
                           double recsPerSec, double mbPerSec,
                           double avgLatency, long minLatency,
                           long maxLatency, long invalid,
                           long lowerDiscard, long higherDiscard,
                           long slc1, long slc2,
                           long[] percentileLatencies,
                           long[] percentileLatencyCounts) {
        RESULT.accumulateAndGet(
                new Result(records, recsPerSec, avgLatency, invalid),
                (current, update) -> current == null
                        || update.records() >= current.records()
                        ? update : current);
    }

    /**
     * Minimal final result required by the functional performance test.
     *
     * @param records completed record count
     * @param recordsPerSecond completed records per second
     * @param averageLatency average measured operation latency
     * @param invalidLatencies invalid latency count
     */
    public record Result(long records, double recordsPerSecond,
                         double averageLatency, long invalidLatencies) {
    }
}
