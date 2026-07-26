/**
 * Copyright (c) KMG. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.perl.api.impl;

import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Deterministic tests for {@link ElasticWait}.
 *
 * <p>No test depends on the host scheduler, clock resolution, processor speed,
 * or the accuracy of {@code parkNanos}. Elapsed time is supplied explicitly
 * just as it is by the PerL recorder.
 */
public class ElasticWaitTest {

    /**
     * Invalid intervals are rejected instead of producing a broken threshold.
     */
    @Test
    public void rejectsNonPositiveIntervals() {
        assertThrows(IllegalArgumentException.class,
                () -> new ElasticWait(0, 1000, 100));
        assertThrows(IllegalArgumentException.class,
                () -> new ElasticWait(1000, 0, 100));
        assertThrows(IllegalArgumentException.class,
                () -> new ElasticWait(1000, 1000, 0));
    }

    /**
     * Bootstrap sampling grows exponentially until the supplied clock moves.
     */
    @Test
    public void bootstrapHandlesCoarseClocks() {
        final ElasticWait wait = new ElasticWait(1000, 1000, 100, ignored -> { });

        assertTrue(wait.waitAndCheck(), "The first park must calibrate the platform");
        wait.updateElastic(0);
        assertFalse(wait.waitAndCheck(), "A zero-duration sample must grow the batch");
        assertTrue(wait.waitAndCheck(), "The doubled bootstrap batch must trigger");
        wait.updateElastic(0);
        assertFalse(wait.waitAndCheck());
        assertFalse(wait.waitAndCheck());
        assertFalse(wait.waitAndCheck());
        assertTrue(wait.waitAndCheck(), "The bootstrap batch must double again");
    }

    /**
     * A slow park implementation produces a small wait batch.
     */
    @Test
    public void calibratesForSlowOperatingSystemParks() {
        final ElasticWait wait = new ElasticWait(1000, 1000, 100, ignored -> { });

        assertTrue(wait.waitAndCheck());
        wait.updateElastic(10);

        assertEquals(99, waitsUntilCheck(wait),
                "One park per 10 ms should require 99 parks for the remaining 990 ms");
    }

    /**
     * A fast park implementation produces a larger wait batch without using
     * processor-specific loop counts or MIPS assumptions.
     */
    @Test
    public void calibratesForFastOperatingSystemParks() {
        final ElasticWait wait = new ElasticWait(1000, 1000, 100, ignored -> { });

        for (int i = 0; i < 100; i++) {
            wait.waitAndCheck();
        }
        wait.updateElastic(10);

        assertEquals(9900, waitsUntilCheck(wait),
                "Ten parks per millisecond should require 9900 parks for the remaining window");
    }

    /**
     * Partial clock checks reset their batch and schedule only the remaining
     * part of the current reporting window.
     */
    @Test
    public void partialCheckDoesNotLeakWaitsIntoNextBatch() {
        final ElasticWait wait = new ElasticWait(1000, 1000, 100, ignored -> { });

        assertTrue(wait.waitAndCheck());
        wait.updateElastic(1);
        for (int i = 0; i < 10; i++) {
            assertFalse(wait.waitAndCheck());
        }
        wait.updateElastic(11);
        assertEquals(989, waitsUntilCheck(wait));
    }

    /**
     * Window rotation retains the measured rate but clears all window-local
     * counters.
     */
    @Test
    public void expiredWindowStartsWithIndependentCounters() {
        final ElasticWait wait = new ElasticWait(1000, 1000, 100, ignored -> { });

        assertTrue(wait.waitAndCheck());
        wait.setElastic(2);
        assertEquals(500, waitsUntilCheck(wait));

        wait.reset();
        assertEquals(500, waitsUntilCheck(wait),
                "Repeated reset must not carry completed waits across windows");
    }

    /**
     * The configured idle strategy is invoked exactly once per wait.
     */
    @Test
    public void invokesIdleStrategyOncePerWait() {
        final AtomicLong parks = new AtomicLong();
        final ElasticWait wait = new ElasticWait(1000, 1000, 100,
                ignored -> parks.incrementAndGet());

        wait.waitAndCheck();
        wait.updateElastic(1);
        waitsUntilCheck(wait);

        assertEquals(1000, parks.get());
    }

    private static long waitsUntilCheck(ElasticWait wait) {
        long count = 0;
        do {
            count++;
        } while (!wait.waitAndCheck());
        return count;
    }
}
