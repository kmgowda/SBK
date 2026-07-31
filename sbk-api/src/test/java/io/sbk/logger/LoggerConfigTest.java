/**
 * Copyright (c) KMG. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 */
package io.sbk.logger;

import io.perl.config.PerlConfig;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.dataformat.javaprop.JavaPropsFactory;

import java.io.IOException;
import java.io.InputStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/** Verifies defaults shared by SBK and PerL logger configuration. */
public final class LoggerConfigTest {

    /**
     * The bundled logger configuration must inherit PerL's authoritative
     * reporting interval instead of duplicating its numeric value.
     *
     * @throws IOException if the bundled properties cannot be parsed
     */
    @Test
    public void inheritsDefaultReportingIntervalFromPerl() throws IOException {
        try (InputStream input = LoggerConfigTest.class.getClassLoader()
                .getResourceAsStream("logger.properties")) {
            assertNotNull(input, "logger.properties must be available");
            final LoggerConfig config = new ObjectMapper(new JavaPropsFactory())
                    .readValue(input, LoggerConfig.class);

            assertEquals(PerlConfig.DEFAULT_PRINTING_INTERVAL_SECONDS,
                    config.reportingSeconds);
        }
    }
}
