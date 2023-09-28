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
 * Class GemConfig.
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
     *<code>String SBK_GEM_LOGGER_PACKAGE_NAME = "io.sbm.logger"</code>.
     */
    final public static String SBK_GEM_LOGGER_PACKAGE_NAME = "io.gem.logger";

    //override by props file or command line parameters
    /**
     * <code>String nodes</code>.
     */
    public String nodes;
    /**
     * <code>String gemuse</code>.
     */
    public String gemuser;
    /**
     * <code>String gempass</code>.
     */
    public String gempass;
    /**
     * <code>int gemport</code>.
     */
    public int gemport;
    /**
     * <code>String sbkdir</code>.
     */
    public String sbkdir;
    /**
     * <code>String sbkcommand</code>.
     */
    public String sbkcommand;
    /**
     * <code>boolean copy</code>.
     */
    public boolean copy;
    /**
     * <code>boolean delete</code>.
     */
    public boolean delete;


    //override by props file
    /**
     * <code>long remoteTimeoutSeconds</code>.
     */
    public long remoteTimeoutSeconds;
    /**
     * <code>int timeoutSeconds</code>.
     */
    public int timeoutSeconds;
    /**
     * <code>int maxIterations</code>.
     */
    public int maxIterations;
    /**
     * <code>String remoteDir</code>.
     */
    public String remoteDir;
    /**
     * <code>boolean fork</code>.
     */
    public boolean fork;
}
