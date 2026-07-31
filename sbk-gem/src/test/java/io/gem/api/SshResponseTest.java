/**
 * Copyright (c) KMG. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.gem.api;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Tests bounded SSH command diagnostics.
 */
final class SshResponseTest {
    @Test
    void retainsOnlyTheMostRecentDiagnosticBytes() throws IOException {
        final SshResponse response = new SshResponse(true, 8);

        response.stdOutputStream.write("012345".getBytes(StandardCharsets.UTF_8));
        response.stdOutputStream.write("6789".getBytes(StandardCharsets.UTF_8));
        response.errOutputStream.write("abcdefghijk".getBytes(StandardCharsets.UTF_8));

        assertEquals("23456789", response.stdOutputStream.toString());
        assertEquals("defghijk", response.errOutputStream.toString());
    }

    @Test
    void handlesWrappedWritesWithoutChangingByteOrder() throws IOException {
        final SshResponse response = new SshResponse(true, 5);

        response.stdOutputStream.write("abc".getBytes(StandardCharsets.UTF_8));
        response.stdOutputStream.write("de".getBytes(StandardCharsets.UTF_8));
        response.stdOutputStream.write("f".getBytes(StandardCharsets.UTF_8));

        assertEquals("bcdef", response.stdOutputStream.toString());
    }
}
