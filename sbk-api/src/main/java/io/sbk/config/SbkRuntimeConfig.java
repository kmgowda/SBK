/**
 * Copyright (c) KMG. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 */
package io.sbk.config;

import tools.jackson.databind.ObjectMapper;
import tools.jackson.dataformat.javaprop.JavaPropsFactory;

import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.TimeUnit;

/** Lifecycle and executor settings shared by the SBK benchmark runtime. */
public final class SbkRuntimeConfig {
    private static final String CONFIG_FILE = "sbk-runtime.properties";
    private static final SbkRuntimeConfig INSTANCE = loadConfig();

    /** Maximum graceful-cleanup period before application completion is forced. */
    public long forcedShutdownGraceSeconds;
    /** Portion of cleanup reserved for publishing final aggregate results. */
    public long finalResultPublicationMillis;
    /** Executor threads reserved for lifecycle and measurement tasks. */
    public int workerExecutorReserve;
    /** Parallelism of the PerL executor. */
    public int perlExecutorParallelism;
    /** Worker executor termination wait in seconds. */
    public long workerTerminationSeconds;
    /** JVM shutdown-hook cleanup timeout in seconds. */
    public long shutdownHookTimeoutSeconds;
    /** Default storage-operation timeout in milliseconds. */
    public int defaultOperationTimeoutMillis;

    /** Creates an empty configuration for properties binding. */
    public SbkRuntimeConfig() {
    }

    /**
     * Returns the validated bundled runtime configuration.
     *
     * @return shared runtime configuration
     */
    public static SbkRuntimeConfig get() {
        return INSTANCE;
    }

    private static SbkRuntimeConfig loadConfig() {
        try (InputStream input = SbkRuntimeConfig.class.getClassLoader().getResourceAsStream(CONFIG_FILE)) {
            if (input == null) {
                throw new IOException("Missing " + CONFIG_FILE);
            }
            final SbkRuntimeConfig config = new ObjectMapper(new JavaPropsFactory())
                    .readValue(input, SbkRuntimeConfig.class);
            config.validate();
            return config;
        } catch (IOException exception) {
            throw new ExceptionInInitializerError(exception);
        }
    }

    private void validate() {
        if (forcedShutdownGraceSeconds < 1 || finalResultPublicationMillis < 1
                || finalResultPublicationMillis >= TimeUnit.SECONDS.toMillis(forcedShutdownGraceSeconds)
                || workerExecutorReserve < 1
                || perlExecutorParallelism < 1 || workerTerminationSeconds < 1
                || shutdownHookTimeoutSeconds < 1 || defaultOperationTimeoutMillis < 1) {
            throw new IllegalArgumentException("All SBK runtime settings must be greater than zero");
        }
    }
}
