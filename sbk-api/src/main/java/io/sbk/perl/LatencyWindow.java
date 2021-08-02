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


    public LatencyWindow(long lowLatency, long highLatency, long totalLatencyMax, long totalRecordsMax, long bytesMax,
                        double[] percentilesFractions, Time time) {
        super(lowLatency, highLatency, totalLatencyMax, totalRecordsMax, bytesMax);
        this.percentiles = new LatencyPercentiles(percentilesFractions);
        this.time = time;
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
        final double elapsedSec = Math.max(time.elapsedSeconds(endTime, startTime), 1.0);
        final long totalLatencyRecords  = this.validLatencyRecords +
                this.lowerLatencyDiscardRecords + this.higherLatencyDiscardRecords;
        final double recsPerSec = this.totalRecords / elapsedSec;
        final double mbPerSec = (this.totalBytes / (PerlConfig.BYTES_PER_MB * 1.0)) / elapsedSec;
        final double avgLatency = this.totalLatency / (double) totalLatencyRecords;
        copyPercentiles(percentiles, copyLatencies);
        logger.print(elapsedSec, this.totalBytes, this.totalRecords, recsPerSec, mbPerSec,
                avgLatency, this.maxLatency, this.invalidLatencyRecords,
                this.lowerLatencyDiscardRecords, this.higherLatencyDiscardRecords,
                getSLC(this.percentiles.latencies), this.percentiles.latencies);
    }
    
    final public double getSLC(long[] latencies) {
        final int h = latencies.length-1;
        if (h <= 0 || latencies[h] <= 0) {
            return 0;
        }
        final double maxVal = latencies[h] * 1.0;
        double slcFactor = 0;
        for (int i = 0; i < h; i++) {
            slcFactor += latencies[i] / maxVal;
        }
        return (1.0 - slcFactor / h) * 100;
    }


    /**
     * get the Percentiles.
     * @param percentiles Copy Percentiles
     * @param reportLatencies  Copy Latency records.
     */
    abstract public void copyPercentiles(LatencyPercentiles percentiles, ReportLatencies reportLatencies);
}
