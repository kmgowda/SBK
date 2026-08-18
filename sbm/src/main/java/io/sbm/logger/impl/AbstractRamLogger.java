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

import io.perl.api.LatencyRecord;
import io.sbk.action.Action;
import io.sbk.logger.impl.CSVLogger;
import io.sbk.params.ParsedOptions;
import io.sbk.system.Printer;
import io.sbm.logger.RamLogger;
import io.time.Time;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Exporter-neutral base logger for SBM.
 *
 * <p>Adds SBM connection counts and request aggregation to SBK's console/CSV logger. Concrete implementations decide
 * whether summaries are exported to Prometheus, the Local Web Console, or another destination.
 */
public abstract class AbstractRamLogger extends CSVLogger implements RamLogger {
    final static String SBM_PREFIX = "SBM";
    private AtomicInteger connections;
    private AtomicInteger maxConnections;

    /**
     * Creates an SBM logger with empty connection counters.
     */
    public AbstractRamLogger() {
        super();
    }

    /**
     * Parse logger arguments and set SBM-specific limits for writer/reader ID dimensions.
     *
     * @param params parsed CLI options
     * @throws IllegalArgumentException if an argument is invalid
     */
    @Override
    public void parseArgs(final ParsedOptions params) throws IllegalArgumentException {
        super.parseArgs(params);
        setMaxReadersIds(getConfiguredMaxRequestIds());
        setMaxWritersIds(getConfiguredMaxRequestIds());
    }

    /**
     * Open the logger and initialize connection counters.
     *
     * @param params      parsed options
     * @param storageName storage under test
     * @param action      selected action
     * @param time        time source
     * @throws IllegalArgumentException on invalid params
     * @throws IOException              on initialization errors
     */
    @Override
    public void open(final ParsedOptions params, final String storageName, Action action, Time time) throws IllegalArgumentException, IOException {
        super.open(params, storageName, action, time);
        this.connections = new AtomicInteger(0);
        this.maxConnections = new AtomicInteger(0);
        Printer.log.info("SBM connection tracking started");
    }


    /**
     * Increment current and maximum connection counters.
     */
    @Override
    public void incrementConnections() {
        connections.incrementAndGet();
        maxConnections.incrementAndGet();
    }

    /**
     * Decrement current connection counter.
     */
    @Override
    public void decrementConnections() {
        connections.decrementAndGet();
    }

    @Override
    public final void recordWriteRequests(int writerId, long startTime, long bytes, long events) {
        if (isWriteRequestsEnabled()) {
            super.recordWriteRequests(writerId % getMaxWriterIDs(), startTime, bytes, events);
        }
    }

    @Override
    public void recordWriteTimeoutEvents(int writerId, long startTime, long timeoutEvents) {
        if (isWriteRequestsEnabled()) {
            super.recordWriteTimeoutEvents(writerId % getMaxWriterIDs(), startTime, timeoutEvents);
        }
    }

    @Override
    public final void recordReadRequests(int readerId, long startTime, long bytes, long events) {
        if (isReadRequestsEnabled()) {
            super.recordReadRequests(readerId % getMaxReaderIDs(), startTime, bytes, events);
        }
    }

    @Override
    public void recordReadTimeoutEvents(int readerId, long startTime, long timeoutEvents) {
        if (isReadRequestsEnabled()) {
            super.recordReadTimeoutEvents(readerId % getMaxReaderIDs(), startTime, timeoutEvents);
        }
    }


    /**
     * Append connection summary to the output builder.
     *
     * @param out            string builder to append to
     * @param connections    current connections
     * @param maxConnections maximum observed connections
     */
    protected final void appendConnections(@NotNull StringBuilder out, int connections, int maxConnections) {
        out.append(String.format(" %5d connections, %5d max connections: ", connections, maxConnections));
    }


    @Override
    public final void reportLatencyRecord(LatencyRecord record) {

    }

    @Override
    public final void reportLatency(long latency, long count) {

    }

    @Override
    public final void recordLatency(long startTime, int events, int bytes, long latency) {

    }

