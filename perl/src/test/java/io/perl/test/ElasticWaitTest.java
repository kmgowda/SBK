/**
 * Copyright (c) KMG. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.perl.test;

import io.perl.api.impl.ElasticWait;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 * Unit tests for {@link ElasticWait}.
 */
public class ElasticWaitTest {

    /**
     * Test that ElasticWait can be initialized without errors.
     */
    @Test
    public void testInitialization() {
        ElasticWait wait = new ElasticWait(1000, 1000, 100);
        assertNotNull("ElasticWait should be initialized", wait);
    }

    /**
     * Test that reset() sets idleCount to zero and waitAndCheck returns false after reset.
     */
    @Test
    public void testReset() {
        ElasticWait wait = new ElasticWait(1000, 1000, 100);
        wait.waitAndCheck(); // increment idleCount
        wait.reset();
        assertFalse("After reset, waitAndCheck should return false", wait.waitAndCheck());
    }

    /**
     * Test that waitAndCheck returns false initially and true after enough calls to exceed elasticCount.
     */
    @Test
    public void testWaitAndCheck() {
        ElasticWait wait = new ElasticWait(1000, 1000, 1);
        assertFalse("Initial waitAndCheck should return false", wait.waitAndCheck());

        // Force multiple waits to exceed elasticCount
        for (int i = 0; i < 1000; i++) {
            wait.waitAndCheck();
        }
        assertTrue("After multiple waits, should return true", wait.waitAndCheck());
    }

    /**
     * Test that updateElastic adjusts the elasticCount and eventually triggers waitAndCheck to return true.
     */
    @Test
    public void testUpdateElastic() {
        ElasticWait wait = new ElasticWait(1000, 1000, 1);
        wait.updateElastic(10); // Update with a small elapsed interval

        boolean triggered = false;
        for (int i = 0; i < 1000000 && !triggered; i++) {
            triggered = wait.waitAndCheck();
        }
        wait.waitAndCheck();
        assertTrue("UpdateElastic should adjust the wait threshold", triggered);
    }

    /**
     * Test that setElastic sets elasticCount based on totalCount and currentIntervalMS,
     * and that waitAndCheck returns false immediately after.
     */
    @Test
    public void testSetElastic() {
        ElasticWait wait = new ElasticWait(1000, 1000, 100);

        // Perform some waits to accumulate totalCount
        for (int i = 0; i < 10; i++) {
            wait.waitAndCheck();
        }

        wait.setElastic(500); // Set elastic with half the interval
        assertFalse("Initial check after setElastic should be false", wait.waitAndCheck());
    }

    /**
     * Test that updateElastic respects the minimum interval and does not trigger waitAndCheck immediately.
     */
    @Test
    public void testMinimumInterval() {
        ElasticWait wait = new ElasticWait(1000, 1000, 500);
        wait.updateElastic(999); // Try to update with almost full window interval

        // Should not trigger immediately due to minimum interval
        assertFalse("Should respect minimum interval", wait.waitAndCheck());
    }
}