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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests GEM web-console logger discovery and local-SBM argument forwarding.
 */
final class GemWebLoggerTest {

    @Test
    void discoversWebLoggerAndForwardsWebConsoleOptions() throws Exception {
        final GemWebLogger logger = new GemWebLogger();
        final SbmParameters parameters = new SbmParameters("test", 9717, 1, 0,
                new String[]{GemWebLogger.class.getSimpleName()});
        logger.addArgs(parameters);
        parameters.parseArgs(new String[]{"-class", "file", "-action", "r",
                "-webport", "9876", "-webopen", "false",
                "-websnapshotminutes", "42", "-webtimeoutminutes", "3", "-boardname", "gem-test", "-time", "ns"});
        logger.parseArgs(parameters);

        final String[] options = logger.getOptionsArgs();
        final String[] parsed = logger.getParsedArgs();
        assertTrue(Arrays.asList(options).contains("-webport"));
        assertFalse(Arrays.asList(options).contains("-webhost"));
        assertFalse(Arrays.asList(options).contains("-webstart"));
        assertFalse(Arrays.asList(parsed).contains("-webhost"));
        assertFalse(Arrays.asList(parsed).contains("-webstart"));
        assertEquals("9876", valueOf(parsed, "-webport"));
        assertEquals("42", valueOf(parsed, "-websnapshotminutes"));
        assertEquals("3", valueOf(parsed, "-webtimeoutminutes"));
        assertEquals("gem-test", valueOf(parsed, "-boardname"));
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
