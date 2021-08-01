/**
 * Copyright (c) KMG. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 */

package io.sbk.perl.impl;

import io.sbk.perl.LatencyPercentiles;
import io.sbk.perl.LatencyRecord;
import io.sbk.perl.LatencyRecordWindow;
import io.sbk.perl.ReportLatencies;
import io.sbk.perl.PerlConfig;
import io.sbk.perl.Time;

import javax.annotation.concurrent.NotThreadSafe;
import java.util.HashMap;
import java.util.Iterator;


/**
 *  class for Performance statistics.
 */
@NotThreadSafe
public class HashMapLatencyRecorder extends LatencyRecordWindow {
    final public HashMap<Long, Long> latencies;
    final public int maxHashMapSizeMB;
    final public long maxHashMapSizeBytes;
    final public int incBytes;
    public long hashMapBytesCount;

    public HashMapLatencyRecorder(long lowLatency, long highLatency, long totalLatencyMax, long totalRecordsMax, long bytesMax,
                                  double[] percentiles, Time time, int maxHashMapSizeMB) {
        super(lowLatency, highLatency, totalLatencyMax, totalRecordsMax, bytesMax, percentiles, time);
        this.latencies = new HashMap<>();
        this.maxHashMapSizeMB = maxHashMapSizeMB;
        this.maxHashMapSizeBytes = ((long) maxHashMapSizeMB) * PerlConfig.BYTES_PER_MB;
        this.incBytes = PerlConfig.LATENCY_VALUE_SIZE_BYTES * 2;
        this.hashMapBytesCount = 0;
    }


    @Override
    final public void reset(long startTime) {
        super.reset(startTime);
        this.latencies.clear();
        this.hashMapBytesCount = 0;
    }

    @Override
    final public boolean isOverflow() {
        return (this.hashMapBytesCount > this.maxHashMapSizeBytes ) || super.isOverflow();
    }

    @Override
    final public void copyPercentiles(LatencyPercentiles percentiles, ReportLatencies copyLatencies) {
        long cur = 0;
        int index = 0;

        if (copyLatencies != null) {
            copyLatencies.reportLatencyRecord(this);
        }

        for (int i = 0; i < percentiles.fractions.length; i++) {
            percentiles.indexes[i] = (long) (validLatencyRecords * percentiles.fractions[i]);
            percentiles.latencies[i] = 0;
        }

        Iterator<Long> keys =  latencies.keySet().stream().sorted().iterator();
        percentiles.minLatency = 0;
        percentiles.maxLatency = 0;
        boolean first = true;
        while (keys.hasNext()) {
            final long latency  = keys.next();
            final long count = latencies.get(latency);
            final long next =  cur + count;

            if (copyLatencies != null) {
                copyLatencies.reportLatency(latency, count);
            }
            if (first) {
                first = false;
                percentiles.minLatency = latency;
            }
            percentiles.maxLatency = latency;
            while (index < percentiles.indexes.length) {
                if (percentiles.indexes[index] >= cur && percentiles.indexes[index] <  next) {
                    percentiles.latencies[index] = latency;
                    index += 1;
                } else {
                    break;
                }
            }
            cur = next;
            latencies.remove(latency);
        }
        hashMapBytesCount = 0;
    }



    @Override
    final public void reportLatencyRecord(LatencyRecord record) {
        super.updateRecord(record);
    }


    @Override
    final public void reportLatency(long latency, long count) {
        Long val = latencies.get(latency);
        if (val == null) {
            val = 0L;
            hashMapBytesCount += incBytes;
        }
        latencies.put(latency, val + count);
    }

    /**
     * Record the latency.
     *  @param startTime start time.
     * @param bytes number of bytes.
     * @param events number of events(records).
     * @param latency latency value in milliseconds.
     */
    @Override
    final public void recordLatency(long startTime, int bytes, int events, long latency) {
        if (record(bytes, events, latency)) {
            reportLatency(latency, events);
        }
    }


}
