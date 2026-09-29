/**
 * Copyright (c) KMG. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 */
package io.sbk.data.impl;

import org.junit.jupiter.api.Test;

import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertEquals;

/** Verifies fixed-size, lossless timestamp encoding for the complete long range. */
final class SbkStringTest {
    @Test
    void roundTripsBoundaryTimestampsWithoutGrowingRecords() {
        for (long value : new long[]{0, 1, -1, 9_999_999_999_999_999L,
                10_000_000_000_000_000L, Long.MIN_VALUE, Long.MAX_VALUE}) {
            verify(value);
        }
    }

    @Test
    void roundTripsRandomSignedTimestamps() {
        final Random random = new Random(12345);
        for (int i = 0; i < 1000; i++) {
            verify(random.nextLong());
        }
    }

    private static void verify(long value) {
        final SbkString type = new SbkString();
        assertEquals(16, type.getWriteReadMinSize());
        for (String suffix : new String[]{"", "payload-suffix"}) {
            final String original = "x".repeat(type.getWriteReadMinSize()) + suffix;
            final String encoded = type.setTime(original, value);
            assertEquals(original.length(), encoded.length());
            assertEquals(suffix, encoded.substring(type.getWriteReadMinSize()));
            assertEquals(value, type.getTime(encoded));
            assertEquals(value, type.getTime(type.setTime(encoded, value)));
        }
    }
}
