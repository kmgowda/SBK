/**
 * Copyright (c) KMG. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 */
package io.sbk.exception;

/**
 * Signals that bounded cleanup expired after a benchmark failure or before final results.
 *
 * <p>Cleanup timeout without an existing benchmark failure is warning-only only when every
 * active performance recorder has already published its final aggregate.
 */
public final class BenchmarkCleanupTimeoutException extends IllegalStateException {

    /**
     * Creates a cleanup deadline failure.
     *
     * @param timeoutSeconds hard cleanup deadline in seconds
     * @param initiatingFailure failure that originally requested shutdown, or {@code null}
     */
    public BenchmarkCleanupTimeoutException(long timeoutSeconds, Throwable initiatingFailure) {
        this(timeoutSeconds, initiatingFailure, true);
    }

    /**
     * Creates a cleanup deadline failure with explicit final-result state.
     *
     * @param timeoutSeconds hard cleanup deadline in seconds
     * @param initiatingFailure failure retained as the cause
     * @param finalResultsPublished whether all final aggregates were published
     */
    public BenchmarkCleanupTimeoutException(long timeoutSeconds, Throwable initiatingFailure,
                                            boolean finalResultsPublished) {
        super("SBK benchmark cleanup exceeded " + timeoutSeconds
                + (finalResultsPublished
                ? " seconds after a benchmark failure; forcing bounded shutdown"
                : " seconds before the final Total was published; forcing bounded shutdown"),
                initiatingFailure);
    }
}
