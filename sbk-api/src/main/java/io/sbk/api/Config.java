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
    final public static String SBK_MS_NAME = "ms";
    final public static String SBK_NS_NAME = "ns";
    final public static String SBK_MCS_NAME = "mcs";

    final public static int NS_PER_MICRO = 1000;
    final public static int MICROS_PER_MS = 1000;
    final public static int MS_PER_SEC = 1000;
    final public static int MIN_REPORTING_INTERVAL_MS = 5000;
    final public static int MIN_Q_PER_WORKER = 1;
    final public static int DEFAULT_MIN_LATENCY = 0;
    final public static int MS_PER_MIN = MS_PER_SEC * 60;
    final public static int DEFAULT_MAX_LATENCY = MS_PER_MIN * 15;
    final public static int NS_PER_MS = NS_PER_MICRO * MICROS_PER_MS;
    final public static int MIN_IDLE_NS = NS_PER_MICRO;
    final public static int DEFAULT_WINDOW_LATENCY = MS_PER_MIN;
    final public static long NS_PER_SEC = MS_PER_SEC * NS_PER_MS;
    final public static TimeUnit TIME_UNIT = TimeUnit.MILLISECONDS;
    final public static  double[] PERCENTILES = {0.1, 0.25, 0.5, 0.75, 0.95, 0.99, 0.999, 0.9999};


    public String packageName;
    public boolean fork;
    public TimeUnit timeUnit;
    public int reportingMS;
    public int qPerWorker;
    public int idleNS;
    public int maxQs;
    public int maxWindowLatency;
    public int minLatency;
    public int maxLatency;

    public static String timeUnitToString(TimeUnit timeUnit) {
        if (timeUnit == TimeUnit.NANOSECONDS) {
            return SBK_NS_NAME;
        } else if (timeUnit == TimeUnit.MICROSECONDS) {
            return SBK_MCS_NAME;
        }
        return SBK_MS_NAME;
    }

}
