/**
 * Copyright (c) KMG. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 */
package io.sbk.logger.impl;

import io.sbk.params.impl.SbkParameters;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.concurrent.CompletionException;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Verifies fail-fast validation of the SBM endpoint used by {@link GrpcLogger}.
 */
public final class GrpcLoggerOptionsTest {

    /**
     * A selected gRPC logger must not silently disable itself when the SBM host is absent.
     *
     * @throws Exception if the common SBK arguments cannot be parsed
     */
    @Test
    public void rejectsMissingSbmHost() throws Exception {
        final GrpcLogger logger = new GrpcLogger();
        final SbkParameters parameters = parameters(logger,
                "-writers", "1", "-size", "100", "-records", "1");

        final IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class, () -> logger.parseArgs(parameters));

        assertEquals("GrpcLogger requires '-sbm <hostname-or-IP>'", exception.getMessage());
    }

    /**
     * The former disabling value is rejected as an invalid endpoint.
     *
     * @throws Exception if the common SBK arguments cannot be parsed
     */
    @Test
    public void rejectsNoneSbmHost() throws Exception {
        final GrpcLogger logger = new GrpcLogger();
        final SbkParameters parameters = parameters(logger,
                "-writers", "1", "-size", "100", "-records", "1",
                "-sbm", "none");

        assertThrows(IllegalArgumentException.class,
                () -> logger.parseArgs(parameters));
    }

    /**
     * A non-numeric port is reported as an argument error.
     *
     * @throws Exception if the common SBK arguments cannot be parsed
     */
    @Test
    public void rejectsNonNumericSbmPort() throws Exception {
        final GrpcLogger logger = new GrpcLogger();
        final SbkParameters parameters = parameters(logger,
                "-writers", "1", "-size", "100", "-records", "1",
                "-sbm", "127.0.0.1", "-sbmport", "invalid");

        assertThrows(IllegalArgumentException.class,
                () -> logger.parseArgs(parameters));
    }

    /**
     * TCP ports outside the valid range are rejected before a channel is opened.
     *
     * @throws Exception if the common SBK arguments cannot be parsed
     */
    @Test
    public void rejectsOutOfRangeSbmPort() throws Exception {
        final GrpcLogger logger = new GrpcLogger();
        final SbkParameters parameters = parameters(logger,
                "-writers", "1", "-size", "100", "-records", "1",
                "-sbm", "127.0.0.1", "-sbmport", "65536");

        assertThrows(IllegalArgumentException.class,
                () -> logger.parseArgs(parameters));
    }

    /**
     * A valid explicit host and port pass endpoint validation.
     *
     * @throws Exception if the common SBK arguments cannot be parsed
     */
    @Test
    public void acceptsValidSbmEndpoint() throws Exception {
        final GrpcLogger logger = new GrpcLogger();
        final SbkParameters parameters = parameters(logger,
                "-writers", "1", "-size", "100", "-records", "1",
                "-sbm", "127.0.0.1", "-sbmport", "9717");

        assertDoesNotThrow(() -> logger.parseArgs(parameters));
    }

    /**
     * Terminal failure summaries unwrap asynchronous wrappers and normalize whitespace.
     */
    @Test
    public void unwrapsAndNormalizesTerminalFailureForSbm() {
        final String summary = GrpcLogger.failureSummary(
                new CompletionException(new IOException("HTTP 503\nService Unavailable")));

        assertEquals("IOException: HTTP 503 Service Unavailable", summary);
    }

    /**
     * Terminal failure summaries preserve the complete causal chain.
     */
    @Test
    public void includesTheTerminalFailureCauseChain() {
        final IOException failure = new IOException("MinIO write failed",
                new IllegalStateException("HTTP 503 Service Unavailable"));

        final String summary = GrpcLogger.failureSummary(new CompletionException(failure));

        assertEquals("IOException: MinIO write failed -> caused by "
                + "IllegalStateException: HTTP 503 Service Unavailable", summary);
    }

    /**
     * Long failure summaries retain context and the root cause within the protocol limit.
     */
    @Test
    public void retainsFailurePrefixAndRootCauseTailWithinTheCharacterLimit() {
        final String prefix = "MinIO request context ";
        final String rootCause = "ROOT CAUSE: HTTP 503 Service Unavailable";
        final IOException failure = new IOException(prefix + "x".repeat(5000),
                new IllegalStateException(rootCause));

        final String summary = GrpcLogger.failureSummary(failure);

        assertEquals(4096, summary.length());
        assertTrue(summary.startsWith("IOException: " + prefix));
        assertTrue(summary.contains(" ... [truncated] ... "));
        assertTrue(summary.endsWith("IllegalStateException: " + rootCause));
    }

    private static SbkParameters parameters(GrpcLogger logger, String... arguments)
            throws Exception {
        final SbkParameters parameters = new SbkParameters("grpc-options-test");
        logger.addArgs(parameters);
        parameters.parseArgs(arguments);
        return parameters;
    }
}
