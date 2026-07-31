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
import io.gem.api.RemoteExecutionStatus;
import io.gem.api.SshCommandException;
import io.gem.api.SshResponse;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.SocketTimeoutException;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.CancellationException;

import static org.junit.jupiter.api.Assertions.assertEquals;
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
        assertTrue(failure.getMessage().contains("node-a status EXIT_FAILURE returned 2"));
        assertTrue(failure.getMessage().contains("node-c status EXIT_FAILURE returned 17"));
    }

    @Test
    void includesHostInDerivedExitFailureMessage() {
        final RemoteResponse response = new RemoteResponse(2, "", "bad option", "node-a");

        assertEquals("Host 'node-a' remote process returned exit code 2", response.failureMessage);
    }

    @Test
    void classifiesTransportFailureWithHostAndBoundedDiagnostics() throws IOException {
        final SshResponse partial = new SshResponse(true, 32);
        partial.stdOutputStream.write("startup output".getBytes(StandardCharsets.UTF_8));
        partial.errOutputStream.write("connection reset".getBytes(StandardCharsets.UTF_8));
        final SshCommandException exception = new SshCommandException("node-a", partial, false,
                new IOException("SSH channel closed"));

        final RemoteResponse result = SbkGemBenchmark.remoteCommandResult("node-a", null, exception);

        assertEquals(RemoteExecutionStatus.SSH_ERROR, result.status);
        assertEquals(RemoteResponse.UNKNOWN_RETURN_CODE, result.returnCode);
        assertEquals("startup output", result.stdOutput);
        assertEquals("connection reset", result.errOutput);
        assertTrue(result.failureMessage.contains("node-a"));
    }

    @Test
    void classifiesTimeoutWithoutIncludingTheRemoteCommand() {
        final SshCommandException exception = new SshCommandException("node-b", new SshResponse(true), true,
                new SocketTimeoutException("Remote command timed out after 10 seconds"));

        final RemoteResponse result = SbkGemBenchmark.remoteCommandResult("node-b", null, exception);

        assertEquals(RemoteExecutionStatus.TIMEOUT, result.status);
        assertTrue(result.failureMessage.contains("node-b"));
        assertTrue(result.failureMessage.contains("timed out"));
    }

    @Test
    void aggregatesMixedExitAndTransportFailures() {
        final RemoteResponse[] results = {
                new RemoteResponse(0, "", "", "node-a"),
                new RemoteResponse(7, "", "driver failed", "node-b"),
                new RemoteResponse(RemoteResponse.UNKNOWN_RETURN_CODE, "", "", "node-c",
                        RemoteExecutionStatus.TIMEOUT, "node-c timed out")
        };

        final IOException failure = SbkGemBenchmark.remoteCommandFailure(results);

        assertTrue(failure.getMessage().contains("node-b status EXIT_FAILURE returned 7"));
        assertTrue(failure.getMessage().contains("node-c status TIMEOUT"));
    }

    @Test
    void classifiesCancelledRemoteCommand() {
        final RemoteResponse result = SbkGemBenchmark.remoteCommandResult("node-d", null,
                new CancellationException("cancelled after peer failure"));

        assertEquals(RemoteExecutionStatus.CANCELLED, result.status);
        assertTrue(result.failureMessage.contains("node-d"));
    }

    @Test
    void marksPartialOrMissingSbmParticipationAsIncomplete() {
        final RemoteResponse[] mixed = {new RemoteResponse(0, "", "", "node-a"),
                new RemoteResponse(9, "", "", "node-b")};
        final RemoteResponse[] successful = {new RemoteResponse(0, "", "", "node-a"),
                new RemoteResponse(0, "", "", "node-b")};

        assertEquals("INCOMPLETE", SbkGem.distributedRunStatus(mixed, 2));
        assertEquals("INCOMPLETE", SbkGem.distributedRunStatus(successful, 1));
        assertEquals("SUCCESS", SbkGem.distributedRunStatus(successful, 2));
    }
}
