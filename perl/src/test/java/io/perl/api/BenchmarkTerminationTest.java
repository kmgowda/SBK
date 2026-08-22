/**
 * Copyright (c) KMG. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 */
package io.perl.api;

import io.perl.exception.BenchmarkIdleTimeoutException;
import org.junit.jupiter.api.Test;

import java.util.concurrent.CompletionException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Tests the shared benchmark termination vocabulary. */
final class BenchmarkTerminationTest {

    @Test
    void identifiesConfiguredCompletionMode() {
        assertEquals(BenchmarkTermination.SECONDS_COMPLETED,
                BenchmarkTermination.configured(30, 1_000));
        assertEquals(BenchmarkTermination.RECORDS_COMPLETED,
                BenchmarkTermination.configured(0, 1_000));
        assertEquals(BenchmarkTermination.STOP_REQUESTED,
                BenchmarkTermination.configured(0, 0));
    }

    @Test
    void failureOverridesRequestedSuccess() {
        final BenchmarkIdleTimeoutException idleTimeout = new BenchmarkIdleTimeoutException(600);
        assertEquals(BenchmarkTermination.IDLE_TIMEOUT,
                BenchmarkTermination.resolve(BenchmarkTermination.RECORDS_COMPLETED,
                        new CompletionException(idleTimeout)));
        assertEquals(BenchmarkTermination.INTERNAL_FAILURE,
                BenchmarkTermination.resolve(BenchmarkTermination.SECONDS_COMPLETED,
                        new IllegalStateException("failed")));
    }

    @Test
    void descriptionsNameTheAuthoritativeOptionAndFailure() {
        assertEquals("completed successfully in -seconds 30 mode",
                BenchmarkTermination.SECONDS_COMPLETED.describe(30, 0, 600, null));
        assertEquals("completed successfully in -records 1000 mode",
                BenchmarkTermination.RECORDS_COMPLETED.describe(0, 1_000, 600, null));
        assertEquals("exited due to -idletimeoutseconds 600: BenchmarkIdleTimeoutException: "
                        + "No performance benchmarking event was received for 600 seconds",
                BenchmarkTermination.IDLE_TIMEOUT.describe(0, 1_000, 600,
                        new BenchmarkIdleTimeoutException(600)));
        assertEquals("exited due to internal exception: IllegalStateException: failed",
                BenchmarkTermination.INTERNAL_FAILURE.describe(30, 0, 600,
                        new CompletionException(new IllegalStateException("failed"))));
        assertTrue(BenchmarkTermination.SECONDS_COMPLETED.isSuccessfulCompletion());
        assertTrue(BenchmarkTermination.RECORDS_COMPLETED.isSuccessfulCompletion());
        assertFalse(BenchmarkTermination.STOP_REQUESTED.isSuccessfulCompletion());
    }
}
