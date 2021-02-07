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

import io.sbk.api.Print;
import io.sbk.api.Time;
import javax.annotation.concurrent.NotThreadSafe;


@NotThreadSafe
public abstract class LatencyWindow extends LatencyStore {
    final public Time time;
    public long startTime;

    LatencyWindow(long lowLatency, long highLatency, double[] percentiles, Time time, long startTime) {
        super(lowLatency, highLatency, percentiles);
        this.time = time;
        this.startTime = startTime;
    }

    /**
     * Reset the window.
     *
     * @param startTime starting time.
     */
    public void reset(long startTime) {
        super.reset();
        this.startTime = startTime;
    }


    /**
     * Get the current time duration of this window.
     *
     * @param currentTime current time.
     * @return elapsed Time in Milliseconds
     */
    public long elapsedMilliSeconds(long currentTime) {
        return (long) time.elapsedMilliSeconds(currentTime, startTime);
    }

    /**
     * Print the window statistics.
     * @param endTime End time.
     * @param logger printer interface.
     */
    public void print(long endTime, Print logger) {
        final double elapsedSec = Math.max(time.elapsedSeconds(endTime, startTime), 1.0);
        final long totalLatencyRecords  = this.validLatencyRecords +
                this.lowerLatencyDiscardRecords + this.higherLatencyDiscardRecords;
        final double recsPerSec = totalRecords / elapsedSec;
        final double mbPerSec = (this.bytes / (1024.0 * 1024.0)) / elapsedSec;
        final double avgLatency = this.totalLatency / (double) totalLatencyRecords;
        long[] pecs = getPercentiles();
        logger.print(this.bytes, totalRecords, recsPerSec, mbPerSec,
                avgLatency, this.maxLatency, this.invalidLatencyRecords,
                this.lowerLatencyDiscardRecords, this.higherLatencyDiscardRecords,
                pecs);
    }


    /**
     * print only if there is data recorded.
     *
     * @param time current time.
     * @param printer printer interface.
     */
    public void printPendingData(long time,  Print printer) {
        if (this.totalRecords > 0) {
            print(time, printer);
        }
    }

    /**
     * Record the latency.
     *
     * @param startTime start time.
     * @param bytes number of bytes.
     * @param events number of events(records).
     * @param latency latency value in milliseconds.
     */
    abstract void record(long startTime, int bytes, int events, long latency);
}
