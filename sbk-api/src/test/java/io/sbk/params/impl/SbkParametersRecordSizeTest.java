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

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Tests validation of the common SBK record-size option.
 */
public final class SbkParametersRecordSizeTest {

    /**
     * Reject zero before a driver or throughput calculation uses the size.
     */
    @Test
    public void rejectsZeroRecordSize() {
        assertInvalidSize("0");
    }

    /**
     * Reject negative sizes as parameter errors rather than allowing a later
     * {@link NegativeArraySizeException}.
     */
    @Test
    public void rejectsNegativeRecordSize() {
        assertInvalidSize("-1");
    }

    /**
     * Continue accepting positive record sizes.
     *
     * @throws Exception if valid arguments cannot be parsed
     */
    @Test
    public void acceptsPositiveRecordSize() throws Exception {
        final SbkParameters parameters = new SbkParameters("record-size-test");

        parameters.parseArgs(arguments("128"));

        assertEquals(128, parameters.getRecordSize());
    }

    private static void assertInvalidSize(String size) {
        final SbkParameters parameters = new SbkParameters("record-size-test");

        assertThrows(IllegalArgumentException.class,
                () -> parameters.parseArgs(arguments(size)));
    }

    private static String[] arguments(String size) {
        return new String[]{
            "-writers", "1", "-size", size, "-records", "1"
        };
    }
}
