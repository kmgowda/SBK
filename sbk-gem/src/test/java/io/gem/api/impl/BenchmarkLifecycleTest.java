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

import java.time.Duration;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTimeoutPreemptively;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Tests non-blocking SBK-GEM lifecycle state transitions. */
final class BenchmarkLifecycleTest {
    @Test
    void reportsRunningOnlyBetweenBeginAndShutdown() {
        final BenchmarkLifecycle lifecycle = new BenchmarkLifecycle();

        assertFalse(lifecycle.isRunning());
        assertTrue(lifecycle.begin());
        assertTrue(lifecycle.isRunning());
        assertTrue(lifecycle.beginShutdown());
        assertFalse(lifecycle.isRunning());
    }

    @Test
    void shutdownDoesNotWaitForBlockingStartupWork() {
        final BenchmarkLifecycle lifecycle = new BenchmarkLifecycle();
        final CountDownLatch startupBlocked = new CountDownLatch(1);
        final CountDownLatch releaseStartup = new CountDownLatch(1);
        assertTrue(lifecycle.begin());

        try (var executor = Executors.newSingleThreadExecutor()) {
            executor.submit(() -> {
                startupBlocked.countDown();
                releaseStartup.await();
                return null;
            });
            assertTimeoutPreemptively(Duration.ofSeconds(2), () -> {
                assertTrue(startupBlocked.await(1, TimeUnit.SECONDS));
                assertTrue(lifecycle.beginShutdown());
            });
            releaseStartup.countDown();
        }
    }

    @Test
    void shutdownWinsRaceWithEmbeddedSbmStartup() {
        final BenchmarkLifecycle lifecycle = new BenchmarkLifecycle();
        assertTrue(lifecycle.begin());
        assertTrue(lifecycle.beginShutdown());

        assertThrows(CancellationException.class, lifecycle::markSbmStarted);
        assertFalse(lifecycle.takeSbmStarted());
        assertFalse(lifecycle.beginShutdown());
    }
}
