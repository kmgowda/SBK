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
 * Signals that an existing benchmark failure was followed by a cleanup deadline expiry.
 *
 * <p>Cleanup timeout without an existing benchmark failure is warning-only.
 */
public final class BenchmarkCleanupTimeoutException extends IllegalStateException {

    /**
     * Creates a cleanup deadline failure.
     *
     * @param timeoutSeconds hard cleanup deadline in seconds
     * @param initiatingFailure failure that originally requested shutdown, or {@code null}
     */
    public BenchmarkCleanupTimeoutException(long timeoutSeconds, Throwable initiatingFailure) {
        super("SBK benchmark cleanup exceeded " + timeoutSeconds
                + " seconds after a benchmark failure; forcing bounded shutdown",
                initiatingFailure);
    }
}
