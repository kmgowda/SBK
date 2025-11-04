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
 * LatencyWindow aggregates latency and throughput data.
 *
 * It accumulates records, computes averages, percentiles and SLC metrics, and delegates
 * printing to a {@link Print} implementation.
 *
 * <p>Typical flow:
 * <ol>
 *     <li>{@link #reset(long)} is called with the window start time.</li>
 *     <li>Events are recorded into the window's recorder.</li>
 *     <li>When the reporting time arrives the framework calls
 *     {@link #print(long, Print, ReportLatencies)} to compute percentiles and
 *     emit the periodic summary.</li>
 * </ol>
 */
abstract public sealed class LatencyWindow extends LatencyRecorder permits LatencyRecordWindow {

    /* configured percentiles helper */
    final protected LatencyPercentiles percentiles;

    /* time helper used for elapsed conversions */
    final protected Time time;

    /* sliding latency coverage result */
    final private long[] slc;

    /* window start time in the Time implementation's unit */
    private long startTime;


    /**
     * Construct a latency window with configured thresholds and percentiles.
     *
     * @param lowLatency            minimum latency value (inclusive)
     * @param highLatency           maximum latency value (inclusive)
     * @param totalLatencyMax       maximum cumulative latency allowed before overflow
     * @param totalRecordsMax       maximum total records allowed before overflow
     * @param bytesMax              maximum total bytes allowed before overflow
     * @param percentilesFractions  array of percentile fractions (e.g. {0.5, 0.9})
     * @param time                  Time helper used for elapsed conversions
     */
    public LatencyWindow(long lowLatency, long highLatency, long totalLatencyMax, long totalRecordsMax, long bytesMax,
                         double[] percentilesFractions, Time time) {
        super(lowLatency, highLatency, totalLatencyMax, totalRecordsMax, bytesMax);
        this.percentiles = new LatencyPercentiles(percentilesFractions);
        this.time = time;
        this.slc = new long[2];
    }

    /**
     * Reset the window state and set the start time.
     *
     * @param startTime starting time.
     */
    public void reset(long startTime) {
        super.reset();
        this.startTime = startTime;
    }


    /**
     * Get the elapsed time in milliseconds between the provided current time
     * and the window start time.
     *
     * @param currentTime current time.
     * @return elapsed Time in Milliseconds
     */
    final public long elapsedMilliSeconds(long currentTime) {
        return (long) time.elapsedMilliSeconds(currentTime, startTime);
    }

    /**
     * Print the computed window statistics using the supplied logger. This
     * method computes percentiles by delegating to {@link #copyPercentiles}
     * and computes SLC before delegating to the logger's print method.
     *
     * @param endTime       End time.
     * @param logger        printer interface.
     * @param copyLatencies Copy Latency values provider.
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
                slc[0], slc[1], this.percentiles.latencies, this.percentiles.latenciesCount);
    }

    private void getSLC(@NotNull LatencyPercentiles percentiles, long[] slc) {
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
     * Indicates whether the underlying storage used by the window is full.
     *
     * @return true if the storage is full
     */
    abstract public boolean isFull();


    /**
     * Return the maximum memory usage in bytes for the window's storage.
     *
     * @return Maximum window memory size in bytes
     */
    abstract public long getMaxMemoryBytes();
}
