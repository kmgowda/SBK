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

import io.perl.api.LatencyPercentiles;
import io.perl.api.LatencyRecordWindow;
import io.perl.api.impl.PerlBuilder;
import io.perl.config.LatencyConfig;
import io.time.NanoSeconds;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Tests for the array-backed latency recorder.
 */
public class ArrayLatencyRecorderTest {

    /**
     * Verify that the inclusive maximum latency has a dedicated array slot.
     */
    @Test
    public void testRecordsInclusiveMaximumLatency() {
        final LatencyConfig config = new LatencyConfig();
        config.maxArraySizeMB = 1;
        final LatencyRecordWindow window = PerlBuilder.buildLatencyRecordWindow(
                config, new NanoSeconds(), 0, 10, new double[]{0.5});
        final LatencyPercentiles percentiles = new LatencyPercentiles(new double[]{0.5});

        window.recordLatency(0, 1, 1, 10);
        window.copyPercentiles(percentiles, null);

        assertEquals(10, percentiles.latencies[0]);
        assertEquals(1, percentiles.latenciesCount[0]);
    }
}
