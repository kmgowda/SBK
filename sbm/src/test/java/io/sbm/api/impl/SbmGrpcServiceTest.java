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

import io.grpc.stub.StreamObserver;
import io.sbm.api.SbmRegistry;
import io.sbm.logger.CountConnections;
import io.sbm.params.impl.SbmParameters;
import io.sbp.grpc.ClientID;
import io.sbp.grpc.Config;
import io.time.MilliSeconds;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Tests SBP registration coordination used by distributed SBK-GEM runs.
 */
final class SbmGrpcServiceTest {
    @Test
    void releasesAllClientsOnlyAfterExpectedNodesRegister() throws Exception {
        final SbmParameters params = new SbmParameters("test", 0, 2, 0, null);
        params.parseArgs(new String[]{"-class", "file", "-action", "r", "-max", "2"});
        final CountConnections connections = mock(CountConnections.class);
        final SbmRegistry registry = mock(SbmRegistry.class);
        when(registry.getID()).thenReturn(10L, 11L);
        final SbmGrpcService service = new SbmGrpcService(params, new MilliSeconds(), 0, 1000,
                connections, registry, true);
        final CapturingObserver first = new CapturingObserver();
        final CapturingObserver second = new CapturingObserver();

        service.registerClient(Config.getDefaultInstance(), first);
        assertTrue(first.values.isEmpty());

        service.registerClient(Config.getDefaultInstance(), second);
        assertEquals(List.of(10L), first.values);
        assertEquals(List.of(11L), second.values);
        assertTrue(first.completed);
        assertTrue(second.completed);
    }

    private static final class CapturingObserver implements StreamObserver<ClientID> {
        private final List<Long> values = new ArrayList<>();
        private boolean completed;

        @Override
        public void onNext(ClientID value) {
            values.add(value.getId());
        }

        @Override
        public void onError(Throwable throwable) {
            throw new AssertionError(throwable);
        }

        @Override
        public void onCompleted() {
            completed = true;
        }
    }
}
