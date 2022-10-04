/**
 * Copyright (c) KMG. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.perl.api.impl;

import io.time.Time;

import javax.annotation.concurrent.NotThreadSafe;
import java.util.HashMap;

/**
 * class for Performance statistics.
 */
@NotThreadSafe
final public class HashMapLatencyRecorder extends MapLatencyRecorder {

    /**
     * Constructor  HashMapLatencyRecorder initializing all values.
     *
     * @param lowLatency            long
     * @param highLatency           long
     * @param totalLatencyMax       long
     * @param totalRecordsMax       long
     * @param bytesMax              long
     * @param percentiles           double[]
     * @param time                  Time
     * @param maxHashMapSizeMB      int
     */
    public HashMapLatencyRecorder(long lowLatency, long highLatency, long totalLatencyMax, long totalRecordsMax, long bytesMax,
                                  double[] percentiles, Time time, int maxHashMapSizeMB) {
        super(new HashMap<>(), lowLatency, highLatency, totalLatencyMax, totalRecordsMax, bytesMax,
                percentiles, time, maxHashMapSizeMB);

    }

}
