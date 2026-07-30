/**
 * Copyright (c) KMG. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 */
package io.sbk.webconsole;

import tools.jackson.databind.ObjectMapper;
import tools.jackson.dataformat.javaprop.JavaPropsFactory;
import io.sbk.action.Action;
import io.sbk.api.impl.Sbk;
import io.sbk.params.InputOptions;
import io.sbk.params.ParsedOptions;
import io.sbk.system.Printer;
import io.time.TimeUnit;

import java.io.IOException;
import java.io.InputStream;
import java.util.Objects;
import java.util.UUID;

/**
 * Shared Local Web Console configuration, lifecycle, and snapshot construction for SBK, SBM, and GEM loggers.
 */
public final class WebConsoleLoggerSupport implements AutoCloseable {
    /** Backward-compatible web console host CLI option. */
    public static final String HOST_OPTION = "dashboardhost";
    /** Backward-compatible web console port CLI option. */
    public static final String PORT_OPTION = "dashboardport";
    /** Backward-compatible web console automatic-start CLI option. */
    public static final String START_OPTION = "dashboardstart";
    /** Backward-compatible web console browser-open CLI option. */
    public static final String OPEN_OPTION = "dashboardopen";
    /** Backward-compatible web console history duration CLI option. */
    public static final String MINUTES_OPTION = "dashboardminutes";
    /** Backward-compatible web console run-name CLI option. */
    public static final String NAME_OPTION = "dashboardname";
    private static final String CONFIG_FILE = "webconsole.properties";
    private WebConsoleConfig config;
    private WebConsoleClient client;
    private String runId;
    private double[] percentiles;

    /**
     * Creates Local Web Console support with configuration loaded when arguments are added.
     */
    public WebConsoleLoggerSupport() {
    }

    /**
     * Adds the backward-compatible {@code -dashboard...} command-line options.
     *
     * @param params input option registry
     */
    public void addArgs(InputOptions params) {
        config = loadConfig();
        params.addOption(HOST_OPTION, true, "Local Web Console host; default: " + config.host);
        params.addOption(PORT_OPTION, true, "Local Web Console port; default: " + config.port);
        params.addOption(START_OPTION, true, "Start Local Web Console when unavailable; default: " + config.start);
        params.addOption(OPEN_OPTION, true, "Open Local Web Console in the local browser; default: " + config.open);
        params.addOption(MINUTES_OPTION, true, "Minutes of snapshots retained per run; default: " + config.minutes);
        params.addOption(NAME_OPTION, true, "Optional Local Web Console run name; default: empty");
    }

    /**
     * Parses Local Web Console options.
     *
     * @param params parsed command-line options
     * @throws IllegalArgumentException if a web console option is invalid
     */
    public void parseArgs(ParsedOptions params) {
        ensureConfig();
        config.host = params.getOptionValue(HOST_OPTION, config.host);
        config.port = Integer.parseInt(params.getOptionValue(PORT_OPTION, Integer.toString(config.port)));
        config.start = Boolean.parseBoolean(params.getOptionValue(START_OPTION, Boolean.toString(config.start)));
        config.open = Boolean.parseBoolean(params.getOptionValue(OPEN_OPTION, Boolean.toString(config.open)));
        config.minutes = Integer.parseInt(params.getOptionValue(MINUTES_OPTION,
                Integer.toString(config.minutes)));
        config.name = params.getOptionValue(NAME_OPTION, Objects.requireNonNullElse(config.name, ""));
        if (config.port < 1 || config.port > 65535) {
            throw new IllegalArgumentException("Local Web Console port must be between 1 and 65535");
        }
        if (config.minutes < 1) {
            throw new IllegalArgumentException("Local Web Console history minutes must be greater than zero");
        }
    }

    /**
     * Starts or reuses the Local Web Console and registers a benchmark run.
     *
     * @param source      source application name
     * @param storage     storage driver name
     * @param action      benchmark action
     * @param timeUnit    latency time unit
     * @param percentiles configured percentile labels
     * @throws IOException if another benchmark is already using the web console
     */
    public void open(String source, String storage, Action action, TimeUnit timeUnit, double[] percentiles)
            throws IOException {
        ensureConfig();
        this.percentiles = percentiles.clone();
        this.runId = UUID.randomUUID().toString();
        final String version = Objects.requireNonNullElse(Sbk.class.getPackage().getImplementationVersion(), "dev");
        final WebConsoleRun run = new WebConsoleRun(runId, config.name, source, storage, action.name(),
                timeUnit.name(), version, System.getProperty("java.runtime.version"), System.currentTimeMillis());
        try {
            client = WebConsoleClient.connect(config, run);
            client.getRunLinks().forEach(link -> Printer.log.info("SBK Local Web Console ({}): {}",
                    link.label(), link.uri()));
        } catch (WebConsoleClient.WebConsoleBusyException ex) {
            client = null;
            Printer.log.error("{} WebLogger cannot start: {}", source, ex.getMessage());
            throw ex;
        } catch (IOException ex) {
            client = null;
            Printer.log.warn("SBK Local Web Console is unavailable; benchmark will continue without live graphs: {}",
                    ex.getMessage());
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            client = null;
            Printer.log.warn("SBK Local Web Console startup was interrupted; benchmark will continue without live graphs");
        }
    }

