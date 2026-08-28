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

import io.gem.agent.RemoteAgentProtocol;
import io.gem.api.SshResponse;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;

/** Tests runtime transport retry-target classification. */
final class RuntimeDeploymentTransportTest {
    @Test
    void retriesOnlySelectedArchiveDigestMismatches() throws Exception {
        final SshResponse digestMismatch = failure(RemoteAgentProtocol.ARCHIVE_DIGEST_MISMATCH);
        final SshResponse success = new SshResponse(true);
        final CompletableFuture<SshResponse>[] activations = futures(digestMismatch, success);

        assertArrayEquals(new boolean[]{true, false},
                RuntimeDeploymentTransport.archiveDigestMismatchTargets(activations,
                        new boolean[]{true, false}));
    }

    @Test
    void doesNotRetryWhenSelectedFailureIsNotIntegrityRelated() throws Exception {
        final CompletableFuture<SshResponse>[] activations = futures(failure("permission denied"),
                failure(RemoteAgentProtocol.ARCHIVE_DIGEST_MISMATCH));

        assertArrayEquals(new boolean[]{false, false},
                RuntimeDeploymentTransport.archiveDigestMismatchTargets(activations,
                        new boolean[]{true, true}));
    }

    private static SshResponse failure(String message) throws Exception {
        final SshResponse response = new SshResponse(true);
        response.returnCode = 70;
        response.errOutputStream.write(message.getBytes(StandardCharsets.UTF_8));
        return response;
    }

    @SafeVarargs
    private static CompletableFuture<SshResponse>[] futures(SshResponse... responses) {
        @SuppressWarnings("unchecked")
        final CompletableFuture<SshResponse>[] futures = new CompletableFuture[responses.length];
        for (int i = 0; i < responses.length; i++) {
            futures[i] = CompletableFuture.completedFuture(responses[i]);
        }
        return futures;
    }
}
