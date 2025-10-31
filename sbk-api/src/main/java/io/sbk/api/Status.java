/**
 * Copyright (c) KMG. All Rights Reserved..
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.sbk.api;

/**
 * Holder for per-operation read/write status used by the SBK harness.
 *
 * <p>This mutable value object is reused by the benchmark helpers to return
 * timing and size information for a single read or write operation. Fields
 * are public for lightweight access from hot paths in the harness. Typical
 * lifecycle for a {@link Status} instance in the harness:
 * <ol>
 *   <li>Set {@link #startTime} before beginning an operation.</li>
 *   <li>Set {@link #endTime} when the operation completes (or equals
 *       {@link #startTime} for synchronous operations).</li>
 *   <li>Set {@link #records} and {@link #bytes} to indicate how many
 *       records and bytes were processed.</li>
 * </ol>
 *
 * <p>Note: this class intentionally exposes mutable public fields to avoid
 * allocation and accessor overhead in the hot benchmarking loop. Do not
 * share a single {@code Status} instance across concurrent operations.
 */
final public class Status {
    public long startTime;
    public long endTime;
    public int records;
    public int bytes;

    public Status() {
        this.startTime = 0;
        this.endTime = 0;
        this.records = 0;
        this.bytes = 0;
    }
}