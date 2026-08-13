/**
 * Copyright (c) KMG. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 */
package io.sbm.api.impl;

import io.sbp.grpc.ClientFailure;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

/**
 * Tests terminal SBM outcome aggregation outside latency ingestion paths.
 */
final class SbmBenchmarkTest {
    @Test
    void succeedsWithoutLocalOrClientFailures() {
        assertNull(SbmBenchmark.terminalFailure(null, List.of()));
    }

    @Test
    void usesFirstClientFailureAndSuppressesLaterReportsInOrder() {
        final Throwable failure = SbmBenchmark.terminalFailure(null, List.of(
                clientFailure(7, "SBK", "MinIO write failed"),
                clientFailure(8, "SBK", "Kafka read failed")));

        assertEquals("SBM client 7 (SBK) reported terminal failure: MinIO write failed",
                failure.getMessage());
        assertEquals(1, failure.getSuppressed().length);
        assertEquals("SBM client 8 (SBK) reported terminal failure: Kafka read failed",
                failure.getSuppressed()[0].getMessage());
    }

    @Test
    void keepsLocalAggregationFailureAuthoritative() {
        final IOException localFailure = new IOException("latency aggregation failed");

        final Throwable failure = SbmBenchmark.terminalFailure(localFailure,
                List.of(clientFailure(7, "SBK", "storage failed")));

        assertSame(localFailure, failure);
        assertEquals(1, failure.getSuppressed().length);
        assertEquals("SBM client 7 (SBK) reported terminal failure: storage failed",
                failure.getSuppressed()[0].getMessage());
    }

    private static ClientFailure clientFailure(long clientID, String component, String message) {
        return ClientFailure.newBuilder()
                .setClientID(clientID)
                .setComponent(component)
                .setMessage(message)
                .build();
    }
}
