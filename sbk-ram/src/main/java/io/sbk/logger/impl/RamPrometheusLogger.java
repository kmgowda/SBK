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

import io.perl.api.LatencyRecord;
import io.sbk.action.Action;
import io.sbk.config.Config;
import io.sbk.logger.RamLogger;
import io.sbk.logger.SetRW;
import io.sbk.options.ParsedOptions;
import io.sbk.system.Printer;
import io.time.Time;

import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.atomic.AtomicInteger;


/**
 * Class for Recoding/Printing benchmark results on micrometer Composite Meter Registry.
 */
public class RamPrometheusLogger extends PrometheusLogger implements SetRW, RamLogger {
    final static String CONFIG_FILE = "ram-metrics.properties";
    final static String SBK_RAM_PREFIX = "Sbk-Ram";
    private AtomicInteger connections;
    private AtomicInteger maxConnections;
    private RamMetricsPrometheusServer prometheusServer;


    /**
     * Constructor RamPrometheusLogger calling its super calls and initializing {@link #prometheusServer} = null.
     */
    public RamPrometheusLogger() {
        super();
        prometheusServer = null;
    }

    public InputStream getMetricsConfigStream() {
        return RamPrometheusLogger.class.getClassLoader().getResourceAsStream(CONFIG_FILE);
    }

    @Override
    public PrometheusRWMetricsServer getPrometheusRWMetricsServer() throws IOException {
        if (prometheusServer == null) {
            prometheusServer = new RamMetricsPrometheusServer(Config.NAME + " " + storageName, action.name(),
                    percentiles, time, metricsConfig);
        }
        return prometheusServer;
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


    private void print(String ramPrefix, String prefix, double seconds, long bytes, long records, double recsPerSec,
                       double mbPerSec, double avgLatency, long maxLatency, long invalid, long lowerDiscard,
                       long higherDiscard, long slc1, long slc2, long[] percentileValues) {
        StringBuilder out = new StringBuilder(ramPrefix);
        out.append(String.format(" %5d Connections, %5d Max Connections: ", connections.get(), maxConnections.get()));
        out.append(prefix);
        System.out.print(buildResultString(out, seconds, bytes, records, recsPerSec, mbPerSec, avgLatency,
                maxLatency, invalid, lowerDiscard, higherDiscard, slc1, slc2, percentileValues));
    }


    @Override
    public void print(double seconds, long bytes, long records, double recsPerSec, double mbPerSec, double avgLatency,
                      long maxLatency, long invalid, long lowerDiscard, long higherDiscard,
                      long slc1, long slc2, long[] percentileValues) {
        print(SBK_RAM_PREFIX, prefix, seconds, bytes, records, recsPerSec, mbPerSec, avgLatency, maxLatency,
                invalid, lowerDiscard, higherDiscard, slc1, slc2, percentileValues);
        if (prometheusServer != null) {
            prometheusServer.print(seconds, bytes, records, recsPerSec, mbPerSec, avgLatency, maxLatency,
                    invalid, lowerDiscard, higherDiscard, slc1, slc2, percentileValues);
        }
        if (csvEnable) {
            writeToCSV(SBK_RAM_PREFIX, REGULAR_PRINT, connections.get(), maxConnections.get(),
                    (long) seconds, bytes, records, recsPerSec, mbPerSec, avgLatency, maxLatency, invalid,
                    lowerDiscard, higherDiscard, slc1, slc2, percentileValues);
        }
    }

    @Override
    public void printTotal(double seconds, long bytes, long records, double recsPerSec, double mbPerSec,
                           double avgLatency, long maxLatency, long invalid, long lowerDiscard, long higherDiscard,
                           long slc1, long slc2, long[] percentileValues) {
        print("Total : " + SBK_RAM_PREFIX, prefix, seconds, bytes, records, recsPerSec, mbPerSec, avgLatency,
                maxLatency, invalid, lowerDiscard, higherDiscard, slc1, slc2, percentileValues);
        if (csvEnable) {
            writeToCSV(SBK_RAM_PREFIX, TOTAL_PRINT, connections.get(), maxConnections.get(),
                    (long) seconds, bytes, records, recsPerSec, mbPerSec, avgLatency, maxLatency, invalid,
                    lowerDiscard, higherDiscard, slc1, slc2, percentileValues);
        }
    }


    @Override
    public void reportLatencyRecord(LatencyRecord record) {

    }

    @Override
    public void reportLatency(long latency, long count) {

    }

    @Override
    public void setWriters(int val) {
        writers.set(val);
        if (prometheusServer != null) {
            prometheusServer.setWriters(val);
        }
    }

    @Override
    public void setMaxWriters(int val) {
        maxWriters.set(val);
        if (prometheusServer != null) {
            prometheusServer.setMaxWriters(val);
        }

    }

    @Override
    public void setReaders(int val) {
        readers.set(val);
        if (prometheusServer != null) {
            prometheusServer.setReaders(val);
        }

    }

    @Override
    public void setMaxReaders(int val) {
        maxReaders.set(val);
        if (prometheusServer != null) {
            prometheusServer.setMaxReaders(val);
        }
    }
}
