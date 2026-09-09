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
 * Signals that PerL could not drain and stop before its cleanup deadline.
 */
public final class PerlCleanupTimeoutException extends IllegalStateException {

    /**
     * Creates a bounded-cleanup failure.
     *
     * @param phase lifecycle phase that exceeded the deadline
     */
    public PerlCleanupTimeoutException(String phase) {
        super("PerL cleanup deadline expired while " + phase
                + "; final aggregate results may be incomplete");
    }
}
