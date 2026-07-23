/**
 * Copyright (c) KMG. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 */
package io.sbk.dashboard;

/**
 * Immutable benchmark summary sent from an SBK logger to the dashboard.
 *
 * @param runId       benchmark run identifier
 * @param timestamp   snapshot time in epoch milliseconds
 * @param workers     active and maximum worker/connection counts
 * @param requests    request, pending-operation, and timeout statistics
 * @param performance completed data and throughput statistics
 * @param latency     latency distribution summary
 */
public record DashboardSnapshot(String runId, long timestamp, WorkerMetrics workers,
                                RequestMetrics requests, PerformanceMetrics performance,
                                LatencyMetrics latency) {

    /**
     * Worker and distributed-connection counts.
     *
     * @param writers        active writers
     * @param maxWriters     maximum writers
     * @param readers        active readers
     * @param maxReaders     maximum readers
     * @param connections    active SBM connections
     * @param maxConnections maximum SBM connections
     */
    public record WorkerMetrics(int writers, int maxWriters, int readers, int maxReaders,
                                int connections, int maxConnections) {
    }

    /**
     * Request-side statistics for a reporting window.
     *
     * @param writeBytes              write-request bytes
     * @param writeRecords            write-request records
     * @param writeMbPerSec           write-request throughput
     * @param writeRecordsPerSec      write-request rate
     * @param readBytes               read-request bytes
     * @param readRecords             read-request records
     * @param readMbPerSec            read-request throughput
     * @param readRecordsPerSec       read-request rate
     * @param pendingWriteRecords     pending write-response records
     * @param pendingWriteBytes       pending write-response bytes
     * @param pendingReadRecords      pending read-response records
     * @param pendingReadBytes        pending read-response bytes
     * @param pendingCombinedRecords  pending combined request records
     * @param pendingCombinedBytes    pending combined request bytes
     * @param writeTimeouts           write timeout events
     * @param writeTimeoutsPerSec     write timeout event rate
     * @param readTimeouts            read timeout events
     * @param readTimeoutsPerSec      read timeout event rate
     */
    public record RequestMetrics(long writeBytes, long writeRecords, double writeMbPerSec,
                                 double writeRecordsPerSec, long readBytes, long readRecords,
                                 double readMbPerSec, double readRecordsPerSec,
                                 long pendingWriteRecords, long pendingWriteBytes,
                                 long pendingReadRecords, long pendingReadBytes,
                                 long pendingCombinedRecords, long pendingCombinedBytes,
                                 long writeTimeouts, double writeTimeoutsPerSec,
                                 long readTimeouts, double readTimeoutsPerSec) {
    }

    /**
     * Completed-operation statistics.
     *
     * @param seconds       elapsed seconds represented by this snapshot
     * @param bytes         completed bytes
     * @param records       completed records
     * @param recordsPerSec completed record rate
     * @param mbPerSec      completed throughput
     */
    public record PerformanceMetrics(double seconds, long bytes, long records,
                                     double recordsPerSec, double mbPerSec) {
    }

    /**
     * Latency distribution summary.
     *
     * @param average          average latency
     * @param minimum          minimum latency
     * @param maximum          maximum latency
     * @param invalid          invalid latency count
     * @param lowerDiscard     samples below the configured range
     * @param higherDiscard    samples above the configured range
     * @param slc1             first SLC count
     * @param slc2             second SLC count
     * @param percentileLabels configured percentile labels
     * @param percentiles      corresponding percentile latency values
     * @param percentileCounts corresponding percentile sample counts
     */
    public record LatencyMetrics(double average, long minimum, long maximum, long invalid,
                                 long lowerDiscard, long higherDiscard, long slc1, long slc2,
                                 double[] percentileLabels, long[] percentiles,
                                 long[] percentileCounts) {
        /**
         * Protects the immutable snapshot from later mutation of source arrays.
         */
        public LatencyMetrics {
            percentileLabels = percentileLabels.clone();
            percentiles = percentiles.clone();
            percentileCounts = percentileCounts.clone();
        }

        /**
         * Returns a defensive copy of the configured percentile labels.
         *
         * @return percentile labels
         */
        @Override
        public double[] percentileLabels() {
            return percentileLabels.clone();
        }

        /**
         * Returns a defensive copy of the measured percentile latencies.
         *
         * @return percentile latency values
         */
        @Override
        public long[] percentiles() {
            return percentiles.clone();
        }

        /**
         * Returns a defensive copy of the percentile sample counts.
         *
         * @return percentile sample counts
         */
        @Override
        public long[] percentileCounts() {
            return percentileCounts.clone();
        }
    }
}
