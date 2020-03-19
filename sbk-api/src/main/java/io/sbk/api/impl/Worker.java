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

import io.sbk.api.Parameters;
import io.sbk.api.RecordTime;

/**
 * Abstract class for Writers and Readers.
 */
public abstract class Worker {
    protected final int workerID;
    protected final Parameters params;
    protected final RecordTime recordTime;

    Worker(int workerID, Parameters params, RecordTime recordTime) {
        this.workerID = workerID;
        this.params = params;
        this.recordTime = recordTime;
    }
}
