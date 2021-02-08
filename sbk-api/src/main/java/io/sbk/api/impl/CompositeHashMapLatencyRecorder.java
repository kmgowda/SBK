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

import io.sbk.api.CloneLatencies;
import io.sbk.api.LatencyRecorder;
import io.sbk.api.PeriodicLatencyRecorder;
import io.sbk.api.Print;
import javax.annotation.concurrent.NotThreadSafe;


/**
 *  class for Performance statistics.
 */
@NotThreadSafe
public class CompositeHashMapLatencyRecorder extends HashMapLatencyRecorder implements CloneLatencies, PeriodicLatencyRecorder {
    final public LatencyWindow window;
    final public Print windowLogger;
    final public Print loggerTotal;

    CompositeHashMapLatencyRecorder(LatencyWindow window, Print logger, Print loggerTotal) {
        super(window.lowLatency, window.highLatency, window.totalLatencyMax,
                window.totalRecordsMax, window.bytesMax, window.percentileFractions, window.time);
        this.window = window;
        this.windowLogger = logger;
        this.loggerTotal = loggerTotal;
    }

    /**
     * Start the window.
     *
     * @param startTime starting time.
     */
    public void start(long startTime) {
        window.reset(startTime);
        reset(startTime);
    }

    /**
     * Reset the window.
     *
     * @param startTime starting time.
     */
    public void reset(long startTime) {
        window.reset(startTime);
    }


    /**
     * Get the current time duration of this window.
     *
     * @param currentTime current time.
     * @return elapsed Time in Milliseconds from the startTime.
     */
    public long elapsedMilliSeconds(long currentTime) {
        return window.elapsedMilliSeconds(currentTime);
    }


    /**
     * Record the latency.
     *
     * @param startTime start time of the event.
     * @param bytes number of bytes
     * @param events number of events (records)
     * @param latency latency value
     */
    public void record(long startTime, int bytes, int events, long latency) {
        window.record(startTime, bytes, events, latency);
    }

    @Override
    public void updateLatencyRecords(LatencyRecorder latencies) {
        this.totalRecords += latencies.totalRecords;
        this.totalLatency += latencies.totalLatency;
        this.bytes += latencies.bytes;
        this.invalidLatencyRecords += latencies.invalidLatencyRecords;
        this.lowerLatencyDiscardRecords += latencies.lowerLatencyDiscardRecords;
        this.higherLatencyDiscardRecords += latencies.higherLatencyDiscardRecords;
        this.validLatencyRecords += latencies.validLatencyRecords;
        this.maxLatency += Math.max(this.maxLatency, latencies.maxLatency);
    }

    @Override
    public void copyLatency(long latency, long events) {
        latencies.put(latency, latencies.getOrDefault(latency, 0L) + events);
    }


    /**
     * print the periodic Latency Results.
     *
     * @param currentTime current time.
     */
    public void print(long currentTime) {
        window.print(currentTime, windowLogger, this);
    }

    /**
     * print the Final Latency Results.
     *
     * @param endTime current time.
     */
    public void printTotal(long endTime) {
        window.printPendingData(endTime, windowLogger, this);
        print(endTime, loggerTotal, null);
    }

}