    @Override
    public void print(long reportTime, int writers, int maxWriters, int readers, int maxReaders,
                      long writeRequestBytes, double writeRequestMbPerSec, long writeRequestRecords,
                      double writeRequestRecordsPerSec, long readRequestBytes, double readRequestMbPerSec,
                      long readRequestRecords, double readRequestRecordsPerSec, long writeResponsePendingRecords,
                      long writeResponsePendingBytes, long readResponsePendingRecords, long readResponsePendingBytes,
                      long writeReadRequestPendingRecords, long writeReadRequestPendingBytes,
                      long writeTimeoutEvents, double writeTimeoutEventsPerSec,
                      long readTimeoutEvents, double readTimeoutEventsPerSec,
                      double seconds, long bytes,
                      long records, double recsPerSec, double mbPerSec,
                      double avgLatency, long minLatency, long maxLatency, long invalid, long lowerDiscard,
                      long higherDiscard, long slc1, long slc2, long[] percentileLatencies, long[] percentileLatencyCounts) {
        if (isCsvEnable()) {
            writeToCSV(SBM_PREFIX, REGULAR_PRINT, reportTime, connections.get(), maxConnections.get(),
                    writers, maxWriters, readers, maxReaders, writeRequestBytes, writeRequestMbPerSec,
                    writeRequestRecords, writeRequestRecordsPerSec, readRequestBytes, readRequestMbPerSec,
                    readRequestRecords, readRequestRecordsPerSec, writeResponsePendingRecords,
                    writeResponsePendingBytes, readResponsePendingRecords, readResponsePendingBytes,
                    writeReadRequestPendingRecords, writeReadRequestPendingBytes,
                    writeTimeoutEvents, writeTimeoutEventsPerSec, readTimeoutEvents, readTimeoutEventsPerSec,
                    seconds, bytes, records, recsPerSec, mbPerSec, avgLatency, minLatency, maxLatency,
                    invalid, lowerDiscard, higherDiscard, slc1, slc2, percentileLatencies, percentileLatencyCounts);
        }

        print(reportTime, connections.get(), maxConnections.get(), writers, maxWriters, readers, maxReaders,
                writeRequestBytes, writeRequestMbPerSec, writeRequestRecords, writeRequestRecordsPerSec,
                readRequestBytes, readRequestMbPerSec, readRequestRecords, readRequestRecordsPerSec,
                writeResponsePendingRecords, writeResponsePendingBytes, readResponsePendingRecords,
                readResponsePendingBytes, writeReadRequestPendingRecords, writeReadRequestPendingBytes,
                writeTimeoutEvents, writeTimeoutEventsPerSec, readTimeoutEvents, readTimeoutEventsPerSec,
                seconds, bytes, records, recsPerSec, mbPerSec, avgLatency, minLatency, maxLatency,
                invalid, lowerDiscard, higherDiscard, slc1, slc2, percentileLatencies, percentileLatencyCounts);
    }

    /**
     * Prints one SBM reporting window with connection and workload metrics.
     *
     * @param reportTime report timestamp
     * @param connections active connections
     * @param maxConnections maximum connections
     * @param writers active writers
     * @param maxWriters maximum writers
     * @param readers active readers
     * @param maxReaders maximum readers
     * @param writeRequestBytes write-request bytes
     * @param writeRequestMbPerSec write-request throughput
     * @param writeRequestRecords write-request records
     * @param writeRequestRecordsPerSec write requests per second
     * @param readRequestBytes read-request bytes
     * @param readRequestMbPerSec read-request throughput
     * @param readRequestRecords read-request records
     * @param readRequestsRecordsPerSec read requests per second
     * @param writeResponsePendingRecords pending write-response records
     * @param writeResponsePendingBytes pending write-response bytes
     * @param readResponsePendingRecords pending read-response records
     * @param readResponsePendingBytes pending read-response bytes
     * @param writeReadRequestPendingRecords pending combined request records
     * @param writeReadRequestPendingBytes pending combined request bytes
     * @param writeTimeoutEvents write timeout events
     * @param writeTimeoutEventsPerSec write timeout events per second
     * @param readTimeoutEvents read timeout events
     * @param readTimeoutEventsPerSec read timeout events per second
     * @param seconds elapsed seconds
     * @param bytes completed bytes
     * @param records completed records
     * @param recsPerSec completed records per second
     * @param mbPerSec completed megabytes per second
     * @param avgLatency average latency
     * @param minLatency minimum latency
     * @param maxLatency maximum latency
     * @param invalid invalid latency count
     * @param lowerDiscard latencies below the configured range
     * @param higherDiscard latencies above the configured range
     * @param slc1 first SLC count
     * @param slc2 second SLC count
     * @param percentileLatencies percentile latency values
     * @param percentileLatencyCounts percentile sample counts
     */
    public abstract void print(long reportTime, int connections, int maxConnections, int writers, int maxWriters,
                               int readers, int maxReaders, long writeRequestBytes, double writeRequestMbPerSec,
                               long writeRequestRecords, double writeRequestRecordsPerSec, long readRequestBytes,
                               double readRequestMbPerSec, long readRequestRecords, double readRequestsRecordsPerSec,
                               long writeResponsePendingRecords, long writeResponsePendingBytes,
                               long readResponsePendingRecords, long readResponsePendingBytes,
                               long writeReadRequestPendingRecords, long writeReadRequestPendingBytes,
                               long writeTimeoutEvents, double writeTimeoutEventsPerSec, long readTimeoutEvents,
                               double readTimeoutEventsPerSec, double seconds, long bytes, long records,
                               double recsPerSec, double mbPerSec, double avgLatency, long minLatency,
                               long maxLatency, long invalid, long lowerDiscard, long higherDiscard,
                               long slc1, long slc2, long[] percentileLatencies, long[] percentileLatencyCounts);

