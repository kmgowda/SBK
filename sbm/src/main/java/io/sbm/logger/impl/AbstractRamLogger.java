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
import io.sbk.logger.impl.PrometheusLogger;
import io.sbk.logger.impl.PrometheusRWMetricsServer;
import io.sbk.params.ParsedOptions;
import io.sbk.system.Printer;
import io.sbm.logger.RamLogger;
import io.time.Time;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.atomic.AtomicInteger;

public abstract class AbstractRamLogger extends PrometheusLogger implements RamLogger {
    final static String CONFIG_FILE = "sbm-metrics.properties";
    final static String SBM_PREFIX = "SBM";
    final static int MAX_REQUEST_RW_IDS = 10;
    private AtomicInteger connections;
    private AtomicInteger maxConnections;
    private SbmMetricsPrometheusServer prometheusServer;

    /**
     * Constructor RamPrometheusLogger calling its super calls and initializing {@link #prometheusServer} = null.
     */
    public AbstractRamLogger() {
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
    public void print(int writers, int maxWriters, int readers, int maxReaders,
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
        if (prometheusServer != null) {
            prometheusServer.print(writers, maxWriters, readers, maxReaders,
                    writeRequestBytes, writeRequestMbPerSec, writeRequestRecords, writeRequestRecordsPerSec,
                    readRequestBytes, readRequestMbPerSec, readRequestRecords, readRequestRecordsPerSec,
                    writeResponsePendingRecords, writeResponsePendingBytes, readResponsePendingRecords,
                    readResponsePendingBytes, writeReadRequestPendingRecords, writeReadRequestPendingBytes,
                    writeTimeoutEvents, writeTimeoutEventsPerSec, readTimeoutEvents, readTimeoutEventsPerSec,
                    seconds, bytes, records, recsPerSec, mbPerSec, avgLatency, minLatency, maxLatency,
                    invalid, lowerDiscard, higherDiscard, slc1, slc2, percentileValues);
        }
        if (isCsvEnable()) {
            writeToCSV(SBM_PREFIX, REGULAR_PRINT, connections.get(), maxConnections.get(),
                    writers, maxWriters, readers, maxReaders,
                    writeRequestBytes, writeRequestMbPerSec, writeRequestRecords, writeRequestRecordsPerSec,
                    readRequestBytes, readRequestMbPerSec, readRequestRecords, readRequestRecordsPerSec,
                    writeResponsePendingRecords, writeResponsePendingBytes, readResponsePendingRecords,
                    readResponsePendingBytes, writeReadRequestPendingRecords, writeReadRequestPendingBytes,
                    writeTimeoutEvents, writeTimeoutEventsPerSec, readTimeoutEvents, readTimeoutEventsPerSec,
                    seconds, bytes, records, recsPerSec, mbPerSec, avgLatency, minLatency, maxLatency,
                    invalid, lowerDiscard, higherDiscard, slc1, slc2, percentileValues);
        }
        print(connections.get(), maxConnections.get(), writers, maxWriters, readers, maxReaders,
                writeRequestBytes, writeRequestMbPerSec, writeRequestRecords, writeRequestRecordsPerSec,
                readRequestBytes, readRequestMbPerSec, readRequestRecords, readRequestRecordsPerSec,
                writeResponsePendingRecords, writeResponsePendingBytes, readResponsePendingRecords,
                readResponsePendingBytes, writeReadRequestPendingRecords, writeReadRequestPendingBytes,
                writeTimeoutEvents, writeTimeoutEventsPerSec, readTimeoutEvents, readTimeoutEventsPerSec,
                seconds, bytes, records, recsPerSec, mbPerSec, avgLatency, minLatency, maxLatency,
                invalid, lowerDiscard, higherDiscard, slc1, slc2, percentileValues);
    }

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
        try {
            throw new IOException("The print method is not overridden/implemented\n");
        } catch (Exception ex) {
            ex.printStackTrace();
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
        if (isCsvEnable()) {
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

        printTotal(connections.get(), maxConnections.get(), writers, maxWriters, readers, maxReaders,
                writeRequestBytes, writeRequestMbPerSec, writeRequestRecords, writeRequestRecordsPerSec,
                readRequestBytes, readRequestMbPerSec, readRequestRecords, readRequestRecordsPerSec,
                writeResponsePendingRecords, writeResponsePendingBytes, readResponsePendingRecords,
                readResponsePendingBytes, writeReadRequestPendingRecords, writeReadRequestPendingBytes,
                writeTimeoutEvents, writeTimeoutEventsPerSec, readTimeoutEvents, readTimeoutEventsPerSec,
                seconds, bytes, records, recsPerSec, mbPerSec, avgLatency, minLatency, maxLatency,
                invalid, lowerDiscard, higherDiscard, slc1, slc2, percentileValues);
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
        try {
            throw new IOException("The printTotal method is not overridden/implemented\n");
        } catch (Exception ex) {
            ex.printStackTrace();
        }

    }

}
