/**
 * Copyright (c) KMG. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.gem.api.impl;

import org.junit.jupiter.api.Test;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

/** Tests lifecycle progress behavior during concurrent scheduler shutdown. */
final class LifecycleProgressTest {
    @Test
    void toleratesSchedulerShutdownBeforeProgressRegistration() {
        final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
        scheduler.shutdownNow();

        assertDoesNotThrow(() -> {
            try (LifecycleProgress ignored = new LifecycleProgress("test operation", 1,
                    scheduler, () -> "pending")) {
                // Closing an unscheduled progress reporter must also remain safe.
            }
        });
    }
}
