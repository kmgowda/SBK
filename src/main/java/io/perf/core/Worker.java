/**
 * Copyright (c) 2017 Dell Inc., or its subsidiaries. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 */

package io.perf.core;

/**
 * Abstract class for Writers and Readers.
 */
public abstract class Worker {
    final static int TIME_HEADER_SIZE = 8;

    public final int workerID;
    public final int events;
    public final int messageSize;
    public final int timeout;
    public final String streamName;
    public final long startTime;
    public final PerfStats stats;
    public final int secondsToRun;

    Worker(int sensorId, int events, int secondsToRun,
           int messageSize, long start, PerfStats stats,
           String streamName, int timeout) {
        this.workerID = sensorId;
        this.events = events;
        this.secondsToRun = secondsToRun;
        this.startTime = start;
        this.stats = stats;
        this.streamName = streamName;
        this.messageSize = messageSize;
        this.timeout = timeout;
    }
}
