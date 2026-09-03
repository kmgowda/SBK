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

/** Verifies that requested throughput always produces active rate control. */
public final class SbkParametersThroughputTest {
    private static final int ONE_MEBIBYTE = 1024 * 1024;

    /** Reject an aggregate rate that truncates to zero for every worker. */
    @Test
    public void rejectsPositiveThroughputBelowOneRecordPerWorker() {
        assertThrows(IllegalArgumentException.class,
                () -> parse(8, ONE_MEBIBYTE, "1"));
    }

    /** Accept the exact boundary of one record per second per worker. */
    @Test
    public void acceptsOneRecordPerSecondPerWorker() throws Exception {
        assertEquals(1, parse(8, ONE_MEBIBYTE, "8").getRecordsPerSec());
    }

    /** Reject non-finite rates before narrowing them to an integer. */
    @Test
    public void rejectsNonFiniteThroughput() {
        assertThrows(IllegalArgumentException.class,
                () -> parse(1, 1, "Infinity"));
        assertThrows(IllegalArgumentException.class,
                () -> parse(1, 1, "NaN"));
    }

    /** Accept only minus one as the maximum-throughput sentinel. */
    @Test
    public void rejectsUnknownNegativeThroughput() {
        assertThrows(IllegalArgumentException.class,
                () -> parse(1, 1, "-2"));
    }

    private static SbkParameters parse(int writers, int size, String throughput) throws Exception {
        final SbkParameters parameters = new SbkParameters("throughput-test");
        parameters.parseArgs(new String[]{
            "-writers", Integer.toString(writers),
            "-size", Integer.toString(size),
            "-records", "1",
            "-throughput", throughput
        });
        return parameters;
    }
}
