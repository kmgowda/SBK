/**
 * Copyright (c) KMG. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 */
package io.perl.test;

import io.perl.data.Bytes;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/** Verifies the binary byte-size constants used by PerL limits. */
final class BytesTest {

    @Test
    void definesBinaryByteUnitsWithoutOverflow() {
        assertEquals(1_024, Bytes.BYTES_PER_KB);
        assertEquals(1_048_576, Bytes.BYTES_PER_MB);
        assertEquals(1_073_741_824L, Bytes.BYTES_PER_GB);
        assertEquals((long) Bytes.BYTES_PER_MB * Bytes.BYTES_PER_KB,
                Bytes.BYTES_PER_GB);
    }
}
