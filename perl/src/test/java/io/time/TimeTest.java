/**
 * Copyright (c) KMG. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 */
package io.time;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/** Tests native benchmark-duration conversion. */
final class TimeTest {

    @Test
    void convertsSecondsToEveryNativeUnit() {
        assertEquals(2L * Time.MS_PER_SEC, new MilliSeconds().secondsToTimeUnits(2));
        assertEquals(2L * Time.MICROS_PER_SEC, new MicroSeconds().secondsToTimeUnits(2));
        assertEquals(2L * Time.NS_PER_SEC, new NanoSeconds().secondsToTimeUnits(2));
    }

    @Test
    void rejectsDurationOverflow() {
        assertThrows(ArithmeticException.class,
                () -> new NanoSeconds().secondsToTimeUnits(Long.MAX_VALUE));
    }
}
