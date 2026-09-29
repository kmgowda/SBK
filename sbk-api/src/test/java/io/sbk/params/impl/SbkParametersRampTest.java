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

/** Validates ramp sizes before worker admission can spin without progress. */
final class SbkParametersRampTest {
    @Test
    void rejectsNonPositiveStepsForBothWorkerTypes() {
        for (String option : new String[]{"-wstep", "-rstep"}) {
            for (String value : new String[]{"0", "-1", "-2147483648"}) {
                final SbkParameters params = new SbkParameters("ramp-test");
                assertThrows(IllegalArgumentException.class,
                        () -> params.parseArgs(new String[]{"-writers", "1", "-size", "100",
                                "-records", "1", option, value}));
            }
        }
    }

    @Test
    void acceptsPositiveSteps() throws Exception {
        final SbkParameters params = new SbkParameters("ramp-test");
        params.parseArgs(new String[]{"-writers", "1", "-size", "100", "-records", "1",
                "-wstep", "2", "-rstep", "3"});
        assertEquals(2, params.getWritersStep());
        assertEquals(3, params.getReadersStep());
    }
}
