/**
 * Copyright (c) KMG. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.perl.api;

import javax.annotation.concurrent.NotThreadSafe;

/**
 * Recorder that accumulates latency statistics and enforces configured bounds
 * (min/max latencies and total thresholds). This class is intentionally
 * {@link NotThreadSafe} and is expected to be used by a single recorder
 * thread.
 *
 * <p>Units and behavior:
 * <ul>
 *     <li>Latency values passed to {@link #record(long, long, long)} are in the
 *     time unit used by the caller; callers should convert to the logger's
 *     time unit if necessary.</li>
 *     <li>{@link #isOverflow()} returns true when any of the configured total
 *     thresholds (latency sum, bytes, or records) is exceeded.</li>
 * </ul>
 */
@NotThreadSafe
public sealed class LatencyRecorder extends LatencyRecord permits LatencyWindow {
    /** Minimum accepted latency value (below this, samples are discarded). */
    final protected long lowLatency;
    /** Maximum accepted latency value (above this, samples are discarded). */
    final protected long highLatency;
    /** Maximum allowed cumulative latency before overflow. */
    final protected long totalLatencyMax;
    /** Maximum allowed total records before overflow. */
    final protected long totalRecordsMax;
    /** Maximum allowed total bytes before overflow. */
    final protected long totalBytesMax;

    /**
     * Construct a LatencyRecorder with configured thresholds.
     *
     * @param baseLatency      minimum valid latency
     * @param latencyThreshold maximum valid latency
     * @param totalLatencyMax  maximum cumulative latency
     * @param totalRecordsMax  maximum total records
     * @param totalBytesMax    maximum total bytes
     */
    public LatencyRecorder(long baseLatency, long latencyThreshold, long totalLatencyMax, long totalRecordsMax, long totalBytesMax) {
        super();
        this.lowLatency = baseLatency;
        this.highLatency = latencyThreshold;
        this.totalLatencyMax = totalLatencyMax;
        this.totalRecordsMax = totalRecordsMax;
        this.totalBytesMax = totalBytesMax;
    }

    /**
     * Checks whether any configured thresholds have been exceeded.
     *
     * @return true if overflow conditions occurred
     */
    final public boolean isOverflow() {
        return (totalLatency > totalLatencyMax) || (totalBytes > totalBytesMax)
                || (totalRecords > totalRecordsMax);

    }

    /**
     * Update the recorder counters by adding the provided values.
     *
     * @param totalRecords                 total number of records to add
     * @param totalLatency                 total latency to add
     * @param totalBytes                   total bytes to add
     * @param invalidLatencyRecords        number of invalid latency records to add
     * @param lowerLatencyDiscardRecords   number of lower-discarded records to add
     * @param higherLatencyDiscardRecords  number of higher-discarded records to add
     * @param validLatencyRecords          number of valid latency records to add
     * @param minLatency                   minimum latency observed (used to update min)
     * @param maxLatency                   maximum latency observed (used to update max)
     */
    final public void update(long totalRecords, long totalLatency, long totalBytes,
                             long invalidLatencyRecords, long lowerLatencyDiscardRecords,
                             long higherLatencyDiscardRecords, long validLatencyRecords,
                             long minLatency, long maxLatency) {
        this.totalRecords += totalRecords;
        this.totalLatency += totalLatency;
        this.totalBytes += totalBytes;
        this.invalidLatencyRecords += invalidLatencyRecords;
        this.lowerLatencyDiscardRecords += lowerLatencyDiscardRecords;
        this.higherLatencyDiscardRecords += higherLatencyDiscardRecords;
        this.validLatencyRecords += validLatencyRecords;
        this.minLatency = Math.min(this.minLatency, minLatency);
        this.maxLatency = Math.max(this.maxLatency, maxLatency);
    }


    /**
     * Update using another LatencyRecord instance.
     *
     * @param record LatencyRecord instance whose counters will be merged into this recorder
     */
    final public void update(LatencyRecord record) {
        update(record.totalRecords, record.totalLatency, record.totalBytes,
                record.invalidLatencyRecords, record.lowerLatencyDiscardRecords,
                record.higherLatencyDiscardRecords, record.validLatencyRecords,
                record.minLatency, record.maxLatency);
    }

    /**
     * Record latency for a batch of events. Returns true if the latency was
     * considered valid (within low/high thresholds) and counted as valid.
     *
     * @param events  number of events(records).
     * @param bytes   number of bytes.
     * @param latency latency value in milliseconds (or caller-defined unit).
     * @return true if this record is valid and counted; false otherwise.
     */
    final public boolean record(long events, long bytes, long latency) {
        this.totalBytes += bytes;
        this.totalRecords += events;
        if (latency < 0) {
            this.invalidLatencyRecords += events;
        } else {
            this.minLatency = Math.min(this.minLatency, latency);
            this.maxLatency = Math.max(this.maxLatency, latency);
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