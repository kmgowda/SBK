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
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests the PerL configuration bundled with the SBK application.
 */
public class SbkPerlConfigTest {

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
}
