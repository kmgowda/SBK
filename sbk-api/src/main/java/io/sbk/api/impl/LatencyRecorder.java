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

import io.sbk.api.Config;
import javax.annotation.concurrent.NotThreadSafe;

/**
 *  Base class for Performance statistics.
 */
@NotThreadSafe
public class LatencyRecorder {
    final public long lowLatency;
    final public long highLatency;
    public long totalRecords;
    public long validLatencyRecords;
    public long lowerLatencyDiscardRecords;
    public long higherLatencyDiscardRecords;
    public long invalidLatencyRecords;
    public long bytes;
    public long totalLatency;
    public long maxLatency;


    LatencyRecorder(long baseLatency, long latencyThreshold) {
        this.lowLatency = baseLatency;
        this.highLatency = latencyThreshold;
        reset();
    }

    public void reset() {
        this.totalRecords = 0;
        this.validLatencyRecords = 0;
        this.lowerLatencyDiscardRecords = 0;
        this.higherLatencyDiscardRecords = 0;
        this.invalidLatencyRecords = 0;
        this.bytes = 0;
        this.maxLatency = 0;
        this.totalLatency = 0;
    }

    /**
     * is Overflow condition for this recorder.
     *
     * @return isOverflow condition occurred or not
     */
    public boolean isOverflow() {
        return (this.totalLatency > Config.LONG_MAX) || (this.bytes > Config.LONG_MAX)
                || (this.totalRecords > Config.LONG_MAX);

    }

    /**
     * Record the latency and return if the latency is valid or not.
     *
     * @param bytes number of bytes.
     * @param events number of events(records).
     * @param latency latency value in milliseconds.
     * @return is valid latency record or not
     */
    public boolean record(int bytes, int events, long latency) {
        this.bytes += bytes;
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
                return true;
            }
        }
        return false;
    }
}