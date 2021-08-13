/**
 * Copyright (c) KMG. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 */

package io.sbk.config;

final public class RamConfig {
    public final static String NAME = "sbk-ram";
    final public static String DESC = "Storage Benchmark Kit - Results Aggregation Monitor";

    public int port;
    public int maxConnections;
    public int maxArraySizeMB;
    public int maxHashMapSizeMB;
    public int maxQueues;
    public int idleMS;
    public boolean histogram;
    public boolean csv;
    public int csvFileSizeMB;
}
