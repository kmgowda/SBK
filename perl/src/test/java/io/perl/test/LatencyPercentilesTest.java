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

import org.junit.Before;
import org.junit.Test;


import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import io.perl.api.LatencyPercentiles;

/**
 * Class LatencyPercentilesTest.
 */
public class LatencyPercentilesTest {

    private double[] fractions;
    private LatencyPercentiles percentiles;

    @Before
    public void setUp() {
        fractions = new double[]{0.5, 0.9, 0.99};
        percentiles = new LatencyPercentiles(fractions);
    }

    /**
     * Test constructor initializes arrays and fields correctly.
     */
    @Test
    public void testConstructor() {
        assertArrayEquals(fractions, percentiles.fractions, 0.0001);
        assertEquals(fractions.length, percentiles.latencies.length);
        assertEquals(fractions.length, percentiles.latencyIndexes.length);
        assertEquals(fractions.length, percentiles.latencyCount.length);
        assertEquals(0, percentiles.medianLatency);
        assertEquals(0, percentiles.medianIndex);
    }

    /**
     * Test reset sets indexes and clears values.
     */
    @Test
    public void testReset() {
        percentiles.reset(100);
        assertEquals(50, percentiles.latencyIndexes[0]);
        assertEquals(90, percentiles.latencyIndexes[1]);
        assertEquals(99, percentiles.latencyIndexes[2]);
        for (int i = 0; i < fractions.length; i++) {
            assertEquals(0, percentiles.latencies[i]);
            assertEquals(0, percentiles.latencyCount[i]);
        }
        assertEquals(50, percentiles.medianIndex);
        assertEquals(0, percentiles.medianLatency);
    }

    /**
     * Test copyLatency sets median and percentile values.
     */
    @Test
    public void testCopyLatency() {
        percentiles.reset(100);
        // Simulate a bucket covering indexes 0-60, latency=10, count=5
        percentiles.copyLatency(10, 5, 0, 60);
        // medianIndex=50, so medianLatency should be set
        assertEquals(10, percentiles.medianLatency);
        // 0.5 (index 0)  is within 0-60
        assertEquals(10, percentiles.latencies[0]);
        assertEquals(5, percentiles.latencyCount[0]);
        assertEquals(0, percentiles.latencies[1]);
        assertEquals(0, percentiles.latencyCount[1]);

        // Next bucket: 60-100, latency=20, count=2
        percentiles.copyLatency(20, 2, 60, 100);
        // 0.99 (index 2) is within 60-100
        assertEquals(20, percentiles.latencies[1]);
        assertEquals(2, percentiles.latencyCount[1]);
    }
}