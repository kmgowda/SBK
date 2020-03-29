/**
 * Copyright (c) KMG. All Rights Reserved..
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 */

package io.sbk.api;

/**
 * Private class for start and end time.
 */
final public class TimeStamp {
    public long startTime;
    public long endTime;
    public int bytes;
    public int records;

    public TimeStamp(long startTime, long endTime, int bytes, int records) {
        this.startTime = startTime;
        this.endTime = endTime;
        this.bytes = bytes;
        this.records = records;
    }

    public TimeStamp() {
        this(0, 0, 0, 0);
    }

    public TimeStamp(long endTime) {
        this(-1, endTime, 0, 0);
    }

    public boolean isEnd() {
        return this.startTime == -1;
    }
}