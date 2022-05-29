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

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.time.Time;

/**
 * Class LatencyConfig.
 */
@SuppressFBWarnings("MS_MUTABLE_ARRAY")
public class LatencyConfig {

    /**
     * <code>PERCENTILE_FORMAT = "0.##"</code>.
     */
    final public static String PERCENTILE_FORMAT = "0.##";

    /**
     * <code>LATENCY_VALUE_SIZE_BYTES = 8</code>.
     */
    final public static int LATENCY_VALUE_SIZE_BYTES = 8;

    /**
     * <code>LONG_MAX = Long.MAX_VALUE >> 2</code>.
     */
    final public static long LONG_MAX = Long.MAX_VALUE >> 2;

    /**
     * <code>TOTAL_LATENCY_MAX = Long.MAX_VALUE >> 1</code>.
     */
    final public static long TOTAL_LATENCY_MAX = Long.MAX_VALUE >> 1;

    /**
     * <code>DEFAULT_MAX_LATENCY = Time.MS_PER_MIN * 3</code>.
     */
    final public static int DEFAULT_MAX_LATENCY = Time.MS_PER_MIN * 3;

    /**
     * <code>int DEFAULT_MIN_LATENCY = 0</code>.
     */
    final public static int DEFAULT_MIN_LATENCY = 0;

    /**
     * <code>PERCENTILES = {10, 25, 50, 75, 95, 99, 99.9, 99.99}</code>.
     */
    final public static double[] PERCENTILES = {10, 25, 50, 75, 95, 99, 99.9, 99.99};

    /**
     * <code>int HDR_SIGNIFICANT_DIGITS = 3</code>.
     */
    final public static int HDR_SIGNIFICANT_DIGITS = 3;


    /**
     * <code>int maxArraySizeMB</code>.
     */
    public int maxArraySizeMB;

    /**
     * <code>int maxHashMapSizeMB</code>.
     */
    public int maxHashMapSizeMB;

    /**
     * <code>int totalMaxHashMapSizeMB</code>.
     */
    public int totalMaxHashMapSizeMB;

    /**
     * <code>boolean histogram</code>.
     */
    public boolean histogram;

    /**
     * <code>boolean csv</code>.
     */
    public boolean csv;

    /**
     * <code>int csvFileSizeGB</code>.
     */
    public int csvFileSizeGB;
}

