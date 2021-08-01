/**
 * Copyright (c) KMG. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 */

package io.sbk.perl;

import javax.annotation.concurrent.NotThreadSafe;

/**
 *  Base class for Performance statistics.
 */
@NotThreadSafe
public class LatencyRecorder extends LatencyRecord {
    final public long lowLatency;
    final public long highLatency;
    final public long totalLatencyMax;
    final public long totalRecordsMax;
    final public long totalBytesMax;

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
    public boolean isOverflow() {
        return (totalLatency > totalLatencyMax) || (totalBytes > totalBytesMax)
                || (totalRecords > totalRecordsMax);

    }

    /**
     * Add the record.
     *
     * @param record Latency record
     */
    final public void updateRecord(LatencyRecord record) {
        this.totalRecords += record.totalRecords;
        this.totalLatency += record.totalLatency;
        this.totalBytes += record.totalBytes;
        this.invalidLatencyRecords += record.invalidLatencyRecords;
        this.lowerLatencyDiscardRecords += record.lowerLatencyDiscardRecords;
        this.higherLatencyDiscardRecords += record.higherLatencyDiscardRecords;
        this.validLatencyRecords += record.validLatencyRecords;
        this.maxLatency = Math.max(this.maxLatency, record.maxLatency);
        this.minValidLatency = Math.min(this.minValidLatency, record.minValidLatency);
        this.maxValidLatency = Math.max(this.maxValidLatency, record.maxValidLatency);
    }

    /**
     * Record the latency and return if the latency is valid or not.
     *
     * @param bytes number of bytes.
     * @param events number of events(records).
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
            this.totalLatency +=  latency * events;
            if (latency < this.lowLatency) {
                this.lowerLatencyDiscardRecords += events;
            } else if (latency > this.highLatency) {
                this.higherLatencyDiscardRecords += events;
            } else {
                this.validLatencyRecords += events;
                this.minValidLatency = Math.min(this.minValidLatency, latency);
                this.maxValidLatency = Math.max(this.maxValidLatency, latency);
                return true;
            }
        }
        return false;
    }
}