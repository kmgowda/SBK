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

abstract public class LatencyWindow extends LatencyRecorder {
    final public LatencyPercentiles percentiles;
    final public Time time;
    public long startTime;
    final private int[] slc;


    public LatencyWindow(long lowLatency, long highLatency, long totalLatencyMax, long totalRecordsMax, long bytesMax,
                        double[] percentilesFractions, Time time) {
        super(lowLatency, highLatency, totalLatencyMax, totalRecordsMax, bytesMax);
        this.percentiles = new LatencyPercentiles(percentilesFractions);
        this.time = time;
        this.slc = new int[2];
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
     * @param endTime End time.
     * @param logger printer interface.
     * @param copyLatencies  Copy Latency values
     */
    final public void print(long endTime, Print logger, ReportLatencies copyLatencies) {
        final double elapsedSec = time.elapsedSeconds(endTime, startTime);
        final long totalLatencyRecords  = this.validLatencyRecords +
                this.lowerLatencyDiscardRecords + this.higherLatencyDiscardRecords;
        final double recsPerSec =  elapsedSec > 0 ? this.totalRecords / elapsedSec : 0;
        final double mbPerSec = elapsedSec > 0 ? (this.totalBytes / (PerlConfig.BYTES_PER_MB * 1.0)) / elapsedSec : 0;
        final double avgLatency = totalLatencyRecords > 0 ? this.totalLatency / (double) totalLatencyRecords : 0;
        copyPercentiles(percentiles, copyLatencies);
        getSLC(percentiles, slc);
        logger.print(elapsedSec, this.totalBytes, this.totalRecords, recsPerSec, mbPerSec,
                avgLatency, this.maxLatency, this.invalidLatencyRecords,
                this.lowerLatencyDiscardRecords, this.higherLatencyDiscardRecords,
                slc[0], slc[1], this.percentiles.latencies);
    }
    
    final public void getSLC(LatencyPercentiles percentiles, int[] slc) {
        slc[0] = 0;
        slc[1] = 0;
        final int h = percentiles.latencies.length-1;
        if (h <= 0 || percentiles.latencies[h] <= 0) {
            return;
        }
        final double maxVal = percentiles.latencies[h] * 1.0;
        final double midVal = percentiles.medianLatency * 1.0;
        int cnt1 = 0;
        int cnt2 = 0;
        double slcFactor1 = 0;
        double slcFactor2 = 0;
        for (int i = 0; i < h; i++) {
            if (percentiles.latencies[i] < midVal) {
                slcFactor1 += percentiles.latencies[i] / midVal;
                cnt1++;
            } else {
                slcFactor2 += percentiles.latencies[i] / maxVal;
                cnt2++;
            }
        }
        if (cnt1 > 0) {
            slc[0] = (int) ((1 - slcFactor1 / cnt1) * 100);
        }
        if (cnt2 > 0) {
            slc[1] = (int) ((1 - slcFactor2 / cnt2) * 100);
        }
    }


    /**
     * get the Percentiles.
     * @param percentiles Copy Percentiles
     * @param reportLatencies  Copy Latency records.
     */
    abstract public void copyPercentiles(LatencyPercentiles percentiles, ReportLatencies reportLatencies);
}