    /**
     * Publishes one regular reporting-window snapshot without blocking the
     * caller on HTTP I/O.
     *
     * @param connections                     active connections
     * @param maxConnections                  maximum connections
     * @param writers                         active writers
     * @param maxWriters                      maximum writers
     * @param readers                         active readers
     * @param maxReaders                      maximum readers
     * @param writeRequestBytes               write request bytes
     * @param writeRequestMbPerSec            write request throughput
     * @param writeRequestRecords             write request records
     * @param writeRequestRecordsPerSec       write request rate
     * @param readRequestBytes                read request bytes
     * @param readRequestMbPerSec             read request throughput
     * @param readRequestRecords              read request records
     * @param readRequestRecordsPerSec        read request rate
     * @param writeResponsePendingRecords     pending write records
     * @param writeResponsePendingBytes       pending write bytes
     * @param readResponsePendingRecords      pending read records
     * @param readResponsePendingBytes        pending read bytes
     * @param writeReadRequestPendingRecords  pending combined records
     * @param writeReadRequestPendingBytes    pending combined bytes
     * @param writeTimeoutEvents              write timeout events
     * @param writeTimeoutEventsPerSec        write timeout rate
     * @param readTimeoutEvents               read timeout events
     * @param readTimeoutEventsPerSec         read timeout rate
     * @param seconds                         elapsed seconds
     * @param bytes                           completed bytes
     * @param records                         completed records
     * @param recordsPerSec                   completed record rate
     * @param mbPerSec                        completed throughput
     * @param averageLatency                  average latency
     * @param minimumLatency                  minimum latency
     * @param maximumLatency                  maximum latency
     * @param invalid                         invalid latencies
     * @param lowerDiscard                    low discarded latencies
     * @param higherDiscard                   high discarded latencies
     * @param slc1                            first SLC count
     * @param slc2                            second SLC count
     * @param percentileLatencies             percentile values
     * @param percentileLatencyCounts         percentile counts
     */
    public void publish(int connections, int maxConnections, int writers, int maxWriters,
                        int readers, int maxReaders, long writeRequestBytes, double writeRequestMbPerSec,
                        long writeRequestRecords, double writeRequestRecordsPerSec, long readRequestBytes,
                        double readRequestMbPerSec, long readRequestRecords, double readRequestRecordsPerSec,
                        long writeResponsePendingRecords, long writeResponsePendingBytes,
                        long readResponsePendingRecords, long readResponsePendingBytes,
                        long writeReadRequestPendingRecords, long writeReadRequestPendingBytes,
                        long writeTimeoutEvents, double writeTimeoutEventsPerSec, long readTimeoutEvents,
                        double readTimeoutEventsPerSec, double seconds, long bytes, long records,
                        double recordsPerSec, double mbPerSec, double averageLatency, long minimumLatency,
                        long maximumLatency, long invalid, long lowerDiscard, long higherDiscard,
                        long slc1, long slc2, long[] percentileLatencies, long[] percentileLatencyCounts) {
        if (client == null) {
            return;
        }
        final WebConsoleSnapshot.WorkerMetrics workers = new WebConsoleSnapshot.WorkerMetrics(writers, maxWriters,
                readers, maxReaders, connections, maxConnections);
        final WebConsoleSnapshot.RequestMetrics requests = new WebConsoleSnapshot.RequestMetrics(writeRequestBytes,
                writeRequestRecords, writeRequestMbPerSec, writeRequestRecordsPerSec, readRequestBytes,
                readRequestRecords, readRequestMbPerSec, readRequestRecordsPerSec, writeResponsePendingRecords,
                writeResponsePendingBytes, readResponsePendingRecords, readResponsePendingBytes,
                writeReadRequestPendingRecords, writeReadRequestPendingBytes, writeTimeoutEvents,
                writeTimeoutEventsPerSec, readTimeoutEvents, readTimeoutEventsPerSec);
        final WebConsoleSnapshot.PerformanceMetrics performance = new WebConsoleSnapshot.PerformanceMetrics(seconds,
                bytes, records, recordsPerSec, mbPerSec);
        final WebConsoleSnapshot.LatencyMetrics latency = new WebConsoleSnapshot.LatencyMetrics(averageLatency,
                minimumLatency, maximumLatency, invalid, lowerDiscard, higherDiscard, slc1, slc2, percentiles,
                percentileLatencies, percentileLatencyCounts);
        client.publish(new WebConsoleSnapshot(runId, System.currentTimeMillis(), workers, requests,
                performance, latency));
    }

    /**
     * Returns web console option names, including the command-line prefix.
     *
     * @return web console options
     */
    public String[] getOptionsArgs() {
        return new String[]{"-" + HOST_OPTION, "-" + PORT_OPTION, "-" + START_OPTION, "-" + OPEN_OPTION,
                "-" + MINUTES_OPTION, "-" + NAME_OPTION};
    }

    /**
     * Returns the current web console option/value pairs for forwarding to SBM.
     *
     * @return parsed web console arguments
     */
    public String[] getParsedArgs() {
        ensureConfig();
        return new String[]{"-" + HOST_OPTION, config.host, "-" + PORT_OPTION, Integer.toString(config.port),
                "-" + START_OPTION, Boolean.toString(config.start), "-" + OPEN_OPTION,
                Boolean.toString(config.open), "-" + MINUTES_OPTION, Integer.toString(config.minutes),
                "-" + NAME_OPTION, Objects.requireNonNullElse(config.name, "")};
    }

    @Override
    public void close() {
        if (client != null) {
            client.close();
            client = null;
        }
    }

    private static WebConsoleConfig loadConfig() {
        try (InputStream input = WebConsoleLoggerSupport.class.getClassLoader().getResourceAsStream(CONFIG_FILE)) {
            return new ObjectMapper(new JavaPropsFactory()).readValue(input, WebConsoleConfig.class);
        } catch (IOException ex) {
            throw new IllegalArgumentException("Unable to load " + CONFIG_FILE, ex);
        }
    }

    private void ensureConfig() {
        if (config == null) {
            config = loadConfig();
        }
    }
}
