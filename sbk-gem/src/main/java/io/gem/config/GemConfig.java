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

/**
 * Configuration properties for SBK-GEM orchestration.
 *
 * <p>Values are typically loaded from {@code gem.properties} and may be overridden via
 * command-line parameters. Contains defaults for SSH connection, remote directory
 * management, and SBM coordination.
 */
final public class GemConfig {
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
     * SSH port.
     */
    public int gemport;
    /**
     * Local SBK application directory.
     */
    public String sbkdir;
    /**
     * SBK launcher command relative to {@link #sbkdir}.
     */
    public String sbkcommand;
    /**
     * Whether to copy SBK to remote nodes before running.
     */
    public boolean copy;
    /**
     * Whether to delete SBK from remote nodes after run.
     */
    public boolean delete;


    //override by props file
    /**
     * Timeout value used for remote operations (seconds).
     */
    public long remoteTimeoutSeconds;
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
    /**
     * Whether to fork a {@code ForkJoinPool} for execution (vs fixed thread pool).
     */
    public boolean fork;
}
