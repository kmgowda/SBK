/**
 * Copyright (c) KMG. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.sbk.api.impl;

import io.perl.config.PerlConfig;
import io.sbk.config.SbkConfig;
import io.sbk.params.impl.SbkParameters;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests the PerL configuration bundled with the SBK application.
 */
public class SbkPerlConfigTest {

    /** Verifies that the centralized command defaults preserve established CLI behavior. */
    @Test
    public void commandDefaultsPreserveExistingBehavior() {
        final SbkConfig config = SbkConfig.get();

        assertFalse(config.defaultReadOnly);
        assertEquals(0, config.defaultWriters);
        assertEquals(0, config.defaultReaders);
        assertEquals(0, config.defaultRecords);
        assertEquals(0, config.defaultRecordSize);
        assertEquals(0, config.defaultSyncRecords);
        assertEquals(-1.0, config.defaultThroughput);
        assertEquals(1, config.defaultWriterStep);
        assertEquals(0, config.defaultWriterStepSeconds);
        assertEquals(1, config.defaultReaderStep);
        assertEquals(0, config.defaultReaderStepSeconds);
        assertEquals(0, config.defaultIdleSleepMillis);
        assertEquals("v", config.defaultThreadType);
    }

    /**
     * Verifies that installed SBK applications explicitly select the optimized
     * timestamp MPSC queue.
     *
     * @throws IOException if the bundled properties cannot be parsed
     */
    @Test
    public void mpscQueueIsEnabledInSbkProperties() throws IOException {
        try (InputStream inputStream = SbkPerlConfigTest.class
                .getClassLoader().getResourceAsStream("sbk.properties")) {
            assertNotNull(inputStream, "sbk.properties must be available");
            assertTrue(PerlConfig.build(inputStream).mpscQueueEnable,
                    "SBK must enable the optimized timestamp MPSC queue");
        }
    }

    /**
     * Verifies that command-line options override all timestamp queue
     * properties for one benchmark without rebuilding the distribution.
     *
     * @throws Exception if parameters or configuration cannot be parsed
    */
    @Test
    public void commandLineOverridesTimestampQueueImplementation()
            throws Exception {
        final PerlConfig expected = SbkParameters.loadPerlConfig();
        final SbkParameters parameters =
                new SbkParameters("queue-config-test");
        parameters.parseArgs(new String[]{
                "-writers", "1", "-size", "1",
                "-mpscqueue", "false"
        });

        final PerlConfig config = SbkBenchmark.buildPerlConfig(parameters);

        assertFalse(config.mpscQueueEnable);
        assertEquals(expected.maxQs, config.maxQs);
        assertEquals(expected.qPerWorker, config.qPerWorker);
        assertEquals("ConcurrentLinkedQueue (JDK)",
                config.getTimestampQueueName());
    }

    /**
     * Verifies that omitting queue options preserves every value loaded from
     * {@code sbk.properties} in the effective benchmark configuration.
     *
     * @throws Exception if parameters or configuration cannot be loaded
     */
    @Test
    public void propertyDefaultsReachBenchmarkConfiguration()
            throws Exception {
        final PerlConfig expected = SbkParameters.loadPerlConfig();
        final SbkParameters parameters =
                new SbkParameters("queue-default-test");
        parameters.parseArgs(new String[]{
                "-writers", "1", "-size", "1"
        });

        final PerlConfig actual = SbkBenchmark.buildPerlConfig(parameters);

        assertEquals(expected.mpscQueueEnable, actual.mpscQueueEnable);
        assertEquals(expected.maxQs, actual.maxQs);
        assertEquals(expected.qPerWorker, actual.qPerWorker);
    }
}
