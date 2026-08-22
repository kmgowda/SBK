/**
 * Copyright (c) KMG. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 */
package io.perl.exception;

/**
 * Signals that a benchmark produced no performance events within its configured idle deadline.
 */
public final class BenchmarkIdleTimeoutException extends RuntimeException {
    private static final long serialVersionUID = 1L;

    /**
     * Creates an idle-timeout failure with a stable operator-facing diagnostic.
     *
     * @param idleTimeoutSeconds configured maximum idle duration in seconds
     */
    public BenchmarkIdleTimeoutException(long idleTimeoutSeconds) {
        super("No performance benchmarking event was received for " + idleTimeoutSeconds + " seconds");
    }

    /**
     * Finds an idle-timeout failure in an asynchronous completion cause chain.
     *
     * @param failure failure to inspect
     * @return the idle-timeout cause, or {@code null} when the chain does not contain one
     */
    public static BenchmarkIdleTimeoutException find(Throwable failure) {
        Throwable cause = failure;
        while (cause != null) {
            if (cause instanceof BenchmarkIdleTimeoutException idleTimeout) {
                return idleTimeout;
            }
            cause = cause.getCause();
        }
        return null;
    }
}
