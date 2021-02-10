/**
 * Copyright (c) KMG. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 */

package io.sbk.api;

import java.util.concurrent.TimeUnit;

public class Config {
    final public static String NAME = "sbk";
    final public static String DESC = "Storage Benchmark Kit";
    final public static String SBK_APP_NAME = "sbk.applicationName";
    final public static String SBK_CLASS_NAME = "sbk.className";
    final public static String SBK_PERCENTILE_FORMAT = "0.##";

    final public static int MIN_DATA_RW_SIZE = DataType.TIME_HEADER_BYTES + 2;

    final public static int LATENCY_VALUE_SIZE_BYTES = 8;
    final public static int BYTES_PER_KB = 1024;
    final public static int BYTES_PER_MB = BYTES_PER_KB * BYTES_PER_KB;

    final public static int NS_PER_MICRO = 1000;
    final public static int MICROS_PER_MS = 1000;
    final public static int MS_PER_SEC = 1000;
    final public static int MICROS_PER_SEC = MICROS_PER_MS * MS_PER_SEC;
    final public static int DEFAULT_REPORTING_INTERVAL_SECONDS = 5000;
    final public static int DEFAULT_TIMEOUT_MS = MS_PER_SEC;
    final public static long DEFAULT_RUNTIME_SECONDS = Long.MAX_VALUE / MS_PER_SEC;
    final public static long LONG_MAX = Long.MAX_VALUE / 256;

    final public static int MIN_Q_PER_WORKER = 1;

    final public static int MS_PER_MIN = MS_PER_SEC * 60;
    final public static int NS_PER_MS = NS_PER_MICRO * MICROS_PER_MS;
    final public static int MIN_IDLE_NS = NS_PER_MICRO;
    final public static int DEFAULT_MIN_LATENCY = 0;
    final public static int DEFAULT_MAX_LATENCY = MS_PER_MIN * 3;
    final public static long NS_PER_SEC = MS_PER_SEC * NS_PER_MS;
    final public static  double[] PERCENTILES = {10, 25, 50, 75, 95, 99, 99.9, 99.99};



    public String packageName;
    public boolean fork;
    public TimeUnit timeUnit;
    public int qPerWorker;
    public int idleNS;
    public int maxQs;
    public int maxArraySizeMB;
    public int maxHashMapSizeMB;
    public boolean csv;
}
