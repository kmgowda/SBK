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

import tools.jackson.databind.ObjectMapper;
import tools.jackson.dataformat.javaprop.JavaPropsFactory;
import io.sbk.action.Action;
import io.sbk.config.Config;
import io.sbk.logger.MetricsConfig;
import io.sbk.logger.impl.PrometheusLinks;
import io.sbk.params.InputOptions;
import io.sbk.params.ParsedOptions;
import io.sbk.system.Printer;
import io.time.Time;

import java.io.IOException;
import java.io.InputStream;

/**
 * Concrete SBM logger that prints to stdout and exports metrics via Prometheus.
 *
 * <p>Extends {@link AbstractRamLogger} to format periodic and total results including
 * connection counts, request/response stats, throughput, and latency percentiles.
 */
public class SbmPrometheusLogger extends AbstractRamLogger {
    private static final String CONFIG_FILE = "sbm-metrics.properties";
    private MetricsConfig metricsConfig;
    private boolean contextDisabled;
    private SbmPrometheusServer prometheusServer;

    /**
     * Creates a Prometheus logger for aggregated SBM results.
     */
    public SbmPrometheusLogger() {
    }

    /**
     * Opens the bundled SBM metrics configuration.
     *
     * @return metrics configuration stream
     */
    public InputStream getMetricsConfigStream() {
        return SbmPrometheusLogger.class.getClassLoader().getResourceAsStream(CONFIG_FILE);
    }

    /**
     * Returns the parsed metrics configuration for GEM argument forwarding.
     *
     * @return active metrics configuration
     */
    protected final MetricsConfig getMetricsConfig() {
        return metricsConfig;
    }

    @Override
    public void addArgs(InputOptions params) throws IllegalArgumentException {
        super.addArgs(params);
        try {
            metricsConfig = new ObjectMapper(new JavaPropsFactory()).readValue(getMetricsConfigStream(),
                    MetricsConfig.class);
        } catch (Exception ex) {
            throw new IllegalArgumentException("Unable to load " + CONFIG_FILE, ex);
        }
        params.addOption("context", true, "Prometheus metric context; 'no' disables this option; default: "
                + metricsConfig.port + metricsConfig.context);
    }

    @Override
    public void parseArgs(ParsedOptions params) throws IllegalArgumentException {
        super.parseArgs(params);
        final String parsedContext = params.getOptionValue("context",
                metricsConfig.port + metricsConfig.context);
        contextDisabled = parsedContext.equalsIgnoreCase(DISABLE_STRING);
        if (!contextDisabled) {
            final String[] values = parsedContext.split("/", 2);
            metricsConfig.port = Integer.parseInt(values[0]);
            if (values.length == 2 && values[1] != null) {
                metricsConfig.context = "/" + values[1];
            }
        }
    }

    @Override
    public void open(ParsedOptions params, String storageName, Action action, Time time) throws IOException {
        super.open(params, storageName, action, time);
        try {
            if (!contextDisabled) {
                prometheusServer = createPrometheusServer(storageName, action, time);
                prometheusServer.start();
                PrometheusLinks.log("SBM", metricsConfig);
            }
        } catch (IOException | RuntimeException | Error failure) {
            rollbackOpen(params, failure);
            throw propagate(failure);
        }
        Printer.log.info("SBM PrometheusLogger Started");
    }

    @Override
    public void close(ParsedOptions params) throws IOException {
        Throwable failure = null;
        try {
            stopPrometheusServer();
        } catch (IOException | RuntimeException | Error ex) {
            failure = ex;
        }
        try {
            super.close(params);
        } catch (IOException | RuntimeException | Error ex) {
            failure = recordFailure(failure, ex);
        }
        if (failure != null) {
            throw propagate(failure);
        }
        Printer.log.info("SBM PrometheusLogger Shutdown");
    }

