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

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.javaprop.JavaPropsFactory;
import io.sbk.action.Action;
import io.sbk.config.Config;
import io.sbk.logger.MetricsConfig;
import io.sbk.logger.RWPrint;
import io.sbk.params.InputOptions;
import io.sbk.params.ParsedOptions;
import io.sbk.system.Printer;
import io.time.Time;

import java.io.IOException;
import java.io.InputStream;


/**
 * Class for Recoding/Printing benchmark results on micrometer Composite Meter Registry.
 */
public abstract class PrometheusLogger extends CSVLogger {
    final private static String CONFIG_FILE = "metrics.properties";
    protected MetricsConfig metricsConfig;
    private boolean contextDisabled;
    private PrometheusRWMetricsServer prometheusServer;
    private RWPrint printer;


    public PrometheusLogger() {
        super();
        prometheusServer = null;
    }

    public PrometheusRWMetricsServer getPrometheusRWMetricsServer() throws IOException {
        return new PrometheusRWMetricsServer(Config.NAME, action.name(), storageName,
                percentiles, time, metricsConfig);
    }

    public InputStream getMetricsConfigStream() {
        return PrometheusLogger.class.getClassLoader().getResourceAsStream(CONFIG_FILE);
    }

    @Override
    public void addArgs(final InputOptions params) throws IllegalArgumentException {
        super.addArgs(params);
        final ObjectMapper mapper = new ObjectMapper(new JavaPropsFactory())
                .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        try {
            metricsConfig = mapper.readValue(getMetricsConfigStream(), MetricsConfig.class);
        } catch (Exception ex) {
            ex.printStackTrace();
            throw new IllegalArgumentException(ex);
        }

        params.addOption("context", true, "Prometheus Metric context;" +
                "\n'" + DISABLE_STRING + "' disables this option; default: " + metricsConfig.port + metricsConfig.context);
    }


    @Override
    public void parseArgs(final ParsedOptions params) throws IllegalArgumentException {
        super.parseArgs(params);
        final String parsedContext = params.getOptionValue("context", metricsConfig.port + metricsConfig.context);
        if (parsedContext.equalsIgnoreCase(DISABLE_STRING)) {
            contextDisabled = true;
        } else {
            contextDisabled = false;
            String[] str = parsedContext.split("/", 2);
            metricsConfig.port = Integer.parseInt(str[0]);
            if (str.length == 2 && str[1] != null) {
                metricsConfig.context = "/" + str[1];
            }
        }
    }


    @Override
    public void open(final ParsedOptions params, final String storageName, Action action, Time time) throws IllegalArgumentException, IOException {
        super.open(params, storageName, action, time);
        if (contextDisabled) {
            printer = super::print;
            prometheusServer = null;
        } else {
            prometheusServer = getPrometheusRWMetricsServer();
            prometheusServer.start();
            printer = this::printMetrics;
        }
        Printer.log.info("SBK PrometheusLogger Started");
    }

    @Override
    public void close(final ParsedOptions params) throws IllegalArgumentException, IOException {
        if (prometheusServer != null) {
            prometheusServer.stop();
        }
        super.close(params);
        Printer.log.info("SBK PrometheusLogger Shutdown");
    }

    private void printMetrics(int writers, int maxWriters, int readers, int maxReaders,
                              long writeRequestBytes, double writeRequestsMbPerSec, long writesRequests,
                              double writeRequestsPerSec, long readRequestBytes, double readRequestsMBPerSec,
                              long readRequests, double readRequestsPerSec, long writeResponsePendingRecords,
                              long writeResponsePendingBytes, long readResponsePendingRecords, long readResponsePendingBytes,
                              long writeReadPendingRecords, long writeReadPendingBytes, double seconds, long bytes,
                              long records, double recsPerSec, double mbPerSec,
                              double avgLatency, long minLatency, long maxLatency, long invalid, long lowerDiscard,
                              long higherDiscard, long slc1, long slc2, long[] percentileValues) {
        super.print(writers, maxWriters, readers, maxReaders, writeRequestBytes, writeRequestsMbPerSec, writesRequests,
                writeRequestsPerSec, readRequestBytes, readRequestsMBPerSec, readRequests, readRequestsPerSec,
                writeResponsePendingRecords, writeResponsePendingBytes, readResponsePendingRecords,
                readResponsePendingBytes, writeReadPendingRecords, writeReadPendingBytes,
                seconds, bytes, records, recsPerSec, mbPerSec, avgLatency, minLatency, maxLatency,
                invalid, lowerDiscard, higherDiscard, slc1, slc2, percentileValues);
        prometheusServer.print(writers, maxWriters, readers, maxReaders, writeRequestBytes, writeRequestsMbPerSec,
                writesRequests, writeRequestsPerSec, readRequestBytes, readRequestsMBPerSec, readRequests,
                readRequestsPerSec, writeResponsePendingRecords, writeResponsePendingBytes, readResponsePendingRecords,
                readResponsePendingBytes, writeReadPendingRecords, writeReadPendingBytes,
                seconds, bytes, records, recsPerSec, mbPerSec, avgLatency, minLatency, maxLatency,
                invalid, lowerDiscard, higherDiscard, slc1, slc2, percentileValues);
    }

    @Override
    public void print(int writers, int maxWriters, int readers, int maxReaders,
                      long writeRequestBytes, double writeRequestMbPerSec, long writeRequestRecords,
                      double writeRequestRecordsPerSec, long readRequestBytes, double readRequestMbPerSec,
                      long readRequestRecords, double readRequestsRecordsPerSec, long writeResponsePendingRecords,
                      long writeResponsePendingBytes, long readResponsePendingRecords, long readResponsePendingBytes,
                      long writeReadRequestPendingRecords, long writeReadRequestPendingBytes, double seconds, long bytes,
                      long records, double recsPerSec, double mbPerSec,
                      double avgLatency, long minLatency, long maxLatency, long invalid, long lowerDiscard,
                      long higherDiscard, long slc1, long slc2, long[] percentileValues) {
        printer.print(writers, maxWriters, readers, maxReaders, writeRequestBytes, writeRequestMbPerSec,
                writeRequestRecords, writeRequestRecordsPerSec, readRequestBytes, readRequestMbPerSec,
                readRequestRecords, readRequestsRecordsPerSec, writeResponsePendingRecords, writeResponsePendingBytes,
                readResponsePendingRecords, readResponsePendingBytes, writeReadRequestPendingRecords, writeReadRequestPendingBytes,
                seconds, bytes, records, recsPerSec, mbPerSec, avgLatency, minLatency, maxLatency, invalid,
                lowerDiscard, higherDiscard, slc1, slc2, percentileValues);
    }
}
