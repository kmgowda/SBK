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

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.perl.api.LatencyRecord;
import io.sbk.action.Action;
import io.sbk.config.Config;
import io.sbm.logger.RamLogger;
import io.sbk.logger.impl.PrometheusLogger;
import io.sbk.logger.impl.PrometheusRWMetricsServer;
import io.sbk.params.ParsedOptions;
import io.sbk.system.Printer;
import io.time.Time;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.atomic.AtomicInteger;


/**
 * Class for Recoding/Printing benchmark results on micrometer Composite Meter Registry.
 */
public class SbmPrometheusLogger extends PrometheusLogger implements RamLogger {
    final static String CONFIG_FILE = "sbm-metrics.properties";
    final static String SBM_PREFIX = "SBM";
    final static int MAX_REQUEST_RW_IDS = 10;
    private AtomicInteger connections;
    private AtomicInteger maxConnections;
    private SbmMetricsPrometheusServer prometheusServer;

    /**
     * Constructor RamPrometheusLogger calling its super calls and initializing {@link #prometheusServer} = null.
     */
    public SbmPrometheusLogger() {
        super();
        prometheusServer = null;
    }

    public InputStream getMetricsConfigStream() {
        return SbmPrometheusLogger.class.getClassLoader().getResourceAsStream(CONFIG_FILE);
    }

    @Override
    @SuppressFBWarnings("EI_EXPOSE_REP")
    public @Nonnull PrometheusRWMetricsServer getPrometheusRWMetricsServer() throws IOException {
        if (prometheusServer == null) {
            prometheusServer = new SbmMetricsPrometheusServer(Config.NAME, getAction().name(), getStorageName(),
                    getPercentiles(), getTime(), metricsConfig);
        }
        return prometheusServer;
    }

    @Override
    public void parseArgs(final ParsedOptions params) throws IllegalArgumentException {
        super.parseArgs(params);
        setMaxReadersIds(MAX_REQUEST_RW_IDS);
        setMaxWritersIds(MAX_REQUEST_RW_IDS);
    }

    @Override
    public void open(final ParsedOptions params, final String storageName, Action action, Time time) throws IllegalArgumentException, IOException {
        super.open(params, storageName, action, time);
        this.connections = new AtomicInteger(0);
        this.maxConnections = new AtomicInteger(0);
        Printer.log.info("SBK Connections PrometheusLogger Started");
    }


    @Override
    public void incrementConnections() {
        connections.incrementAndGet();
        maxConnections.incrementAndGet();
        if (prometheusServer != null) {
            prometheusServer.incrementConnections();
        }
    }