    /**
     * Creates the Prometheus server for this logger.
     *
     * @param storageName resolved storage driver name
     * @param action benchmark action
     * @param time benchmark time source
     * @return configured SBM Prometheus server
     * @throws IOException if the server cannot be created
     */
    protected SbmPrometheusServer createPrometheusServer(String storageName, Action action, Time time)
            throws IOException {
        return new SbmPrometheusServer(Config.NAME, action.name(), storageName,
                getPercentiles(), time, metricsConfig);
    }

    private void rollbackOpen(ParsedOptions params, Throwable failure) {
        try {
            stopPrometheusServer();
        } catch (IOException | RuntimeException | Error ex) {
            failure.addSuppressed(ex);
        }
        try {
            super.close(params);
        } catch (IOException | RuntimeException | Error ex) {
            failure.addSuppressed(ex);
        }
    }

    private void stopPrometheusServer() throws IOException {
        final SbmPrometheusServer server = prometheusServer;
        prometheusServer = null;
        if (server != null) {
            server.stop();
        }
    }

    private static Throwable recordFailure(Throwable failure, Throwable additionalFailure) {
        if (failure == null) {
            return additionalFailure;
        }
        failure.addSuppressed(additionalFailure);
        return failure;
    }

    private static IOException propagate(Throwable failure) {
        if (failure instanceof IOException ioException) {
            return ioException;
        }
        if (failure instanceof RuntimeException runtimeException) {
            throw runtimeException;
        }
        if (failure instanceof Error error) {
            throw error;
        }
        return new IOException("SBM Prometheus logger lifecycle failed", failure);
    }

    @Override
    public void incrementConnections() {
        super.incrementConnections();
        if (prometheusServer != null) {
            prometheusServer.incrementConnections();
        }
    }

    @Override
    public void decrementConnections() {
        super.decrementConnections();
        if (prometheusServer != null) {
            prometheusServer.decrementConnections();
        }
    }

    @Override
    public void print(long reportTime, int connections, int maxConnections, int writers, int maxWriters, int readers,
                      int maxReaders, long writeRequestBytes, double writeRequestMbPerSec, long writeRequestRecords,
                      double writeRequestRecordsPerSec, long readRequestBytes, double readRequestMbPerSec,
                      long readRequestRecords, double readRequestsRecordsPerSec, long writeResponsePendingRecords,
                      long writeResponsePendingBytes, long readResponsePendingRecords, long readResponsePendingBytes,
                      long writeReadRequestPendingRecords, long writeReadRequestPendingBytes, long writeTimeoutEvents,
                      double writeTimeoutEventsPerSec, long readTimeoutEvents, double readTimeoutEventsPerSec,
                      double seconds, long bytes, long records, double recsPerSec, double mbPerSec, double avgLatency,
                      long minLatency, long maxLatency, long invalid, long lowerDiscard, long higherDiscard,
                      long slc1, long slc2, long[] percentileLatencies, long[] percentileLatencyCounts) {
        publishMetrics(writers, maxWriters, readers, maxReaders, writeRequestBytes, writeRequestMbPerSec,
                writeRequestRecords, writeRequestRecordsPerSec, readRequestBytes, readRequestMbPerSec,
                readRequestRecords, readRequestsRecordsPerSec, writeResponsePendingRecords,
                writeResponsePendingBytes, readResponsePendingRecords, readResponsePendingBytes,
                writeReadRequestPendingRecords, writeReadRequestPendingBytes, writeTimeoutEvents,
                writeTimeoutEventsPerSec, readTimeoutEvents, readTimeoutEventsPerSec, seconds, bytes, records,
                recsPerSec, mbPerSec, avgLatency, minLatency, maxLatency, invalid, lowerDiscard, higherDiscard,
                slc1, slc2, percentileLatencies, percentileLatencyCounts);
        String timestamp = getTimeStamp(reportTime);
        StringBuilder out = new StringBuilder(timestamp+", "+SBM_PREFIX);
        appendConnections(out, connections, maxConnections);
        out.append(getPrefix());
        appendResultString(out, writers, maxWriters, readers, maxReaders,
                writeRequestBytes, writeRequestMbPerSec, writeRequestRecords, writeRequestRecordsPerSec,
                readRequestBytes, readRequestMbPerSec, readRequestRecords, readRequestsRecordsPerSec,
                writeResponsePendingRecords, writeResponsePendingBytes, readResponsePendingRecords,
                readResponsePendingBytes, writeReadRequestPendingRecords, writeReadRequestPendingBytes,
                writeTimeoutEvents, writeTimeoutEventsPerSec, readTimeoutEvents, readTimeoutEventsPerSec,
                seconds, bytes, records, recsPerSec, mbPerSec, avgLatency, minLatency, maxLatency,
                invalid, lowerDiscard, higherDiscard, slc1, slc2, percentileLatencies, percentileLatencyCounts);
        System.out.println(out);
    }

