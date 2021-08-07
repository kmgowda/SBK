/**
 * Copyright (c) KMG. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 */
package io.sbk.logger.impl;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.javaprop.JavaPropsFactory;
import io.sbk.action.Action;
import io.sbk.config.Config;
import io.sbk.options.InputOptions;
import io.sbk.logger.MetricsConfig;
import io.sbk.logger.Print;
import io.sbk.time.Time;
import io.sbk.system.Printer;
import java.io.IOException;
import java.io.InputStream;


/**
 * Class for Recoding/Printing benchmark results on micrometer Composite Meter Registry.
 */
public class SbkPrometheusLogger extends SbkCSVLogger {
    final static String CONFIG_FILE = "metrics.properties";
    public MetricsConfig metricsConfig;
    private boolean contextDisabled;
    private RWMetricsPrometheusServer prometheusServer;
    private Print printer;


    public SbkPrometheusLogger() {
        super();
        prometheusServer = null;
    }

    public RWMetricsPrometheusServer getMetricsPrometheusServer() throws IOException {
        return new RWMetricsPrometheusServer(Config.NAME+" "+storageName, action.name(),
                percentiles, time, metricsConfig);
    }

    public InputStream getMetricsConfigStream() {
        return  io.sbk.logger.impl.SbkPrometheusLogger.class.getClassLoader().getResourceAsStream(CONFIG_FILE);
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

        params.addOption("context", true, "Prometheus Metric context" +
                "; '"+DISABLE_STRING+"' disables this option; default: " + metricsConfig.port + metricsConfig.context);
    }


    @Override
    public void parseArgs(final InputOptions params) throws IllegalArgumentException {
        super.parseArgs(params);
        final String parsedContext =  params.getOptionValue("context", metricsConfig.port + metricsConfig.context);
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
    public void open(final InputOptions params, final String storageName, Action action, Time time) throws IllegalArgumentException, IOException {
        super.open(params, storageName, action, time);
        if (contextDisabled) {
            printer = super::print;
            prometheusServer = null;
        } else {
            prometheusServer = getMetricsPrometheusServer();
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
    public void incrementWriters() {
        super.incrementWriters();
        if (prometheusServer != null) {
            prometheusServer.incrementWriters();
        }
    }

    @Override
    public void decrementWriters() {
        super.decrementWriters();
        if (prometheusServer != null) {
            prometheusServer.decrementWriters();
        }
    }


    @Override
    public void incrementReaders() {
        super.incrementReaders();
        if (prometheusServer != null) {
            prometheusServer.incrementReaders();
        }

    }

    @Override
    public void decrementReaders() {
       super.decrementReaders();
        if (prometheusServer != null) {
            prometheusServer.decrementReaders();
        }
    }

    private void printMetrics(double seconds, long bytes, long records, double recsPerSec, double mbPerSec,
                              double avgLatency, long maxLatency, long invalid, long lowerDiscard, long higherDiscard,
                              int slc1, int slc2, long[] percentileValues) {
        super.print(seconds, bytes, records, recsPerSec, mbPerSec, avgLatency, maxLatency,
                invalid, lowerDiscard, higherDiscard, slc1, slc2, percentileValues);
        prometheusServer.print(seconds, bytes, records, recsPerSec, mbPerSec, avgLatency, maxLatency,
                invalid, lowerDiscard, higherDiscard, slc1, slc2, percentileValues);
    }

    @Override
    public void print(double seconds, long bytes, long records, double recsPerSec, double mbPerSec, double avgLatency,
                      long maxLatency, long invalid, long lowerDiscard, long higherDiscard,
                      int slc1, int slc2, long[] percentileValues) {
        printer.print(seconds, bytes, records, recsPerSec, mbPerSec, avgLatency, maxLatency,
                invalid, lowerDiscard, higherDiscard, slc1, slc2,  percentileValues);
    }
}
