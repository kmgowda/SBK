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

/**
 * Interface for recording latencies.
 */
public interface PeriodicRecorder {

    /**
     * Start the Recorder.
     *
     * @param startTime starting time.
     */
    void start(long startTime);

    /**
     * Start the Recording window.
     *
     * @param startTime starting time.
     */
    void startWindow(long startTime);

    /**
     * Get the current time duration of this window starting from startWindow.
     *
     * @param currentTime current time.
     * @return elapsed Time in Milliseconds from the startTime.
     */
    long elapsedMilliSeconds(long currentTime);


    /**
     * Record the Event/record.
     *
     * @param startTime start time
     * @param endTime end time
     * @param bytes number of bytes
     * @param events number of events (records)
     */
    void record(long startTime, long endTime, int bytes, int events);


    /**
     * Stop the Recording window.
     * Results from startWindow to this method are printed /flushed.
     *
     * @param currentTime current time.
     */
    void stopWindow(long currentTime);

    /**
     * Stop the Recorder.
     *
     * @param endTime current time.
     */
    void stop(long endTime);
}
