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

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.dataformat.javaprop.JavaPropsFactory;

import java.io.IOException;
import java.io.InputStream;

/**
 * Configuration for the SBK Local Web Console.
 */
public final class WebConsoleConfig {
    /** Lowest valid TCP port. */
    public static final int MIN_PORT = 1;
    /** Highest valid TCP port. */
    public static final int MAX_PORT = 65535;
    private static final String CONFIG_FILE = "webconsole.properties";
    /** TCP port used by the web console server. */
    public int port;
    /** Whether SBK should open the web console in the default browser. */
    public boolean open;
    /** Number of minutes of snapshots retained for each benchmark run. */
    public int snapshotMinutes;
    /** Number of idle minutes before the web console exits. */
    public int timeoutMinutes;
    /** Optional display name for the benchmark board. */
    @SuppressFBWarnings(value = "UUF_UNUSED_PUBLIC_OR_PROTECTED_FIELD",
            justification = "The sbk-api WebConsoleLoggerSupport adapter reads and writes this public configuration")
    public String name;
    /** Periodic benchmark reporting interval used to size retained history. */
    public int reportingIntervalSeconds;
    /** HTTP connection timeout in milliseconds. */
    public int connectTimeoutMillis;
    /** HTTP request timeout in milliseconds. */
    public int requestTimeoutMillis;
    /** Maximum wait for the publisher thread to finish during shutdown. */
    public int publisherShutdownTimeoutMillis;
    /** Child-server startup timeout in milliseconds. */
    public int startTimeoutMillis;
    /** Benchmark lease heartbeat interval in milliseconds. */
    public int leaseHeartbeatMillis;
    /** Publisher queue polling interval in milliseconds. */
    public int publisherPollMillis;
    /** Child-server startup polling interval in milliseconds. */
    public int startupPollMillis;
    /** Server-sent event heartbeat interval in milliseconds. */
    public int serverHeartbeatMillis;
    /** HTTP listener backlog. */
    public int httpBacklog;
    /** Server-sent event retry interval in milliseconds. */
    public int sseRetryMillis;
    /** Browser lease heartbeat interval in milliseconds. */
    public int browserHeartbeatMillis;
    /** Maximum snapshots retained by the browser. */
    public int browserSnapshotLimit;
    /** Maximum snapshots drawn in each chart. */
    public int chartSnapshotLimit;
    /** Browser run/history refresh interval in milliseconds. */
    public int refreshMillis;
    /** Background server log directory, relative to the user's home when not absolute. */
    public String logDirectory;

    /**
     * Creates an empty web console configuration for property binding.
     */
    public WebConsoleConfig() {
    }

    /**
     * Loads and validates the module-owned Web Console defaults.
     *
     * @return Web Console configuration
     * @throws IllegalArgumentException if the bundled configuration is missing or invalid
     */
    public static WebConsoleConfig load() {
        try (InputStream input = WebConsoleConfig.class.getClassLoader().getResourceAsStream(CONFIG_FILE)) {
            if (input == null) {
                throw new IOException("Missing " + CONFIG_FILE);
            }
            final WebConsoleConfig config = new ObjectMapper(new JavaPropsFactory())
                    .readValue(input, WebConsoleConfig.class);
            config.validate();
            return config;
        } catch (IOException exception) {
            throw new IllegalArgumentException("Unable to load " + CONFIG_FILE, exception);
        }
    }

    private void validate() {
        if (port < MIN_PORT || port > MAX_PORT || snapshotMinutes < 1 || timeoutMinutes < 1
                || reportingIntervalSeconds < 1 || connectTimeoutMillis < 1 || requestTimeoutMillis < 1
                || publisherShutdownTimeoutMillis < 1 || startTimeoutMillis < 1 || leaseHeartbeatMillis < 1
                || publisherPollMillis < 1
                || startupPollMillis < 1 || serverHeartbeatMillis < 1 || httpBacklog < 1
                || sseRetryMillis < 1 || browserHeartbeatMillis < 1 || browserSnapshotLimit < 1
                || chartSnapshotLimit < 1 || refreshMillis < 1 || logDirectory == null
                || logDirectory.isBlank()) {
            throw new IllegalArgumentException("Invalid SBK Web Console configuration");
        }
    }
}
