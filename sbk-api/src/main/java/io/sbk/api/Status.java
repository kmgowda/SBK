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
 * class for Read and Write status.
 */
final public class Status {
    public long startTime;
    public long endTime;
    public int bytes;
    public int records;

    public Status() {
        this.startTime = 0;
        this.endTime = 0;
        this.bytes = 0;
        this.records = 0;
    }
}