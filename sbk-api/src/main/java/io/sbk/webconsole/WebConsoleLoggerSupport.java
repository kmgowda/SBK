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

import io.sbk.action.Action;
import io.sbk.api.impl.Sbk;
import io.sbk.params.InputOptions;
import io.sbk.params.ParsedOptions;
import io.sbk.system.Printer;
import io.time.TimeUnit;

import java.io.IOException;
import java.util.Objects;
import java.util.UUID;

/**
 * Shared Local Web Console configuration, lifecycle, and snapshot construction for SBK, SBM, and GEM loggers.
 */
public final class WebConsoleLoggerSupport implements AutoCloseable {
    /** Web console port CLI option. */
    public static final String PORT_OPTION = "webport";
    /** Web console browser-open CLI option. */
    public static final String OPEN_OPTION = "webopen";
    /** Web console history duration CLI option. */
    public static final String SNAPSHOT_MINUTES_OPTION = "websnapshotminutes";
    /** Web console idle-timeout CLI option. */
    public static final String TIMEOUT_OPTION = "webtimeoutminutes";
    /** Web console benchmark-board name CLI option. */
    public static final String BOARD_NAME_OPTION = "boardname";
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
     * Adds the Local Web Console command-line options.
     *
     * @param params input option registry
     */
    public void addArgs(InputOptions params) {
        config = loadConfig();
        params.addOption(PORT_OPTION, true, "Local Web Console port; default: " + config.port);
        params.addOption(OPEN_OPTION, true, "Open Local Web Console in the local browser; default: " + config.open);
        params.addOption(SNAPSHOT_MINUTES_OPTION, true,
                "Minutes of snapshots retained per run; default: " + config.snapshotMinutes);
        params.addOption(TIMEOUT_OPTION, true,
                "Idle minutes without a benchmark or browser before Local Web Console exits; default: "
                        + config.timeoutMinutes);
        params.addOption(BOARD_NAME_OPTION, true,
                "Optional display name for the benchmark board in Local Web Console"
                        + "; default: <application> <storage>");
    }

    /**
     * Parses Local Web Console options.
     *
     * @param params parsed command-line options
     * @throws IllegalArgumentException if a web console option is invalid
     */
    public void parseArgs(ParsedOptions params) {
        ensureConfig();
        config.port = Integer.parseInt(params.getOptionValue(PORT_OPTION, Integer.toString(config.port)));
        config.open = Boolean.parseBoolean(params.getOptionValue(OPEN_OPTION, Boolean.toString(config.open)));
        config.snapshotMinutes = Integer.parseInt(params.getOptionValue(SNAPSHOT_MINUTES_OPTION,
                Integer.toString(config.snapshotMinutes)));
        config.timeoutMinutes = Integer.parseInt(params.getOptionValue(TIMEOUT_OPTION,
                Integer.toString(config.timeoutMinutes)));
        config.name = params.getOptionValue(BOARD_NAME_OPTION, Objects.requireNonNullElse(config.name, ""));
        if (config.port < WebConsoleConfig.MIN_PORT || config.port > WebConsoleConfig.MAX_PORT) {
            throw new IllegalArgumentException("Local Web Console port must be between "
                    + WebConsoleConfig.MIN_PORT + " and " + WebConsoleConfig.MAX_PORT);
        }
        if (config.snapshotMinutes < 1) {
            throw new IllegalArgumentException("Local Web Console history minutes must be greater than zero");
        }
        if (config.timeoutMinutes < 1) {
            throw new IllegalArgumentException("Local Web Console idle timeout minutes must be greater than zero");
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
     * @throws IOException if the web console cannot register this benchmark run
     */
    public void open(String source, String storage, Action action, TimeUnit timeUnit, double[] percentiles)
            throws IOException {
        ensureConfig();
        this.percentiles = percentiles.clone();
        this.runId = UUID.randomUUID().toString();
        final String version = Objects.requireNonNullElse(Sbk.class.getPackage().getImplementationVersion(), "dev");
        final String boardName = resolveBoardName(config.name, source, storage);
        final WebConsoleRun run = new WebConsoleRun(runId, boardName, source, storage, action.name(),
                timeUnit.name(), version, System.getProperty("java.runtime.version"), System.currentTimeMillis());
        try {
            client = WebConsoleClient.connect(config, run);
            logRunLinks(client);
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
        return new String[]{"-" + PORT_OPTION, "-" + OPEN_OPTION, "-" + SNAPSHOT_MINUTES_OPTION,
                "-" + TIMEOUT_OPTION, "-" + BOARD_NAME_OPTION};
    }

    /**
     * Returns the current web console option/value pairs for forwarding to SBM.
     *
     * @return parsed web console arguments
     */
    public String[] getParsedArgs() {
        ensureConfig();
        return new String[]{"-" + PORT_OPTION, Integer.toString(config.port), "-" + OPEN_OPTION,
                Boolean.toString(config.open), "-" + SNAPSHOT_MINUTES_OPTION,
                Integer.toString(config.snapshotMinutes),
                "-" + TIMEOUT_OPTION, Integer.toString(config.timeoutMinutes),
                "-" + BOARD_NAME_OPTION, Objects.requireNonNullElse(config.name, "")};
    }

    @Override
    public void close() {
        if (client != null) {
            client.close();
            logRunLinks(client);
            client = null;
        }
    }

    private static void logRunLinks(WebConsoleClient webConsoleClient) {
        webConsoleClient.getRunLinks().forEach(link -> Printer.log.info("SBK Web Console ({}): {}",
                link.label(), link.uri()));
    }

    static String resolveBoardName(String configuredName, String source, String storage) {
        return configuredName == null || configuredName.isBlank()
                ? source + " " + storage : configuredName;
    }

    private static WebConsoleConfig loadConfig() {
        return WebConsoleConfig.load();
    }

    private void ensureConfig() {
        if (config == null) {
            config = loadConfig();
        }
    }
}
