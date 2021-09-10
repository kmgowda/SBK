/**
 * Copyright (c) KMG. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.sbk.perl;

import javax.annotation.concurrent.NotThreadSafe;

/**
 * Base class for Performance statistics.
 */
@NotThreadSafe
public class LatencyRecorder extends LatencyRecord {
    final protected long lowLatency;
    final protected long highLatency;
    final protected long totalLatencyMax;
    final protected long totalRecordsMax;
    final protected long totalBytesMax;

    public LatencyRecorder(long baseLatency, long latencyThreshold, long totalLatencyMax, long totalRecordsMax, long totalBytesMax) {
        super();
        this.lowLatency = baseLatency;
        this.highLatency = latencyThreshold;
        this.totalLatencyMax = totalLatencyMax;
        this.totalRecordsMax = totalRecordsMax;
        this.totalBytesMax = totalBytesMax;
    }

    /**
     * is Overflow condition for this recorder.
     *
     * @return isOverflow condition occurred or not
     */
    final public boolean isOverflow() {
        return (totalLatency > totalLatencyMax) || (totalBytes > totalBytesMax)
                || (totalRecords > totalRecordsMax);

    }

    /**
     * Add the record.
     *
     * @param totalRecords                total records
     * @param totalLatency                total latency
     * @param totalBytes                  total bytes
     * @param invalidLatencyRecords       invalid latency records
     * @param lowerLatencyDiscardRecords  lower discarded latency records
     * @param higherLatencyDiscardRecords higher discarded latency records
     * @param validLatencyRecords         valid latency records
     * @param maxLatency                  Max latency
     */
    final public void update(long totalRecords, long totalLatency, long totalBytes,
                             long invalidLatencyRecords, long lowerLatencyDiscardRecords,
                             long higherLatencyDiscardRecords, long validLatencyRecords,
                             long maxLatency) {
        this.totalRecords += totalRecords;
        this.totalLatency += totalLatency;
        this.totalBytes += totalBytes;
        this.invalidLatencyRecords += invalidLatencyRecords;
        this.lowerLatencyDiscardRecords += lowerLatencyDiscardRecords;
        this.higherLatencyDiscardRecords += higherLatencyDiscardRecords;
        this.validLatencyRecords += validLatencyRecords;
        this.maxLatency = Math.max(this.maxLatency, maxLatency);
    }


    /**
     * Add the record.
     *
     * @param record Latency record
     */
    final public void update(LatencyRecord record) {
        update(record.totalRecords, record.totalLatency, record.totalBytes,
                record.invalidLatencyRecords, record.lowerLatencyDiscardRecords,
                record.higherLatencyDiscardRecords, record.validLatencyRecords, record.maxLatency);
    }

    /**
     * Record the latency and return if the latency is valid or not.
     *
     * @param bytes   number of bytes.
     * @param events  number of events(records).
     * @param latency latency value in milliseconds.
     * @return is valid latency record or not
     */
    final public boolean record(long bytes, long events, long latency) {
        this.totalBytes += bytes;
        this.totalRecords += events;
        this.maxLatency = Math.max(this.maxLatency, latency);
        if (latency < 0) {
            this.invalidLatencyRecords += events;
        } else {
            this.totalLatency += latency * events;
            if (latency < this.lowLatency) {
                this.lowerLatencyDiscardRecords += events;
            } else if (latency > this.highLatency) {
                this.higherLatencyDiscardRecords += events;
            } else {
                this.validLatencyRecords += events;
                return true;
            }
        }
        return false;
    }
}