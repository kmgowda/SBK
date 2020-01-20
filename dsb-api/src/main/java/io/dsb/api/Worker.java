/**
 * Copyright (c) 2020 KMG. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 */

package io.dsb.api;

/**
 * Abstract class for Writers and Readers.
 */
public abstract class Worker {
    final static int TIME_HEADER_SIZE = 8;

    protected final int workerID;
    protected final Parameters params;
    protected final QuadConsumer recordTime;

    Worker(int workerID, Parameters params, QuadConsumer recordTime) {
        this.workerID = workerID;
        this.recordTime = recordTime;
        this.params = params;
    }
}
