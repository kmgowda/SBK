/**
 * Copyright (c) KMG. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 */
package io.sbk.params.impl;

import io.perl.config.PerlConfig;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests common command-line options controlling the PerL timestamp queue.
 */
public final class SbkParametersPerlQueueTest {

    /**
     * Accept the JDK queue implementation as an explicit override.
     *
     * @throws Exception if the valid option cannot be parsed
    */
    @Test
    public void parsesTimestampQueueOverride() throws Exception {
        final SbkParameters parameters = parse(
                "-mpscqueue", "false");

        assertEquals("false", parameters.getOptionValue("mpscqueue"));
        assertFalse(parameters.isMpscQueueEnabled());
    }

    /**
     * Load initial option values from sbk.properties and include the concrete
     * values in CLI help.
     *
     * @throws Exception if the property-backed defaults cannot be parsed
     */
    @Test
    public void usesPropertyBackedDefaultsInParametersAndHelp()
            throws Exception {
        final PerlConfig expected = SbkParameters.loadPerlConfig();
        final SbkParameters parameters = parse();

        assertEquals(expected.mpscQueueEnable,
                parameters.isMpscQueueEnabled());

        final String helpText = parameters.getHelpText()
                .replaceAll("\\s+", " ");
        assertTrue(helpText.contains("default: "
                + expected.mpscQueueEnable), helpText);
        assertFalse(helpText.contains("-timestampqueues"), helpText);
        assertFalse(helpText.contains("-qperworker"), helpText);
    }

    /**
     * Reject values that Boolean.parseBoolean would otherwise silently map
     * to false.
     */
    @Test
    public void rejectsInvalidQueueSelection() {
        assertThrows(IllegalArgumentException.class,
                () -> parse("-mpscqueue", "yes"));
    }

    private static SbkParameters parse(String... queueArgs)
            throws Exception {
        final String[] args = new String[queueArgs.length + 4];
        args[0] = "-writers";
        args[1] = "1";
        args[2] = "-size";
        args[3] = "1";
        System.arraycopy(queueArgs, 0, args, 4, queueArgs.length);
        final SbkParameters parameters =
                new SbkParameters("perl-queue-test");
        parameters.parseArgs(args);
        return parameters;
    }
}
