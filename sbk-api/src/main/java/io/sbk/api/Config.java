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

public class Config {
    final public static int NS_PER_MICRO = 1000;
    final public static int MICROS_PER_MS = 1000;
    final public static int MS_PER_SEC = 1000;
    final public static int NS_PER_MS = NS_PER_MICRO * MICROS_PER_MS;
    final public static int MS_PER_MIN = MS_PER_SEC * 60;
    final public static int MS_PER_HR = MS_PER_MIN * 60;
    final public static int MIN_REPORTING_INTERVAL_MS = 5000;
    final public static int MIN_IDLE_NS = MICROS_PER_MS;
    final public static int MIN_Q_PER_WORKER = 1;
    final public static int DEFAULT_WINDOW_LATENCY = MS_PER_MIN;
    final public static int DEFAULT_MAX_LATENCY = MS_PER_MIN * 15;

    public String name;
    public String description;
    public String packageName;
    public boolean fork;
    public int reportingMS;
    public int qPerWorker;
    public int idleNS;
    public int maxQs;
    public int maxWindowLatency;
    public int maxLatency;
}
