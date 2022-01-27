/**
 * Copyright (c) KMG. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.perl;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.javaprop.JavaPropsFactory;
import io.time.Time;

import java.io.IOException;
import java.io.InputStream;
import java.util.Objects;

final public class PerlConfig {
    final public static String NAME = "PerL";
    final public static String PERCENTILE_FORMAT = "0.##";

    final public static int LATENCY_VALUE_SIZE_BYTES = 8;
    final public static int BYTES_PER_KB = 1024;
    final public static int BYTES_PER_MB = BYTES_PER_KB * BYTES_PER_KB;
    final public static long BYTES_PER_GB = ((long) BYTES_PER_MB) * BYTES_PER_MB;

    final public static int DEFAULT_REPORTING_INTERVAL_SECONDS = 5;
    final public static int DEFAULT_TIMEOUT_MS = Time.MS_PER_SEC;
    final public static long DEFAULT_RUNTIME_SECONDS = Long.MAX_VALUE / Time.MS_PER_SEC;

    final public static long LONG_MAX = Long.MAX_VALUE >> 2;
    final public static long TOTAL_LATENCY_MAX = Long.MAX_VALUE >> 1;

    final public static int MIN_WORKERS = 1;
    final public static int MIN_Q_PER_WORKER = 3;

    final public static int DEFAULT_MAX_LATENCY = Time.MS_PER_MIN * 3;
    final public static int MIN_IDLE_NS = Time.NS_PER_MICRO;
    final public static int DEFAULT_MIN_LATENCY = 0;
    final public static double[] PERCENTILES = {10, 25, 50, 75, 95, 99, 99.9, 99.99};

    final public static int HDR_SIGNIFICANT_DIGITS = 3;

    final private static String CONFIGFILE = "perl.properties";

    public int workers;
    public int qPerWorker;
    public int idleNS;
    public int maxQs;
    public int maxArraySizeMB;
    public int maxHashMapSizeMB;
    public int totalMaxHashMapSizeMB;
    public boolean histogram;
    public boolean csv;
    public int csvFileSizeGB;


    public static PerlConfig build() throws IOException {
        return build(PerlConfig.class.getClassLoader().getResourceAsStream(CONFIGFILE));
    }

    public static PerlConfig build(InputStream in) throws IOException {
        final ObjectMapper mapper = new ObjectMapper(new JavaPropsFactory())
                .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        return mapper.readValue(Objects.requireNonNull(in), PerlConfig.class);
    }

}
