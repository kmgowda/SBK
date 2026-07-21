/**
 * Copyright (c) KMG. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 */
package io.sbk.utils;

import org.junit.jupiter.api.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.assertTrue;

/** Tests bounded application shutdown execution. */
public class ApplicationShutdownHookTest {

    /** Verifies that a cleanup operation is invoked and allowed to finish. */
    @Test
    public void completesCleanup() {
        final AtomicBoolean cleaned = new AtomicBoolean();

        ApplicationShutdownHook.runBounded("test", () -> cleaned.set(true), 1, TimeUnit.SECONDS);

        assertTrue(cleaned.get());
    }

    /** Verifies that a blocked cleanup operation cannot block the hook indefinitely. */
    @Test
    public void boundsBlockedCleanup() {
        final CountDownLatch blocked = new CountDownLatch(1);
        final long start = System.nanoTime();

        ApplicationShutdownHook.runBounded("test", () -> {
            try {
                blocked.await();
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
            }
        }, 20, TimeUnit.MILLISECONDS);

        final long elapsedMillis = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - start);
        assertTrue(elapsedMillis < 1000, "shutdown cleanup must have a bounded wait");
    }
}
