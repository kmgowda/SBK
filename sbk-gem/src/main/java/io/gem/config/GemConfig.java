/**
 * Copyright (c) KMG. All Rights Reserved..
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.gem.config;

import tools.jackson.databind.ObjectMapper;
import tools.jackson.dataformat.javaprop.JavaPropsFactory;

import java.io.IOException;
import java.io.InputStream;

/**
 * Configuration properties for SBK-GEM orchestration.
 *
 * <p>Values are typically loaded from {@code gem.properties} and may be overridden via
 * command-line parameters. Contains defaults for SSH connection, remote directory
 * management, and SBM coordination.
 */
final public class GemConfig {
    /** Marker inserted between retained diagnostic prefix and suffix text. */
    public static final String DIAGNOSTIC_TRUNCATION_MARKER = " ... [truncated] ... ";
    /** Default maximum stdout/stderr bytes retained per SSH command. */
    public static final int DEFAULT_DIAGNOSTIC_BYTES = 262_144;
    /** Default maximum characters retained in a diagnostic summary. */
    public static final int DEFAULT_MAXIMUM_DIAGNOSTIC_CHARACTERS = 512;
    /** Default diagnostic prefix retained before the truncation marker. */
    public static final int DEFAULT_DIAGNOSTIC_PREFIX_CHARACTERS = 320;
    /**
     *<code>String SBK_GEM_APP_NAME = "sbk.gem.applicationName</code>.
     */
    final public static String SBK_GEM_APP_NAME = "sbk.gem.applicationName";
    /**
     * <code>String NAME = "sbk-gem"</code>.
     */
    final public static String NAME = "sbk-gem";
    /**
     * <code>String DESC = "Storage Benchmark Kit - Group Execution Monitor"</code>.
     */
    final public static String DESC = "Storage Benchmark Kit - Group Execution Monitor";
    /**
     * <code>String BIN_DIR = "bin"</code>.
     */
    final public static String BIN_DIR = "bin";
    /** Standard SBK launcher relative to an installed distribution. */
    final public static String SBK_COMMAND = BIN_DIR + "/sbk";
    /**
     * <code>String LOCAL_HOST = "localhost"</code>.
     */
    final public static String LOCAL_HOST = "localhost";

    /**
     * Default logger package to scan for {@code GemLogger} implementations.
     */
    final public static String SBK_GEM_LOGGER_PACKAGE_NAME = "io.gem.logger";

    /**
     * Environment variable name from which to read the SSH password if not provided in properties.
     */
    final public static String SBK_GEM_SSH_PASSWD = "SBK_GEM_SSH_PASSWD";

    /**
     * Command-line option name used to supply the SSH password.
     */
    final public static String GEM_PASS_OPTION = "gempass";
    private static final String CONFIG_FILE = "gem.properties";

    //override by props file or command line parameters
    /**
     * <code>String nodes</code>.
     */
    public String nodes;
    /**
     * SSH user name.
     */
    public String gemuser;
    /**
     * SSH password.
     */
    public String gempass;
    /**
     * Whether remote SSH host keys must match an entry in {@link #knownhosts}.
     */
    public boolean hostkeycheck;
    /**
     * Optional known-hosts file path; an empty value uses the launching user's default file.
     */
    public String knownhosts;
    /**
     * SSH port.
     */
    public int gemport;
    /** Local SBK application directory resolved from the generated launcher's {@code sbk.appHome}. */
    public String sbkdir;
    /**
     * Expected SBK version, discovered from the local SBK distribution under {@link #sbkdir}.
     */
    public String sbkVersion;
    /**
     * Optional remote Java home containing {@code bin/java}.
     */
    public String javadir;
    /** Whether inactive non-current managed runtimes and local cached bundles are removed automatically. */
    public boolean runtimecleanup;


    //override by props file
    /**
     * Timeout value used for remote operations (seconds).
     */
    public long remoteTimeoutSeconds;
    /** Maximum runtime bundle creation, transfer, and activation duration in seconds. */
    public long deploymentTimeoutSeconds;
    /** Interval between runtime preparation and transfer progress messages. */
    public int runtimeProgressIntervalSeconds;
    /** Local content-addressed runtime bundle cache directory, absolute or relative to the user's home. */
    public String runtimeCacheDirectory;
    /** Maximum wait for the remote managed-runtime lifecycle lock. */
    public long runtimeManagementLockTimeoutSeconds;
    /** Age after which an abandoned remote managed-runtime lifecycle lock may be reclaimed. */
    public long runtimeManagementLockStaleSeconds;
    /** Age after which an unlaunched runtime lease reservation may be reclaimed. */
    public long runtimeLeaseReservationSeconds;
    /**
     * Maximum time to wait for all remote SBK clients to register with SBM (seconds).
     */
    public long sbmRegistrationTimeoutSeconds;
    /**
     * Per-iteration wait timeout used during async joins (seconds).
     */
    public int timeoutSeconds;
    /**
     * Maximum number of iterations to wait/retry for remote operations.
     */
    public int maxIterations;
    /**
     * Remote working directory on each host (derived from app name/version).
     */
    public String remoteDir;
    /** Maximum platform threads used for concurrent SSH connection and control operations. */
    public int controlExecutorThreads;
    /** Maximum platform threads used for concurrent SFTP deployment transfers. */
    public int transferExecutorThreads;
    /** Maximum stdout/stderr bytes retained per SSH command. */
    public int diagnosticBytes;
    /** Maximum accepted OpenSSH-agent response frame bytes. */
    public int maximumAgentResponseBytes;
    /** Maximum characters retained in a summarized remote diagnostic. */
    public int maximumDiagnosticCharacters;
    /** Prefix characters retained when a remote diagnostic is truncated. */
    public int diagnosticPrefixCharacters;

    /**
     * Creates an empty GEM configuration for property binding.
     */
    public GemConfig() {
    }

    /**
     * Loads the module-owned GEM configuration.
     *
     * @return GEM configuration
     * @throws IOException if the bundled configuration cannot be read
     */
    public static GemConfig load() throws IOException {
        try (InputStream input = GemConfig.class.getClassLoader().getResourceAsStream(CONFIG_FILE)) {
            if (input == null) {
                throw new IOException("Missing " + CONFIG_FILE);
            }
            final GemConfig config = new ObjectMapper(new JavaPropsFactory()).readValue(input, GemConfig.class);
            config.validate();
            return config;
        }
    }

    void validate() {
        if (remoteTimeoutSeconds < 1 || deploymentTimeoutSeconds < 1
                || runtimeProgressIntervalSeconds < 1
                || runtimeCacheDirectory == null || runtimeCacheDirectory.isBlank()
                || controlExecutorThreads < 1 || transferExecutorThreads < 1
                || runtimeManagementLockTimeoutSeconds < 1 || runtimeManagementLockStaleSeconds < 1
                || runtimeManagementLockStaleSeconds <= deploymentTimeoutSeconds
                || runtimeLeaseReservationSeconds
                <= runtimeManagementLockTimeoutSeconds + remoteTimeoutSeconds
                || diagnosticBytes < 1 || maximumAgentResponseBytes < 1
                || maximumDiagnosticCharacters < 1 || diagnosticPrefixCharacters < 1
                || maximumDiagnosticCharacters - diagnosticPrefixCharacters
                <= DIAGNOSTIC_TRUNCATION_MARKER.length()) {
            throw new IllegalArgumentException("Invalid SBK-GEM runtime configuration");
        }
    }

}
