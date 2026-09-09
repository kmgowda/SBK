/**
 * Copyright (c) KMG. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.sbk.api;

import io.perl.api.PerlChannel;
import io.sbk.params.Parameters;

import java.io.InterruptedIOException;
import java.net.SocketTimeoutException;
import java.util.concurrent.RejectedExecutionException;

/**
 * Abstract class for Writers and Readers.
 *
 * <p>This small immutable holder provides per-worker context that is shared
 * with driver implementations for both readers and writers. It centralises
 * three commonly used pieces of information:
 * <ul>
 *   <li>{@link #id} - the worker identifier (0-based) used for request logging
 *       and per-worker statistics.</li>
 *   <li>{@link #params} - the parsed benchmark parameters that control run
 *       behaviour (counts, rates, sizes, etc.).</li>
 *   <li>{@link #perlChannel} - optional PerL channel used to emit timing and
 *       throughput events to the metrics/collector subsystem.</li>
 * </ul>
 *
 * <p>Implementation notes and guidelines:
 * <ul>
 *   <li>The fields are intentionally final to make the Worker instance
 *       immutable after construction; driver code may safely read these
 *       values concurrently without additional synchronization.</li>
 *   <li>Do not add driver-specific mutable state to this class; instead
 *       keep per-worker state inside the driver implementation to avoid
 *       accidental sharing between workers.</li>
 *   <li>Construct Worker instances using the worker id assigned by the
 *       harness (writers/readers are created by {@code SbkBenchmark}).</li>
 * </ul>
 */
public abstract class Worker {
    /** Worker identifier assigned by the benchmark harness. */
    public final int id;
    /** Benchmark parameters shared with this worker. */
    public final Parameters params;
    /** Performance channel used to publish measurements. */
    public final PerlChannel perlChannel;

    /**
     * Creates a benchmark worker.
     *
     * @param workerID worker identifier
     * @param params benchmark parameters
     * @param perlChannel performance channel
     */
    public Worker(int workerID, Parameters params, PerlChannel perlChannel) {
        this.id = workerID;
        this.params = params;
        this.perlChannel = perlChannel;
    }

    /**
     * Identifies an I/O failure caused solely by active benchmark shutdown.
     *
     * <p>This is invoked only from worker exception handling. A socket timeout
     * remains a benchmark failure even though the JDK models it as an
     * {@link InterruptedIOException}.</p>
     *
     * @param failure worker failure
     * @return {@code true} when interruption or executor rejection caused the failure
     */
    public static boolean isShutdownInterruption(Throwable failure) {
        if (!Thread.currentThread().isInterrupted()) {
            return false;
        }
        Throwable cause = failure;
        while (cause != null) {
            if (cause instanceof SocketTimeoutException) {
                return false;
            }
            if (cause instanceof InterruptedException
                    || cause instanceof InterruptedIOException
                    || cause instanceof RejectedExecutionException) {
                return true;
            }
            cause = cause.getCause();
        }
        return false;
    }
}
