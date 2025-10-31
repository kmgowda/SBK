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

/**
 * Immutable container representing a benchmarking event timestamp. An instance
 * carries a start and end time together with the number of records and bytes
 * processed for that event.
 *
 * <p>Convention:
 * <ul>
 *     <li>A {@link TimeStamp} with {@code startTime == -1} is treated as an
 *     end marker (see {@link #isEnd()}).</li>
 *     <li>Time units for {@code startTime} and {@code endTime} are caller
 *     defined (commonly nanoseconds); callers must convert timings to the
 *     desired {@link io.perl.logger.PerformanceLogger#getTimeUnit()} when
 *     reporting latencies.</li>
 * </ul>
 */
final public class TimeStamp {

    /**
     * Event start time (caller-defined time unit).
     */
    final public long startTime;

    /**
     * Event end time (caller-defined time unit).
     */
    final public long endTime;

    /**
     * Number of records/events represented by this timestamp.
     */
    final public int records;

    /**
     * Size in bytes for the event(s).
     */
    final public int bytes;


    /**
     * Constructor TimeStamp initialize all values.
     *
     * @param startTime long
     * @param endTime   long
     * @param records   int
     * @param bytes     int
     */
    public TimeStamp(long startTime, long endTime, int records, int bytes) {
        this.startTime = startTime;
        this.endTime = endTime;
        this.bytes = bytes;
        this.records = records;
    }

    /**
     * Create an empty TimeStamp with all zeros.
     */
    public TimeStamp() {
        this(0, 0, 0, 0);
    }

    /**
     * Create an end marker TimeStamp. A TimeStamp with {@code startTime == -1}
     * is treated as an end-of-stream indicator by consumers.
     *
     * @param endTime end time value
     */
    public TimeStamp(long endTime) {
        this(-1, endTime, 0, 0);
    }

    /**
     * Checks if this timestamp is the end marker.
     *
     * @return true if {@link #startTime} == -1.
     */
    public boolean isEnd() {
        return this.startTime == -1;
    }
}