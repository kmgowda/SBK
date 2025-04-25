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

import io.perl.data.Bytes;
import io.perl.logger.Print;
import io.time.Time;
import org.jetbrains.annotations.NotNull;

/**
 * Sealed Class LatencyWindow.
 */
abstract public sealed class LatencyWindow extends LatencyRecorder permits LatencyRecordWindow {
    /**
     * <code>LatencyPercentiles percentiles</code>.
     */
    final protected LatencyPercentiles percentiles;
    /**
     * <code>Time time</code>.
     */
    final protected Time time;
    /**
     * <code>long[] slc</code>.
     */
    final private long[] slc;
    /**
     * <code>long startTime</code>.
     */
    private long startTime;


    /**
     * Constructor LatencyWindow to initialize values and pass Latencies to it's super class.
     *
     * @param lowLatency                 long
     * @param highLatency                long
     * @param totalLatencyMax            long
     * @param totalRecordsMax            long
     * @param bytesMax                   long
     * @param percentilesFractions       double[]
     * @param time                       Time
     */
    public LatencyWindow(long lowLatency, long highLatency, long totalLatencyMax, long totalRecordsMax, long bytesMax,
                         double[] percentilesFractions, Time time) {
        super(lowLatency, highLatency, totalLatencyMax, totalRecordsMax, bytesMax);
        this.percentiles = new LatencyPercentiles(percentilesFractions);
        this.time = time;
        this.slc = new long[2];
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
     * Get the current time duration of this window since reset() method invoked.
     *
     * @param currentTime current time.
     * @return elapsed Time in Milliseconds
     */
    final public long elapsedMilliSeconds(long currentTime) {
        return (long) time.elapsedMilliSeconds(currentTime, startTime);
    }

    /**
     * Print the window statistics.
     *
     * @param endTime       End time.
     * @param logger        printer interface.
     * @param copyLatencies Copy Latency values
     */
    final public void print(long endTime, Print logger, ReportLatencies copyLatencies) {
        copyPercentiles(percentiles, copyLatencies);
        getSLC(percentiles, slc);
        print(endTime, logger);
    }

    private void print(long endTime, Print logger) {
        final double elapsedSec = time.elapsedSeconds(endTime, startTime);
        final long totalLatencyRecords = this.validLatencyRecords +
                this.lowerLatencyDiscardRecords + this.higherLatencyDiscardRecords;
        final double recsPerSec = elapsedSec > 0 ? this.totalRecords / elapsedSec : 0;
        final double mbPerSec = elapsedSec > 0 ? (this.totalBytes / (Bytes.BYTES_PER_MB * 1.0)) / elapsedSec : 0;
        final double avgLatency = totalLatencyRecords > 0 ? this.totalLatency / (double) totalLatencyRecords : 0;
        final long minLatency = this.minLatency == Long.MAX_VALUE ? 0 : this.minLatency;
        logger.print(elapsedSec, this.totalBytes, this.totalRecords, recsPerSec, mbPerSec,
                avgLatency, minLatency, this.maxLatency, this.invalidLatencyRecords,
                this.lowerLatencyDiscardRecords, this.higherLatencyDiscardRecords,
                slc[0], slc[1], this.percentiles.latencies);
    }

    private void getSLC(@NotNull LatencyPercentiles percentiles, @NotNull long[] slc) {
        slc[0] = 0;
        slc[1] = 0;
        final int h = percentiles.latencies.length - 1;
        if (h <= 0 || percentiles.latencies[h] <= 0) {
            return;
        }
        final double minVal = percentiles.latencies[0] * 1.0;
        final double midVal = percentiles.medianLatency * 1.0;
        long cnt1 = 0;
        long cnt2 = 0;
        double slcFactor1 = 0;
        double slcFactor2 = 0;
        for (int i = 1; i <= h; i++) {
            if (percentiles.latencies[i] <= midVal && minVal > 0) {
                slcFactor1 += (percentiles.latencies[i] - minVal) / minVal;
                cnt1++;
            } else if (midVal > 0) {
                slcFactor2 += (percentiles.latencies[i] - midVal) / midVal;
                cnt2++;
            }
        }

        if (cnt1 > 0) {
            slc[0] = (long) ((slcFactor1 + cnt1 - 1) / cnt1);
        }
        if (cnt2 > 0) {
            slc[1] = (long) ((slcFactor2 + cnt2 - 1) / cnt2);
        }
    }


    /**
     * get the Percentiles.
     *
     * @param percentiles     Copy Percentiles
     * @param reportLatencies Copy Latency records.
     */
    abstract public void copyPercentiles(LatencyPercentiles percentiles, ReportLatencies reportLatencies);


    /**
     * is the latency storage full.
     *
     * @return indicate the latency storage is full or not
     */
    abstract public boolean isFull();


    /**
     * Max memory Size in Bytes.
     *
     * @return Maximum window memory size in bytes
     */
    abstract public long getMaxMemoryBytes();
}
