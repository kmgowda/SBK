/**
 * Copyright (c) KMG. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 */
package io.sbk.ram.impl;

import io.sbk.api.Action;
import io.sbk.api.Config;
import io.sbk.ram.RamLogger;
import io.sbk.api.InputOptions;
import io.sbk.api.impl.RWMetricsPrometheusServer;
import io.sbk.api.impl.SbkPrometheusLogger;
import io.sbk.perl.LatencyRecord;
import io.sbk.perl.Time;
import io.sbk.ram.SetRW;
import io.sbk.system.Printer;
import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.atomic.AtomicInteger;


/**
 * Class for Recoding/Printing benchmark results on micrometer Composite Meter Registry.
 */
public class SbkRamPrometheusLogger extends SbkPrometheusLogger implements SetRW, RamLogger {
    final static String CONFIG_FILE = "ram-metrics.properties";
    final static String SBK_RAM_PREFIX = "Sbk-Ram";
    private AtomicInteger connections;
    private AtomicInteger maxConnections;
    private RamMetricsPrometheusServer prometheusServer;


    public SbkRamPrometheusLogger() {
        super();
        prometheusServer = null;
    }


    public InputStream getMetricsConfigStream() {
        return  io.sbk.ram.impl.SbkRam.class.getClassLoader().getResourceAsStream(CONFIG_FILE);
    }

    @Override
    public RWMetricsPrometheusServer getMetricsPrometheusServer() throws IOException {
        if (prometheusServer == null) {
            prometheusServer = new RamMetricsPrometheusServer(Config.NAME + " " + storageName, action.name(),
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
        prometheusServer.print( bytes, records, recsPerSec, mbPerSec, avgLatency, maxLatency,
                invalid, lowerDiscard, higherDiscard, percentileValues);
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

    @Override
    public void setWriters(int val) {
        writers.set(val);
    }

    @Override
    public void setMaxWriters(int val) {
        maxWriters.set(val);
    }

    @Override
    public void setReaders(int val) {
        readers.set(val);
    }

    @Override
    public void setMaxReaders(int val) {
        maxReaders.set(val);
    }
}
