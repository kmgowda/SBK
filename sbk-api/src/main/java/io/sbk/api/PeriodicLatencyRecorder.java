/**
 * Copyright (c) KMG. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
*/

package io.sbk.api;

/**
 * Interface for recording latencies.
 */
public interface PeriodicLatencyRecorder {

    /**
     * Start the window.
     *
     * @param startTime starting time.
     */
    void start(long startTime);

    /**
     * Reset the window.
     *
     * @param startTime starting time.
     */
    void resetWindow(long startTime);

    /**
     * is Overflow condition for this recorder.
     *
     * @return isOverflow condition occurred or not
     */
    boolean isOverflow();

    /**
     * Get the current time duration of this window.
     *
     * @param currentTime current time.
     * @return elapsed Time in Milliseconds from the startTime.
     */
    long elapsedMilliSeconds(long currentTime);


    /**
     * Record the latency.
     *
     * @param startTime start time of the event.
     * @param bytes number of bytes
     * @param events number of events (records)
     * @param latency latency value
     */
    void record(long startTime, int bytes, int events, long latency);


    /**
     * print the periodic Latency Results.
     *
     * @param currentTime current time.
     */
    void print(long currentTime);

    /**
     * print the Final Latency Results.
     *
     * @param endTime current time.
     */
    void printTotal(long endTime);
}
