/**
 * Copyright (c) KMG. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 */

package io.sbk.api.impl;

import io.sbk.api.CloneLatencies;
import io.sbk.api.Time;
import javax.annotation.concurrent.NotThreadSafe;
import java.util.HashMap;
import java.util.Iterator;


/**
 *  class for Performance statistics.
 */
@NotThreadSafe
public class HashMapLatencyRecorder extends LatencyWindow {
    final private HashMap<Long, Long> latencies;

    HashMapLatencyRecorder(long lowLatency, long highLatency, long totalLatencyMax, long totalRecordsMax, long bytesMax,
                           double[] percentiles, Time time) {
        super(lowLatency, highLatency, totalLatencyMax, totalRecordsMax, bytesMax, percentiles, time);
        this.latencies = new HashMap<>();
    }

    @Override
    public long[] getPercentiles(CloneLatencies copyLatencies) {
        final long[] values = new long[percentiles.length];
        final long[] percentileIds = new long[percentiles.length];
        long cur = 0;
        int index = 0;

        if (copyLatencies != null) {
            copyLatencies.updateLatencyRecords(this);
        }

        for (int i = 0; i < percentileIds.length; i++) {
            percentileIds[i] = (long) (validLatencyRecords * percentiles[i]);
        }

        Iterator<Long> keys =  latencies.keySet().stream().sorted().iterator();
        while (keys.hasNext()) {
            final long key  = keys.next();
            final long val = latencies.get(key);
            final long next =  cur + val;

            if (copyLatencies != null) {
                copyLatencies.copyLatency( key, val);
            }

            while (index < values.length) {
                if (percentileIds[index] >= cur && percentileIds[index] <  next) {
                    values[index] = key;
                    index += 1;
                } else {
                    break;
                }
            }
            cur = next;
            latencies.remove(key);
        }

        return values;
    }

    /**
     * Record the latency.
     *
     * @param startTime start time.
     * @param bytes number of bytes.
     * @param events number of events(records).
     * @param latency latency value in milliseconds.
     */
    @Override
    public void record(long startTime, int bytes, int events, long latency) {
        if (record(bytes, events, latency)) {
            latencies.put(latency, latencies.getOrDefault(latency, 0L) + events);
        }
    }
}
