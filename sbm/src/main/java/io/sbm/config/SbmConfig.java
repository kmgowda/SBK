/**
 * Copyright (c) KMG. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.sbm.config;

import io.perl.config.LatencyConfig;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.dataformat.javaprop.JavaPropsFactory;

import java.io.IOException;
import java.io.InputStream;

/**
 * Class RamConfig.
 */
final public class SbmConfig extends LatencyConfig {
    /**
     * <code>String NAME = "sbm"</code>.
     */
    public final static String NAME = "sbm";

    /**
     * <code>String DESC = "Storage Benchmark Monitorr"</code>.
     */
    final public static String DESC = "Storage Benchmark Monitor";

    /**
     * <code>SBM_LOGGER_PACKAGE_NAME = "io.sbm.logger";</code>.
     */
    final public static String SBM_LOGGER_PACKAGE_NAME = "io.sbm.logger";
    private static final int MIN_PORT = 1;
    private static final int MAX_PORT = 65535;
    private static final String CONFIG_FILE = "sbm.properties";
    private static final SbmConfig INSTANCE = loadConfig();

    /**
     * <code>int port</code>.
     */
    public int port;
    /**
     * <code>int maxConnections</code>.
     */
    public int maxConnections;
    /**
     * <code>int maxQueues</code>.
     */
    public int maxQueues;
    /**
     * <code>int idleMS</code>.
     */
    public int idleMS;
    /**
     * Maximum inbound SBP latency-record size in MiB.
     */
    public int maxRecordSizeMB;

    /** Default benchmark action selector. */
    public String defaultAction;

    /**
     * Creates an empty SBM configuration for property binding.
     */
    public SbmConfig() {
    }

    /**
     * Returns the validated bundled SBM configuration.
     *
     * @return shared SBM configuration
     */
    public static SbmConfig get() {
        return INSTANCE;
    }

    private static SbmConfig loadConfig() {
        try (InputStream input = SbmConfig.class.getClassLoader().getResourceAsStream(CONFIG_FILE)) {
            if (input == null) {
                throw new IOException("Missing " + CONFIG_FILE);
            }
            final SbmConfig config = new ObjectMapper(new JavaPropsFactory()).readValue(input, SbmConfig.class);
            config.validate();
            return config;
        } catch (IOException exception) {
            throw new ExceptionInInitializerError(exception);
        }
    }

    private void validate() {
        if (port < MIN_PORT || port > MAX_PORT || maxConnections < 1 || maxQueues < 1 || idleMS < 0
                || maxRecordSizeMB < 1 || maxArraySizeMB < 1 || maxHashMapSizeMB < 1
                || totalMaxHashMapSizeMB < 1 || defaultAction == null
                || !defaultAction.matches("(?i)(r|w|wr|wro|rw|rwo)")) {
            throw new IllegalArgumentException("Invalid SBM defaults in " + CONFIG_FILE);
        }
    }

}
