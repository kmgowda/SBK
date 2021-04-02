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
import com.sun.net.httpserver.HttpServer;
import io.micrometer.core.instrument.Clock;
import io.micrometer.core.instrument.Metrics;
import io.micrometer.core.instrument.composite.CompositeMeterRegistry;
import io.micrometer.jmx.JmxConfig;
import io.micrometer.jmx.JmxMeterRegistry;
import io.micrometer.prometheus.PrometheusConfig;
import io.micrometer.prometheus.PrometheusMeterRegistry;
import io.micrometer.prometheus.PrometheusRenameFilter;
import io.sbk.api.Action;
import io.sbk.api.Config;
import io.sbk.perl.PerlConfig;
import io.sbk.perl.LoggerConfig;
import io.sbk.perl.MetricsConfig;
import io.sbk.api.Parameters;
import io.sbk.perl.Print;
import io.sbk.perl.Time;
import io.sbk.perl.TimeUnit;
import io.sbk.perl.impl.MetricsLogger;
import io.sbk.system.Printer;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.util.Arrays;
import java.util.concurrent.Executors;


/**
 * Class for Recoding/Printing benchmark results on micrometer Composite Meter Registry.
 */
public class PrometheusLogger extends SystemLogger {
    final static String LOGGER_FILE = "logger.properties";
    final static String CONFIG_FILE = "metrics.properties";
    private LoggerConfig loggerConfig;
    private MetricsConfig config;
    private boolean disabled;
    private double[] percentilesIndices;
    private MetricsLogger metricsLogger;
    private HttpServer server;
    private Print printer;
    private long minLatency;
    private long maxLatency;

    public PrometheusLogger() {
        super();
    }

    @Override
    public void addArgs(final Parameters params) throws IllegalArgumentException {
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
    public void parseArgs(final Parameters params) throws IllegalArgumentException {
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

    private HttpServer createHttpServer(PrometheusMeterRegistry prometheusRegistry) throws IOException {
        final HttpServer server = HttpServer.create(new InetSocketAddress(config.port), 0);
        server.createContext(config.context, httpExchange -> {
            String response = prometheusRegistry.scrape();
            httpExchange.sendResponseHeaders(200, response.getBytes().length);
            try (OutputStream os = httpExchange.getResponseBody()) {
                os.write(response.getBytes());
            }
        });
        server.setExecutor(Executors.newSingleThreadExecutor());
        server.start();
        return server;
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
    public void open(final Parameters params, final String storageName, Action action, Time time) throws IllegalArgumentException, IOException {
        super.open(params, storageName, action, time);
        if (disabled) {
            printer = super::print;
            metricsLogger = null;
            server = null;
        } else {
            final CompositeMeterRegistry compositeRegistry = Metrics.globalRegistry;
            final PrometheusMeterRegistry prometheusRegistry = new PrometheusMeterRegistry(PrometheusConfig.DEFAULT);
            prometheusRegistry.config().meterFilter(new PrometheusRenameFilter());
            compositeRegistry.add(new JmxMeterRegistry(JmxConfig.DEFAULT, Clock.SYSTEM));
            compositeRegistry.add(prometheusRegistry);
            metricsLogger = new MetricsLogger(Config.NAME, storageName, action.name(), time, config.latencyTimeUnit,
                    percentiles, params.getWritersCount(), params.getReadersCount(), compositeRegistry);
            printer = this::printMetrics;
            server = createHttpServer(prometheusRegistry);
        }
        Printer.log.info("PrometheusLogger Started");
    }

    @Override
    public void close(final Parameters params) throws IllegalArgumentException, IOException  {
        if (metricsLogger != null) {
            metricsLogger.close();
        }
        if (server != null) {
            server.stop(0);
        }
        super.close(params);
        Printer.log.info("PrometheusLogger Stopped");
    }

    private void printMetrics(long bytes, long records, double recsPerSec, double mbPerSec, double avgLatency, long maxLatency,
                      long invalid, long lowerDiscard, long higherDiscard, long[] percentileValues) {
        super.print( bytes, records, recsPerSec, mbPerSec, avgLatency, maxLatency,
                invalid, lowerDiscard, higherDiscard, percentileValues);
        metricsLogger.print( bytes, records, recsPerSec, mbPerSec, avgLatency, maxLatency,
                invalid, lowerDiscard, higherDiscard, percentileValues);
    }

    @Override
    public void print(long bytes, long records, double recsPerSec, double mbPerSec, double avgLatency, long maxLatency,
               long invalid, long lowerDiscard, long higherDiscard, long[] percentileValues) {
        printer.print(bytes, records, recsPerSec, mbPerSec, avgLatency, maxLatency,
                invalid, lowerDiscard, higherDiscard, percentileValues);
    }
}
