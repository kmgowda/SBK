/**
 * Copyright (c) KMG. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 */
package io.perl.config;

import tools.jackson.databind.ObjectMapper;
import tools.jackson.dataformat.javaprop.JavaPropsFactory;
import io.time.Time;

import java.io.IOException;
import java.io.InputStream;
import java.util.Objects;

/**
 * Class PerlConfig extending LatencyConfig.
 *
 * PerL runtime configuration that augments {@link LatencyConfig} with worker/queue
 * parameters and timing defaults used by the PerL benchmark/runtime. Instances are
 * typically populated from the {@code perl.properties} file on the classpath via
 * the static {@link #build()} helpers.
 */
final public class PerlConfig extends LatencyConfig {

    /**
     * <code>String NAME = "PerL"</code>.
     */
    final public static String NAME = "PerL";
    /**
     * Default interval (in seconds) between periodic status/metrics prints.
     * <code>int DEFAULT_PRINTING_INTERVAL_SECONDS = 5</code>.
     */
    final public static int DEFAULT_PRINTING_INTERVAL_SECONDS = 5;

    /**
     * Default timeout (in milliseconds) for internal operations.
     * <code>int DEFAULT_TIMEOUT_MS = Time.MS_PER_SEC</code>.
     */
    final public static int DEFAULT_TIMEOUT_MS = Time.MS_PER_SEC;

    /**
     * Default maximum runtime (in seconds) if not otherwise limited.
     * <code>long DEFAULT_RUNTIME_SECONDS = Long.MAX_VALUE / Time.MS_PER_SEC</code>.
     */
    final public static long DEFAULT_RUNTIME_SECONDS = Long.MAX_VALUE / Time.MS_PER_SEC;

    /**
     * Minimum number of worker threads allowed.
     * <code>int MIN_WORKERS = 1</code>.
     */
    final public static int MIN_WORKERS = 1;

    /**
     * Minimum number of queues per worker.
     * <code>int MIN_Q_PER_WORKER = 3</code>.
     */
    final public static int MIN_Q_PER_WORKER = 3;

    /**
     * Minimum idle time (in nanoseconds) used to yield between operations.
     * <code>int MIN_IDLE_NS = Time.NS_PER_MICRO</code>.
     */
    final public static int MIN_IDLE_NS = Time.NS_PER_MICRO;


    /**
     * Default configuration file name from which to load PerL settings.
     * <code>String CONFIGFILE = "perl.properties"</code>.
     */
    final private static String CONFIGFILE = "perl.properties";

    /**
     * Number of worker threads to run.
     * <code>int workers</code>.
     */
    public int workers;

    /**
     * Number of queues assigned per worker.
     * <code>int qPerWorker</code>.
     */
    public int qPerWorker;

    /**
     * Idle time between operations (in nanoseconds).
     * <code>int idleNS</code>.
     */
    public int idleNS;

    /**
     * Sleep interval between loops (in milliseconds), when applicable.
     * <code>int sleepMS</code>.
     */
    public int sleepMS;

    /**
     * Maximum number of queues allowed.
     * <code>int maxQs</code>.
     */
    public int maxQs;


    /**
     * Build configuration by loading the default {@code perl.properties} from the classpath.
     *
     * @return PerlConfig - build(PerlConfig.class.getClassLoader().getResourceAsStream(CONFIGFILE));
     * @throws IOException  If it occurs.
     */
    public static PerlConfig build() throws IOException {
        return build(PerlConfig.class.getClassLoader().getResourceAsStream(CONFIGFILE));
    }

    /**
     * Build configuration by reading properties from the given input stream.
     *
     * @param in            InputStream
     * @return PerlConfig - mapper.readValue(Objects.requireNonNull(in), PerlConfig.class);
     * @throws IOException  If it occurs.
     */
    public static PerlConfig build(InputStream in) throws IOException {
        final ObjectMapper mapper = new ObjectMapper(new JavaPropsFactory());
        return mapper.readValue(Objects.requireNonNull(in), PerlConfig.class);
    }

}