    @Override
    public void printTotal(long reportTime, int connections, int maxConnections, int writers, int maxWriters,
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
                           long slc1, long slc2, long[] percentileLatencies, long[] percentileLatencyCounts) {
        // Prometheus gauges represent the latest regular reporting window; cumulative totals would distort them.
        String timestamp = getTimeStamp(reportTime);
        StringBuilder out = new StringBuilder(timestamp+" Total : " + SBM_PREFIX);
        appendConnections(out, connections, maxConnections);
        out.append(getPrefix());
        appendResultString(out, writers, maxWriters, readers, maxReaders,
                writeRequestBytes, writeRequestMbPerSec, writeRequestRecords, writeRequestRecordsPerSec,
                readRequestBytes, readRequestMbPerSec, readRequestRecords, readRequestRecordsPerSec,
                writeResponsePendingRecords, writeResponsePendingBytes, readResponsePendingRecords,
                readResponsePendingBytes, writeReadRequestPendingRecords, writeReadRequestPendingBytes,
                writeTimeoutEvents, writeTimeoutEventsPerSec, readTimeoutEvents, readTimeoutEventsPerSec,
                seconds, bytes, records, recsPerSec, mbPerSec, avgLatency, minLatency, maxLatency,
                invalid, lowerDiscard, higherDiscard, slc1, slc2, percentileLatencies, percentileLatencyCounts);
        System.out.println(out);
    }

    private void publishMetrics(int writers, int maxWriters, int readers, int maxReaders,
                                long writeRequestBytes, double writeRequestMbPerSec, long writeRequestRecords,
                                double writeRequestRecordsPerSec, long readRequestBytes, double readRequestMbPerSec,
                                long readRequestRecords, double readRequestRecordsPerSec,
                                long writeResponsePendingRecords, long writeResponsePendingBytes,
                                long readResponsePendingRecords, long readResponsePendingBytes,
                                long writeReadRequestPendingRecords, long writeReadRequestPendingBytes,
                                long writeTimeoutEvents, double writeTimeoutEventsPerSec, long readTimeoutEvents,
                                double readTimeoutEventsPerSec, double seconds, long bytes, long records,
                                double recsPerSec, double mbPerSec, double avgLatency, long minLatency,
                                long maxLatency, long invalid, long lowerDiscard, long higherDiscard,
                                long slc1, long slc2, long[] percentileLatencies,
                                long[] percentileLatencyCounts) {
        if (prometheusServer != null) {
            prometheusServer.print(writers, maxWriters, readers, maxReaders, writeRequestBytes,
                    writeRequestMbPerSec, writeRequestRecords, writeRequestRecordsPerSec, readRequestBytes,
                    readRequestMbPerSec, readRequestRecords, readRequestRecordsPerSec,
                    writeResponsePendingRecords, writeResponsePendingBytes, readResponsePendingRecords,
                    readResponsePendingBytes, writeReadRequestPendingRecords, writeReadRequestPendingBytes,
                    writeTimeoutEvents, writeTimeoutEventsPerSec, readTimeoutEvents, readTimeoutEventsPerSec,
                    seconds, bytes, records, recsPerSec, mbPerSec, avgLatency, minLatency, maxLatency, invalid,
                    lowerDiscard, higherDiscard, slc1, slc2, percentileLatencies, percentileLatencyCounts);
        }
    }
}
