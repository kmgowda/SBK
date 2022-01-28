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

import io.time.Time;

public class LatencyConfig {
    final public static String PERCENTILE_FORMAT = "0.##";
    final public static int LATENCY_VALUE_SIZE_BYTES = 8;
    final public static long LONG_MAX = Long.MAX_VALUE >> 2;
    final public static long TOTAL_LATENCY_MAX = Long.MAX_VALUE >> 1;
    final public static int DEFAULT_MAX_LATENCY = Time.MS_PER_MIN * 3;
    final public static int DEFAULT_MIN_LATENCY = 0;
    final public static double[] PERCENTILES = {10, 25, 50, 75, 95, 99, 99.9, 99.99};
    final public static int HDR_SIGNIFICANT_DIGITS = 3;

    public int maxArraySizeMB;
    public int maxHashMapSizeMB;
    public int totalMaxHashMapSizeMB;
    public boolean histogram;
    public boolean csv;
    public int csvFileSizeGB;
}

