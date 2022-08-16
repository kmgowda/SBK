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
 * class for time stamp including start, end time, bytes and records.
 */
final public class TimeStamp {

    /**
     * <code>long startTime</code>.
     */
    final public long startTime;

    /**
     * <code>long endTime</code>.
     */
    final public long endTime;

    /**
     * <code>int bytes</code>.
     */
    final public int bytes;

    /**
     * <code>int records</code>.
     */
    final public int records;

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
     * This Constructor TimeStamp with no arguments initialize all values with 0.
     */
    public TimeStamp() {
        this(0, 0, 0, 0);
    }

    /**
     * This Constructor TimeStamp with just one argument, initialize {@link #startTime} = -1
     * and {@link #endTime} according to the given parameters.
     *
     * @param endTime long
     */
    public TimeStamp(long endTime) {
        this(-1, endTime, 0, 0);
    }

    /**
     * Checks if it is the end.
     *
     * @return true if {@link #startTime} == -1.
     */
    public boolean isEnd() {
        return this.startTime == -1;
    }
}