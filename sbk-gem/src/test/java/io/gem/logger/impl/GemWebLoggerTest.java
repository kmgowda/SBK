/**
 * Copyright (c) KMG. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 */
package io.gem.logger.impl;

import io.gem.api.GemLoggerPackage;
import io.sbm.params.impl.SbmParameters;
import org.junit.jupiter.api.Test;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests GEM dashboard logger discovery and local-SBM argument forwarding.
 */
final class GemWebLoggerTest {

    @Test
    void discoversWebLoggerAndForwardsDashboardOptions() throws Exception {
        final GemWebLogger logger = new GemWebLogger();
        final SbmParameters parameters = new SbmParameters("test", 9717, 1, 0,
                new String[]{GemWebLogger.class.getSimpleName()});
        logger.addArgs(parameters);
        parameters.parseArgs(new String[]{"-class", "file", "-action", "r", "-dashboardhost", "127.0.0.1",
                "-dashboardport", "9876", "-dashboardstart", "false", "-dashboardopen", "false",
                "-dashboardretention", "42", "-dashboardname", "gem-test", "-time", "ns"});
        logger.parseArgs(parameters);

        final String[] options = logger.getOptionsArgs();
        final String[] parsed = logger.getParsedArgs();
        assertTrue(Arrays.asList(options).contains("-dashboardport"));
        assertEquals("9876", valueOf(parsed, "-dashboardport"));
        assertEquals("false", valueOf(parsed, "-dashboardstart"));
        assertEquals("gem-test", valueOf(parsed, "-dashboardname"));
        assertTrue(Arrays.asList(new GemLoggerPackage("io.gem.logger").getClassNames())
                .contains(GemWebLogger.class.getSimpleName()));
    }

    private static String valueOf(String[] arguments, String option) {
        for (int index = 0; index + 1 < arguments.length; index += 2) {
            if (option.equals(arguments[index])) {
                return arguments[index + 1];
            }
        }
        throw new AssertionError("Missing option " + option);
    }
}
