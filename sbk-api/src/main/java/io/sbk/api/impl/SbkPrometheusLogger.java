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

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.javaprop.JavaPropsFactory;
import io.sbk.api.Action;
import io.sbk.api.Config;
import io.sbk.api.InputOptions;
import io.sbk.perl.PerlConfig;
import io.sbk.perl.LoggerConfig;
import io.sbk.perl.MetricsConfig;
import io.sbk.perl.Print;
import io.sbk.perl.Time;
import io.sbk.perl.TimeUnit;
import io.sbk.system.Printer;

import java.io.IOException;
import java.util.Arrays;


/**
 * Class for Recoding/Printing benchmark results on micrometer Composite Meter Registry.
 */
public class SbkPrometheusLogger extends SystemLogger {
    final static String LOGGER_FILE = "logger.properties";
    final static String CONFIG_FILE = "metrics.properties";
    private LoggerConfig loggerConfig;
    private MetricsConfig config;
    private boolean disabled;
    private double[] percentilesIndices;
    private RWMetricsPrometheusServer prometheusServer;
    private Print printer;
    private long minLatency;
    private long maxLatency;

    public SbkPrometheusLogger() {
        super();
    }

    @Override
    public void addArgs(final InputOptions params) throws IllegalArgumentException {
        super.addArgs(params);
        final ObjectMapper mapper = new ObjectMapper(new JavaPropsFactory())
                .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        try {
            loggerConfig = mapper.readValue(io.sbk.api.impl.Sbk.class.getClassLoader().getResourceAsStream(LOGGER_FILE),
                    LoggerConfig.class);
            config = mapper.readValue(io.sbk.api.impl.Sbk.class.getClassLoader().getResourceAsStream(CONFIG_FILE),
                    MetricsConfig.class);
        } catch (Exception ex) {
            ex.printStackTrace();
            throw new IllegalArgumentException(ex);
        }

        String[] percentilesList = loggerConfig.percentiles.split(",");
        percentilesIndices = new double[percentilesList.length];
        for (int i = 0; i < percentilesList.length; i++) {
            percentilesIndices[i] = Double.parseDouble(percentilesList[i].trim());
        }
        Arrays.sort(percentilesIndices);

        params.addOption("time", true, "Latency Time Unit " + getTimeUnitNames() +
                "; default: " + loggerConfig.timeUnit.name());
        params.addOption("context", true, "Prometheus Metric context" +
                "; default context: " + config.port + config.context + "; 'no' disables the metrics");
    }

    private String getTimeUnitNames() {
        String ret = "[";

        for (TimeUnit value : TimeUnit.values()) {
            ret += value.name() +":" +value.toString() + ", ";
        }
        ret += "]";

        return ret.replace(", ]", "]");
    }



    @Override
    public void parseArgs(final InputOptions params) throws IllegalArgumentException {
        super.parseArgs(params);
        loggerConfig.timeUnit = TimeUnit.valueOf(params.getOptionValue("time", loggerConfig.timeUnit.name()));
        final String fullContext =  params.getOptionValue("context", config.port + config.context);
        if (fullContext.equalsIgnoreCase("no")) {
            disabled = true;
        } else {
            disabled = false;
            String[] str = fullContext.split("/", 2);
            config.port = Integer.parseInt(str[0]);
            if (str.length == 2 && str[1] != null) {
                config.context = "/" + str[1];
            }
        }

        int val = 1;
        if (loggerConfig.timeUnit == TimeUnit.ns) {
            val = PerlConfig.NS_PER_MS;
        } else if (loggerConfig.timeUnit == TimeUnit.mcs) {
            val = PerlConfig.MICROS_PER_MS;
        }
        minLatency = (long) (((double) loggerConfig.minLatencyMS) * val);
        maxLatency = (long) (((double) loggerConfig.maxLatencyMS) * val);
    }

    @Override
    public int getReportingIntervalSeconds() {
        return loggerConfig.reportingSeconds;
    }

    @Override
    public TimeUnit getTimeUnit() {
        return loggerConfig.timeUnit;
    }

    @Override
    public long getMinLatency() {
        return minLatency;
    }

    @Override
    public long getMaxLatency() {
        return maxLatency;
    }

    @Override
    public double[] getPercentiles() {
        return percentilesIndices;
    }


    @Override
    public void open(final InputOptions params, final String storageName, Action action, Time time) throws IllegalArgumentException, IOException {
        super.open(params, storageName, action, time);
        if (disabled) {
            printer = super::print;
            prometheusServer = null;
        } else {
            prometheusServer = new RWMetricsPrometheusServer(Config.NAME+" "+storageName, action.name(), percentiles, time, config);
            prometheusServer.start();
            printer = this::printMetrics;
        }
        Printer.log.info("SBK PrometheusLogger Started");
    }

    @Override
    public void close(final InputOptions params) throws IllegalArgumentException, IOException  {
        if (prometheusServer != null) {
            prometheusServer.stop();
        }
        super.close(params);
        Printer.log.info("SBK PrometheusLogger Shutdown");
    }

    @Override
    public void incrementWriters(int val) {
        super.incrementWriters(val);
        if (prometheusServer != null) {
            prometheusServer.incrementWriters(val);
        }
    }

    @Override
    public void decrementWriters(int val) {
        super.decrementWriters(val);
        if (prometheusServer != null) {
            prometheusServer.decrementWriters(val);
        }

    }

    @Override
    public void incrementReaders(int val) {
        super.incrementReaders(val);
        if (prometheusServer != null) {
            prometheusServer.incrementReaders(val);
        }

    }

    @Override
    public void decrementReaders(int val) {
       super.decrementReaders(val);
        if (prometheusServer != null) {
            prometheusServer.decrementReaders(val);
        }

    }

    private void printMetrics(long bytes, long records, double recsPerSec, double mbPerSec, double avgLatency, long maxLatency,
                      long invalid, long lowerDiscard, long higherDiscard, long[] percentileValues) {
        super.print( bytes, records, recsPerSec, mbPerSec, avgLatency, maxLatency,
                invalid, lowerDiscard, higherDiscard, percentileValues);
        prometheusServer.print( bytes, records, recsPerSec, mbPerSec, avgLatency, maxLatency,
                invalid, lowerDiscard, higherDiscard, percentileValues);
    }

    @Override
    public void print(long bytes, long records, double recsPerSec, double mbPerSec, double avgLatency, long maxLatency,
               long invalid, long lowerDiscard, long higherDiscard, long[] percentileValues) {
        printer.print(bytes, records, recsPerSec, mbPerSec, avgLatency, maxLatency,
                invalid, lowerDiscard, higherDiscard, percentileValues);
    }
}
