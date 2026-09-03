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

import io.sbk.thread.ThreadType;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Tests SBK worker-thread option defaults and explicit overrides.
 */
public class SbkParametersThreadTypeTest {
    private static final String[] REQUIRED_ARGS = {
        "-writers", "1", "-size", "1", "-records", "1"
    };

    /**
     * Omitting {@code -thread} selects virtual threads.
     *
     * @throws Exception if the valid test arguments cannot be parsed
     */
    @Test
    public void virtualThreadsAreDefault() throws Exception {
        assertEquals(ThreadType.Virtual, parseThreadType(REQUIRED_ARGS));
    }

    /**
     * Explicit platform, fork-join, and virtual selections remain supported.
     *
     * @throws Exception if the valid test arguments cannot be parsed
     */
    @Test
    public void explicitThreadTypesOverrideDefault() throws Exception {
        assertEquals(ThreadType.Platform, parseThreadType(withThreadType("p")));
        assertEquals(ThreadType.ForkJoin, parseThreadType(withThreadType("f")));
        assertEquals(ThreadType.Virtual, parseThreadType(withThreadType("v")));
    }

    /** Reject misspelled thread types instead of silently selecting platform threads. */
    @Test
    public void rejectsUnknownThreadType() {
        assertThrows(IllegalArgumentException.class,
                () -> parseThreadType(withThreadType("virtual")));
    }

    private static ThreadType parseThreadType(String[] args) throws Exception {
        final SbkParameters parameters = new SbkParameters("thread-type-test");
        parameters.parseArgs(args);
        return parameters.getThreadType();
    }

    private static String[] withThreadType(String value) {
        final String[] args = new String[REQUIRED_ARGS.length + 2];
        System.arraycopy(REQUIRED_ARGS, 0, args, 0, REQUIRED_ARGS.length);
        args[REQUIRED_ARGS.length] = "-thread";
        args[REQUIRED_ARGS.length + 1] = value;
        return args;
    }
}
