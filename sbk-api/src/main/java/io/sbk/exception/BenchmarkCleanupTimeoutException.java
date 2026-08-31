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
 * Signals that the hard benchmark-cleanup deadline expired before lifecycle completion.
 *
 * <p>The benchmark result is deliberately failed because worker termination, recorder
 * draining, final aggregate reporting, or driver cleanup may still be incomplete when
 * the process is released.
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
                + " seconds; benchmark lifecycle and final aggregate results may be incomplete",
                initiatingFailure);
    }
}
