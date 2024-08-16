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

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.javaprop.JavaPropsFactory;
import io.time.Time;

import java.io.IOException;
import java.io.InputStream;
import java.util.Objects;

/**
 * Class PerlConfig extending LatencyConfig.
 */
final public class PerlConfig extends LatencyConfig {

    /**
     * <code>String NAME = "PerL"</code>.
     */
    final public static String NAME = "PerL";


    /**
     * <code>int DEFAULT_PRINTING_INTERVAL_SECONDS = 5</code>.
     */
    final public static int DEFAULT_PRINTING_INTERVAL_SECONDS = 5;

    /**
     * <code>int DEFAULT_TIMEOUT_MS = Time.MS_PER_SEC</code>.
     */
    final public static int DEFAULT_TIMEOUT_MS = Time.MS_PER_SEC;

    /**
     * <code>long DEFAULT_RUNTIME_SECONDS = Long.MAX_VALUE / Time.MS_PER_SEC</code>.
     */
    final public static long DEFAULT_RUNTIME_SECONDS = Long.MAX_VALUE / Time.MS_PER_SEC;

    /**
     * <code>int MIN_WORKERS = 1</code>.
     */
    final public static int MIN_WORKERS = 1;

    /**
     * <code>int MIN_Q_PER_WORKER = 3</code>.
     */
    final public static int MIN_Q_PER_WORKER = 3;

    /**
     * <code>int MIN_IDLE_NS = Time.NS_PER_MICRO</code>.
     */
    final public static int MIN_IDLE_NS = Time.NS_PER_MICRO;


    /**
     * <code>String CONFIGFILE = "perl.properties"</code>.
     */
    final private static String CONFIGFILE = "perl.properties";

    /**
     * <code>int workers</code>.
     */
    public int workers;

    /**
     * <code>int qPerWorker</code>.
     */
    public int qPerWorker;

    /**
     * <code>int idleNS</code>.
     */
    public int idleNS;

    /**
     * <code>int sleepMS</code>.
     */
    public int sleepMS;

    /**
     * <code>int maxQs</code>.
     */
    public int maxQs;


    /**
     * Method build with no arguments.
     *
     * @return PerlConfig - build(PerlConfig.class.getClassLoader().getResourceAsStream(CONFIGFILE));
     * @throws IOException  If it occurs.
     */
    public static PerlConfig build() throws IOException {
        return build(PerlConfig.class.getClassLoader().getResourceAsStream(CONFIGFILE));
    }

    /**
     * Method build with arguments.
     *
     * @param in            InputStream
     * @return PerlConfig - mapper.readValue(Objects.requireNonNull(in), PerlConfig.class);
     * @throws IOException  If it occurs.
     */
    public static PerlConfig build(InputStream in) throws IOException {
        final ObjectMapper mapper = new ObjectMapper(new JavaPropsFactory())
                .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        return mapper.readValue(Objects.requireNonNull(in), PerlConfig.class);
    }

}
