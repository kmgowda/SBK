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
 *
 * Configuration constants and tunables for latency/percentile reporting used by
 * PerL components. Mutable fields are typically populated from configuration files
 * or command-line flags and control histogram/CSV outputs and memory limits for
 * latency storage.
 */
public class LatencyConfig {

    /**
     * Format string used when printing percentile values.
     * <code>PERCENTILE_FORMAT = "0.##"</code>.
     */
    final public static String PERCENTILE_FORMAT = "0.##";

    /**
     * Size in bytes of a single latency value (64-bit long).
     * <code>LATENCY_VALUE_SIZE_BYTES = 8</code>.
     */
    final public static int LATENCY_VALUE_SIZE_BYTES = 8;

    /**
     * Safety cap for individual latency counters to avoid overflow.
     * <code>LONG_MAX = Long.MAX_VALUE >> 2</code>.
     */
    final public static long LONG_MAX = Long.MAX_VALUE >> 2;

    /**
     * Safety cap for total latency accumulators to avoid overflow.
     * <code>TOTAL_LATENCY_MAX = Long.MAX_VALUE >> 1</code>.
     */
    final public static long TOTAL_LATENCY_MAX = Long.MAX_VALUE >> 1;

    /**
     * Default maximum latency expected (in milliseconds).
     * <code>DEFAULT_MAX_LATENCY = Time.MS_PER_MIN * 3</code>.
     */
    final public static int DEFAULT_MAX_LATENCY = Time.MS_PER_MIN * 3;

    /**
     * Default minimum latency expected (in milliseconds).
     * <code>int DEFAULT_MIN_LATENCY = 0</code>.
     */
    final public static int DEFAULT_MIN_LATENCY = 0;

    /**
     * Default percentiles to compute and report.
     * <code>PERCENTILES = {10, 25, 50, 75, 95, 99, 99.9, 99.99}</code>.
     */
    @SuppressFBWarnings("MS_MUTABLE_ARRAY")
    final public static double[] PERCENTILES = {10, 25, 50, 75, 95, 99, 99.9, 99.99};

    /**
     * Number of significant digits used by HDR histograms.
     * <code>int HDR_SIGNIFICANT_DIGITS = 3</code>.
     */
    final public static int HDR_SIGNIFICANT_DIGITS = 3;


    /**
     * Maximum memory allowed (in MB) for array-based latency storage.
     * <code>int maxArraySizeMB</code>.
     */
    public int maxArraySizeMB;

    /**
     * Maximum memory allowed (in MB) for HashMap-based latency storage.
     * <code>int maxHashMapSizeMB</code>.
     */
    public int maxHashMapSizeMB;

    /**
     * Global maximum memory allowed (in MB) for all HashMaps combined.
     * <code>int totalMaxHashMapSizeMB</code>.
     */
    public int totalMaxHashMapSizeMB;

    /**
     * Whether histogram output is enabled.
     * <code>boolean histogram</code>.
     */
    public boolean histogram;

    /**
     * Whether CSV output is enabled.
     * <code>boolean csv</code>.
     */
    public boolean csv;

    /**
     * Maximum size (in GB) for generated CSV files.
     * <code>int csvFileSizeGB</code>.
     */
    public int csvFileSizeGB;
}
