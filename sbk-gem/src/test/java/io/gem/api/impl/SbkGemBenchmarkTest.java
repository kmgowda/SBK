/**
 * Copyright (c) KMG. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.gem.api.impl;

import io.gem.api.RemoteResponse;
import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests distributed result classification independent of SSH transport.
 */
final class SbkGemBenchmarkTest {
    @Test
    void acceptsOnlyWhenEveryRemoteSbkSucceeds() {
        final RemoteResponse[] results = {new RemoteResponse(0, "", "", "node-a"),
                new RemoteResponse(0, "", "", "node-b")};

        assertNull(SbkGemBenchmark.remoteCommandFailure(results));
    }

    @Test
    void reportsEveryFailedRemoteSbk() {
        final RemoteResponse[] results = {new RemoteResponse(2, "", "bad option", "node-a"),
                new RemoteResponse(0, "", "", "node-b"), new RemoteResponse(17, "", "failure", "node-c")};

        final IOException failure = SbkGemBenchmark.remoteCommandFailure(results);
        assertTrue(failure.getMessage().contains("node-a returned 2"));
        assertTrue(failure.getMessage().contains("node-c returned 17"));
    }
}
