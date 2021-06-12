/**
 * Copyright (c) KMG. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 */
package io.sbk.api.impl;

import io.sbk.api.Action;
import io.sbk.api.Config;
import io.sbk.api.RamLogger;
import io.sbk.api.InputOptions;
import io.sbk.perl.LatencyRecord;
import io.sbk.perl.Time;
import io.sbk.system.Printer;
import java.io.IOException;
import java.util.concurrent.atomic.AtomicInteger;


/**
 * Class for Recoding/Printing benchmark results on micrometer Composite Meter Registry.
 */
public class SbkRamPrometheusLogger extends SbkPrometheusLogger implements RamLogger {
    final static String CONFIG_FILE = "ram-metrics.properties";
    final static String SBK_RAM_PREFIX = "Sbk-Ram";
    private AtomicInteger connections;
    private AtomicInteger maxConnections;
    private ConnectionsRWMetricsPrometheusServer prometheusServer;


    public SbkRamPrometheusLogger() {
        super();
        prometheusServer = null;
    }

    @Override
    public String getConfigFile() {
        return CONFIG_FILE;
    }

    @Override
    public RWMetricsPrometheusServer getMetricsPrometheusServer() throws IOException {
        if (prometheusServer == null) {
            prometheusServer = new ConnectionsRWMetricsPrometheusServer(Config.NAME + " " + storageName, action.name(),
                    percentiles, time, metricsConfig);
        }
        return prometheusServer;
    }


    @Override
    public void open(final InputOptions params, final String storageName, Action action, Time time) throws IllegalArgumentException, IOException {
        super.open(params, storageName, action, time);
        this.connections = new AtomicInteger(0);
        this.maxConnections = new AtomicInteger(0);
        Printer.log.info("SBK Connections PrometheusLogger Started");
    }


    @Override
    public void incrementConnections(int val) {
        prometheusServer.incrementConnections(val);
        connections.set(connections.get() + val);
        maxConnections.set(maxConnections.get() + val);
    }

    @Override
    public void decrementConnections(int val) {
        prometheusServer.decrementConnections(val);
        connections.set(connections.get()-val);
    }

    @Override
    public void setConnections(int val) {
        prometheusServer.decrementConnections(val);
        connections.set(val);
        maxConnections.set(Math.max(connections.get(), maxConnections.get()));
    }

    @Override
    public void setMaxConnections(int val) {
        prometheusServer.setMaxConnections(val);
        maxConnections.set(val);
    }


    private void print(String prefix, long bytes, long records, double recsPerSec, double mbPerSec,
                       double avgLatency, long maxLatency, long invalid, long lowerDiscard, long higherDiscard,
                       long[] percentileValues) {
        StringBuilder out = new StringBuilder(SBK_RAM_PREFIX);
        out.append(String.format(" %5d Connections, %5d Max Connections: ", connections.get(), maxConnections.get()));
        out.append(prefix);
        System.out.print(buildResultString(out, bytes, records, recsPerSec, mbPerSec, avgLatency,
                maxLatency, invalid, lowerDiscard, higherDiscard, percentileValues));
    }


    @Override
    public void print(long bytes, long records, double recsPerSec, double mbPerSec, double avgLatency,
                      long maxLatency, long invalid, long lowerDiscard, long higherDiscard, long[] percentileValues) {
        print(prefix, bytes, records, recsPerSec, mbPerSec, avgLatency, maxLatency, invalid, lowerDiscard,
                higherDiscard, percentileValues);
    }

    @Override
    public void printTotal(long bytes, long records, double recsPerSec, double mbPerSec, double avgLatency,
                           long maxLatency, long invalid, long lowerDiscard, long higherDiscard, long[] percentilesValues) {
        print("Total : " + prefix, bytes, records, recsPerSec, mbPerSec, avgLatency, maxLatency,
                invalid, lowerDiscard, higherDiscard, percentilesValues);
    }


    @Override
    public void reportLatencyRecord(LatencyRecord record) {

    }

    @Override
    public void reportLatency(long latency, long count) {

    }
}
