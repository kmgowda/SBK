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
import io.sbk.perl.PerlConfig;
import io.sbk.ram.RamLogger;
import io.sbk.api.InputOptions;
import io.sbk.api.impl.RWMetricsPrometheusServer;
import io.sbk.api.impl.SbkPrometheusLogger;
import io.sbk.perl.LatencyRecord;
import io.sbk.perl.Time;
import io.sbk.ram.SetRW;
import io.sbk.system.Printer;

import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
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
    public void openCSV() throws IOException {
        final StringBuilder headerBuilder = new StringBuilder("Header,Connections,MaxConnections");
        headerBuilder.append(",Action,LatencyTimeUnit,Writers,Readers,MaxWriters,MaxReaders");
        headerBuilder.append(",MB,Records,Records/Sec,MB/Sec");
        headerBuilder.append(",AvgLatency,MaxLatency,InvalidLatencies,LowerDiscard,HigherDiscard");
        for (String percentileName : percentileNames) {
            headerBuilder.append(",Percentile_");
            headerBuilder.append(percentileName);
        }
        csvWriter = new PrintWriter(new FileWriter(csvFile, false));
        csvWriter.println(headerBuilder);
    }

    @Override
    public void writeToCSV(String prefix, long bytes, long records, double recsPerSec, double mbPerSec,
                           double avgLatency, long maxLatency, long invalid, long lowerDiscard, long higherDiscard,
                           long[] percentileValues) {
        final double mBytes = (bytes * 1.0) / PerlConfig.BYTES_PER_MB;
        StringBuilder data = new StringBuilder(
                String.format("%s,%5d,%5d,%s,%s,%5d,%5d,%5d,%5d,%11.1f,%11d,%9.1f,%8.2f,%8.1f,%7d,%8d,%8d,%8d",
                        SBK_RAM_PREFIX, connections.get(), maxConnections.get(),
                        prefix, timeUnitText, writers.get(), readers.get(), maxWriters.get(), maxReaders.get(),
                        mBytes, records, recsPerSec, mbPerSec, avgLatency, maxLatency,
                        invalid, lowerDiscard, higherDiscard)
        );

        for (int i = 0; i < Math.min(percentiles.length, percentileValues.length); ++i) {
            data.append(String.format(", %7d", percentileValues[i]));
        }
        csvWriter.println(data);
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
    public void print(double seconds, long bytes, long records, double recsPerSec, double mbPerSec, double avgLatency,
                      long maxLatency, long invalid, long lowerDiscard, long higherDiscard, long[] percentileValues) {
        print(prefix, bytes, records, recsPerSec, mbPerSec, avgLatency, maxLatency, invalid, lowerDiscard,
                higherDiscard, percentileValues);
        if (prometheusServer != null) {
            prometheusServer.print(seconds, bytes, records, recsPerSec, mbPerSec, avgLatency, maxLatency,
                    invalid, lowerDiscard, higherDiscard, percentileValues);
        }
        if (csvEnable) {
            writeToCSV(prefix, bytes, records, recsPerSec, mbPerSec, avgLatency, maxLatency, invalid, lowerDiscard,
                    higherDiscard, percentileValues);
        }
    }

    @Override
    public void printTotal(double seconds, long bytes, long records, double recsPerSec, double mbPerSec,
                           double avgLatency, long maxLatency, long invalid, long lowerDiscard, long higherDiscard,
                           long[] percentilesValues) {
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