    @Override
    public void decrementConnections() {
        connections.decrementAndGet();
        if (prometheusServer != null) {
            prometheusServer.decrementConnections();
        }
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

    public void print(String ramPrefix, String prefix, int writers, int maxWriters, int readers, int maxReaders,
                      long writeRequestBytes, double writeRequestsMbPerSec, long writeRequestRecords,
                      double writeRequestsRecordsPerSec, long readRequestBytes, double readRequestsMbPerSec,
                      long readRequestRecords, double readRequestsRecordsPerSec, long writeResponsePendingRecords,
                      long writeResponsePendingBytes, long readResponsePendingRecords, long readResponsePendingBytes,
                      long writeReadPendingRecords, long writeReadPendingBytes,
                      long writeTimeoutEvents, double writeTimeoutEventsPerSec,
                      long readTimeoutEvents, double readTimeoutEventsPerSec,
                      double seconds, long bytes,
                      long records, double recsPerSec, double mbPerSec,
                      double avgLatency, long minLatency, long maxLatency, long invalid, long lowerDiscard,
                      long higherDiscard, long slc1, long slc2, long[] percentileValues) {
        StringBuilder out = new StringBuilder(ramPrefix);
        out.append(String.format(" %5d connections, %5d max connections: ", connections.get(), maxConnections.get()));
        out.append(prefix);
        appendResultString(out, writers, maxWriters, readers, maxReaders,
                writeRequestBytes, writeRequestsMbPerSec, writeRequestRecords, writeRequestsRecordsPerSec,
                readRequestBytes, readRequestsMbPerSec, readRequestRecords, readRequestsRecordsPerSec,
                writeResponsePendingRecords, writeResponsePendingBytes, readResponsePendingRecords,
                readResponsePendingBytes, writeReadPendingRecords, writeReadPendingBytes,
                writeTimeoutEvents, writeTimeoutEventsPerSec, readTimeoutEvents, readTimeoutEventsPerSec,
                seconds, bytes, records, recsPerSec, mbPerSec, avgLatency, minLatency, maxLatency,
                invalid, lowerDiscard, higherDiscard, slc1, slc2, percentileValues);
        System.out.println(out);

    }

    @Override
    public void print(int writers, int maxWriters, int readers, int maxReaders,
                      long writeRequestBytes, double writeRequestMbPerSec, long writeRequestRecords,
                      double writeRequestRecordsPerSec, long readRequestBytes, double readRequestMbPerSec,
                      long readRequestRecords, double readRequestsRecordsPerSec, long writeResponsePendingRecords,
                      long writeResponsePendingBytes, long readResponsePendingRecords, long readResponsePendingBytes,
                      long writeReadRequestPendingRecords, long writeReadRequestPendingBytes,
                      long writeTimeoutEvents, double writeTimeoutEventsPerSec,
                      long readTimeoutEvents, double readTimeoutEventsPerSec,
                      double seconds, long bytes,
                      long records, double recsPerSec, double mbPerSec,
                      double avgLatency, long minLatency, long maxLatency, long invalid, long lowerDiscard,
                      long higherDiscard, long slc1, long slc2, long[] percentileValues) {
        print(SBM_PREFIX, getHeader(), writers, maxWriters, readers, maxReaders,
                writeRequestBytes, writeRequestMbPerSec, writeRequestRecords, writeRequestRecordsPerSec,
                readRequestBytes, readRequestMbPerSec, readRequestRecords, readRequestsRecordsPerSec,
                writeResponsePendingRecords, writeResponsePendingBytes, readResponsePendingRecords,
                readResponsePendingBytes, writeReadRequestPendingRecords, writeReadRequestPendingBytes,
                writeTimeoutEvents, writeTimeoutEventsPerSec, readTimeoutEvents, readTimeoutEventsPerSec,
                seconds, bytes, records, recsPerSec, mbPerSec, avgLatency, minLatency, maxLatency,
                invalid, lowerDiscard, higherDiscard, slc1, slc2, percentileValues);

        if (prometheusServer != null) {
            prometheusServer.print(writers, maxWriters, readers, maxReaders,
                    writeRequestBytes, writeRequestMbPerSec, writeRequestRecords, writeRequestRecordsPerSec,
                    readRequestBytes, readRequestMbPerSec, readRequestRecords, readRequestsRecordsPerSec,
                    writeResponsePendingRecords, writeResponsePendingBytes, readResponsePendingRecords,
                    readResponsePendingBytes, writeReadRequestPendingRecords, writeReadRequestPendingBytes,
                    writeTimeoutEvents, writeTimeoutEventsPerSec, readTimeoutEvents, readTimeoutEventsPerSec,
                    seconds, bytes, records, recsPerSec, mbPerSec, avgLatency, minLatency, maxLatency,
                    invalid, lowerDiscard, higherDiscard, slc1, slc2, percentileValues);
        }
        if (csvEnable) {
            writeToCSV(SBM_PREFIX, REGULAR_PRINT, connections.get(), maxConnections.get(),
                    writers, maxWriters, readers, maxReaders,
                    writeRequestBytes, writeRequestMbPerSec, writeRequestRecords, writeRequestRecordsPerSec,
                    readRequestBytes, readRequestMbPerSec, readRequestRecords, readRequestsRecordsPerSec,
                    writeResponsePendingRecords, writeResponsePendingBytes, readResponsePendingRecords,
                    readResponsePendingBytes, writeReadRequestPendingRecords, writeReadRequestPendingBytes,
                    writeTimeoutEvents, writeTimeoutEventsPerSec, readTimeoutEvents, readTimeoutEventsPerSec,
                    seconds, bytes, records, recsPerSec, mbPerSec, avgLatency, minLatency, maxLatency,
                    invalid, lowerDiscard, higherDiscard, slc1, slc2, percentileValues);
        }
    }

    @Override
    public void printTotal(int writers, int maxWriters, int readers, int maxReaders,
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
                           long higherDiscard, long slc1, long slc2, long[] percentileValues) {
        print("Total : " + SBM_PREFIX, getHeader(), writers, maxWriters, readers, maxReaders,
                writeRequestBytes, writeRequestMbPerSec, writeRequestRecords, writeRequestRecordsPerSec,
                readRequestBytes, readRequestMbPerSec, readRequestRecords, readRequestRecordsPerSec,
                writeResponsePendingRecords, writeResponsePendingBytes, readResponsePendingRecords,
                readResponsePendingBytes, writeReadRequestPendingRecords, writeReadRequestPendingBytes,
                writeTimeoutEvents, writeTimeoutEventsPerSec, readTimeoutEvents, readTimeoutEventsPerSec,
                seconds, bytes, records, recsPerSec, mbPerSec, avgLatency, minLatency, maxLatency,
                invalid, lowerDiscard, higherDiscard, slc1, slc2, percentileValues);

        if (csvEnable) {
            writeToCSV(SBM_PREFIX, TOTAL_PRINT, connections.get(), maxConnections.get(),
                    writers, maxWriters, readers, maxReaders,
                    writeRequestBytes, writeRequestMbPerSec, writeRequestRecords, writeRequestRecordsPerSec,
                    readRequestBytes, readRequestMbPerSec, readRequestRecords, readRequestRecordsPerSec,
                    writeResponsePendingRecords, writeResponsePendingBytes, readResponsePendingRecords,
                    readResponsePendingBytes, writeReadRequestPendingRecords, writeReadRequestPendingBytes,
                    writeTimeoutEvents, writeTimeoutEventsPerSec, readTimeoutEvents, readTimeoutEventsPerSec,
                    seconds, bytes, records, recsPerSec, mbPerSec, avgLatency, minLatency, maxLatency,
                    invalid, lowerDiscard, higherDiscard, slc1, slc2, percentileValues);
        }
    }


    @Override
    public final void reportLatencyRecord(LatencyRecord record) {

    }

    @Override
    public final void reportLatency(long latency, long count) {

    }

}
