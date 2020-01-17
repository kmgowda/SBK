/**
 * Copyright (c) 2020 KMG. All Rights Reserved.
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
    public final QuadConsumer recordTime;
    public final Parameters params;

    Worker(int workerID, QuadConsumer recordTime, Parameters params) {
        this.workerID = workerID;
        this.recordTime = recordTime;
        this.params = params;
    }
}