    @Override
    public void printTotal(long reportTime, int writers, int maxWriters, int readers, int maxReaders,
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
        if (isCsvEnable()) {
            writeToCSV(SBM_PREFIX, TOTAL_PRINT, reportTime, connections.get(), maxConnections.get(),
                    writers, maxWriters, readers, maxReaders,
                    writeRequestBytes, writeRequestMbPerSec, writeRequestRecords, writeRequestRecordsPerSec,
                    readRequestBytes, readRequestMbPerSec, readRequestRecords, readRequestRecordsPerSec,
                    writeResponsePendingRecords, writeResponsePendingBytes, readResponsePendingRecords,
                    readResponsePendingBytes, writeReadRequestPendingRecords, writeReadRequestPendingBytes,
                    writeTimeoutEvents, writeTimeoutEventsPerSec, readTimeoutEvents, readTimeoutEventsPerSec,
                    seconds, bytes, records, recsPerSec, mbPerSec, avgLatency, minLatency, maxLatency,
                    invalid, lowerDiscard, higherDiscard, slc1, slc2, percentileLatencies, percentileLatencyCounts);
        }

        printTotal(reportTime, connections.get(), maxConnections.get(), writers, maxWriters, readers, maxReaders,
                writeRequestBytes, writeRequestMbPerSec, writeRequestRecords, writeRequestRecordsPerSec,
                readRequestBytes, readRequestMbPerSec, readRequestRecords, readRequestRecordsPerSec,
                writeResponsePendingRecords, writeResponsePendingBytes, readResponsePendingRecords,
                readResponsePendingBytes, writeReadRequestPendingRecords, writeReadRequestPendingBytes,
                writeTimeoutEvents, writeTimeoutEventsPerSec, readTimeoutEvents, readTimeoutEventsPerSec,
                seconds, bytes, records, recsPerSec, mbPerSec, avgLatency, minLatency, maxLatency,
                invalid, lowerDiscard, higherDiscard, slc1, slc2, percentileLatencies, percentileLatencyCounts);
    }

    /**
     * Prints cumulative SBM results with connection and workload metrics.
     *
     * @param reportTime report timestamp
     * @param connections active connections
     * @param maxConnections maximum connections
     * @param writers active writers
     * @param maxWriters maximum writers
     * @param readers active readers
     * @param maxReaders maximum readers
     * @param writeRequestBytes write-request bytes
     * @param writeRequestMbPerSec write-request throughput
     * @param writeRequestRecords write-request records
     * @param writeRequestRecordsPerSec write requests per second
     * @param readRequestBytes read-request bytes
     * @param readRequestMbPerSec read-request throughput
     * @param readRequestRecords read-request records
     * @param readRequestRecordsPerSec read requests per second
     * @param writeResponsePendingRecords pending write-response records
     * @param writeResponsePendingBytes pending write-response bytes
     * @param readResponsePendingRecords pending read-response records
     * @param readResponsePendingBytes pending read-response bytes
     * @param writeReadRequestPendingRecords pending combined request records
     * @param writeReadRequestPendingBytes pending combined request bytes
     * @param writeTimeoutEvents write timeout events
     * @param writeTimeoutEventsPerSec write timeout events per second
     * @param readTimeoutEvents read timeout events
     * @param readTimeoutEventsPerSec read timeout events per second
     * @param seconds elapsed seconds
     * @param bytes completed bytes
     * @param records completed records
     * @param recsPerSec completed records per second
     * @param mbPerSec completed megabytes per second
     * @param avgLatency average latency
     * @param minLatency minimum latency
     * @param maxLatency maximum latency
     * @param invalid invalid latency count
     * @param lowerDiscard latencies below the configured range
     * @param higherDiscard latencies above the configured range
     * @param slc1 first SLC count
     * @param slc2 second SLC count
     * @param percentileLatencies percentile latency values
     * @param percentileLatencyCounts percentile sample counts
     */
    public abstract void printTotal(long reportTime, int connections, int maxConnections, int writers, int maxWriters,
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
                                    long slc1, long slc2, long[] percentileLatencies, long[] percentileLatencyCounts);

}
