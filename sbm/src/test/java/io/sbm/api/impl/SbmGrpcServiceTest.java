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

import com.google.protobuf.Empty;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.grpc.Status;
import io.grpc.stub.StreamObserver;
import io.sbm.api.SbmRegistry;
import io.sbm.logger.CountConnections;
import io.sbm.params.impl.SbmParameters;
import io.sbp.grpc.ClientID;
import io.sbp.grpc.ClientFailure;
import io.sbp.grpc.Config;
import io.sbp.grpc.MessageLatenciesRecord;
import io.time.MilliSeconds;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTimeoutPreemptively;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.when;

/**
 * Tests SBP registration coordination used by distributed SBK-GEM runs.
 */
final class SbmGrpcServiceTest {
    @Test
    void advertisesTheConfiguredInboundRecordLimit() throws Exception {
        final SbmParameters params = new SbmParameters("test", 0, 1, 0, null);
        params.parseArgs(new String[]{"-class", "file", "-action", "r"});
        final SbmGrpcService service = new SbmGrpcService(params, new MilliSeconds(), 0, 1000,
                mock(CountConnections.class), mock(SbmRegistry.class), false, 4_194_304);
        final CapturingConfigObserver response = new CapturingConfigObserver();

        service.getConfig(Empty.getDefaultInstance(), response);

        assertEquals(4_194_304, response.value.getMaxRecordSizeBytes());
        assertTrue(response.completed);
    }

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

    @Test
    void atomicallyRejectsRegistrationsBeyondTheConnectionCap() throws Exception {
        final SbmParameters params = new SbmParameters("test", 0, 2, 0, null);
        params.parseArgs(new String[]{"-class", "file", "-action", "r", "-max", "2"});
        final CountConnections connections = mock(CountConnections.class);
        final SbmRegistry registry = mock(SbmRegistry.class);
        when(registry.getID()).thenReturn(10L, 11L);
        final SbmGrpcService service = new SbmGrpcService(params, new MilliSeconds(), 0, 1000,
                connections, registry);

        service.getConfig(Empty.getDefaultInstance(), new CapturingConfigObserver());
        service.getConfig(Empty.getDefaultInstance(), new CapturingConfigObserver());
        service.getConfig(Empty.getDefaultInstance(), new CapturingConfigObserver());
        final CapturingObserver first = new CapturingObserver();
        final CapturingObserver second = new CapturingObserver();
        final CapturingObserver excess = new CapturingObserver();

        service.registerClient(Config.getDefaultInstance(), first);
        service.registerClient(Config.getDefaultInstance(), second);
        service.registerClient(Config.getDefaultInstance(), excess);

        assertEquals(List.of(10L), first.values);
        assertEquals(List.of(11L), second.values);
        assertEquals(Status.Code.RESOURCE_EXHAUSTED, Status.fromThrowable(excess.failure).getCode());
        assertEquals(2, service.getMaximumRegisteredClients());
        verify(registry, times(2)).getID();
        verify(connections, times(2)).incrementConnections();
    }

    @Test
    void abortsPendingAndFutureRegistrationsAfterRemoteStartupFailure() throws Exception {
        final SbmParameters params = new SbmParameters("test", 0, 2, 0, null);
        params.parseArgs(new String[]{"-class", "file", "-action", "r", "-max", "2"});
        final CountConnections connections = mock(CountConnections.class);
        final SbmRegistry registry = mock(SbmRegistry.class);
        when(registry.getID()).thenReturn(10L);
        final SbmGrpcService service = new SbmGrpcService(params, new MilliSeconds(), 0, 1000,
                connections, registry, true);
        final CapturingObserver waiting = new CapturingObserver();
        final CapturingObserver late = new CapturingObserver();

        service.registerClient(Config.getDefaultInstance(), waiting);
        final int aborted = service.abortPendingRegistrations("node-b exited before distributed start");
        service.registerClient(Config.getDefaultInstance(), late);

        assertEquals(1, aborted);
        assertEquals(Status.Code.ABORTED, Status.fromThrowable(waiting.failure).getCode());
        assertEquals(Status.Code.ABORTED, Status.fromThrowable(late.failure).getCode());
        assertTrue(Status.fromThrowable(waiting.failure).getDescription().contains("node-b"));
        assertEquals(1, service.getMaximumRegisteredClients());
        verify(connections).incrementConnections();
        verify(connections).decrementConnections();
    }

    @Test
    void doesNotAbortRegistrationsAfterCoordinatedStartWasReleased() throws Exception {
        final SbmParameters params = new SbmParameters("test", 0, 1, 0, null);
        params.parseArgs(new String[]{"-class", "file", "-action", "r", "-max", "1"});
        final CountConnections connections = mock(CountConnections.class);
        final SbmRegistry registry = mock(SbmRegistry.class);
        when(registry.getID()).thenReturn(10L);
        final SbmGrpcService service = new SbmGrpcService(params, new MilliSeconds(), 0, 1000,
                connections, registry, true);
        final CapturingObserver observer = new CapturingObserver();

        service.registerClient(Config.getDefaultInstance(), observer);

        assertEquals(0, service.abortPendingRegistrations("late failure"));
        assertTrue(observer.completed);
        assertEquals(List.of(10L), observer.values);
        assertTrue(service.awaitCoordinatedStart(1, TimeUnit.MILLISECONDS));
        verify(connections, times(1)).incrementConnections();
    }

    @Test
    void coordinatedStartDeadlineReportsMissingClients() throws Exception {
        final SbmParameters params = new SbmParameters("test", 0, 2, 0, null);
        params.parseArgs(new String[]{"-class", "file", "-action", "r", "-max", "2"});
        final SbmGrpcService service = new SbmGrpcService(params, new MilliSeconds(), 0, 1000,
                mock(CountConnections.class), mock(SbmRegistry.class), true);

        assertFalse(service.awaitCoordinatedStart(1, TimeUnit.MILLISECONDS));
        service.abortPendingRegistrations("registration deadline expired");
        assertTrue(service.getRegistrationFailure().contains("deadline"));
    }

    @Test
    void abortBeforeAwaitCannotLoseTheCoordinatedStartNotification() throws Exception {
        final SbmParameters params = new SbmParameters("test", 0, 2, 0, null);
        params.parseArgs(new String[]{"-class", "file", "-action", "r", "-max", "2"});
        final SbmGrpcService service = new SbmGrpcService(params, new MilliSeconds(), 0, 1000,
                mock(CountConnections.class), mock(SbmRegistry.class), true);

        service.abortPendingRegistrations("remote startup failed before wait");

        assertTimeoutPreemptively(Duration.ofMillis(500),
                () -> assertFalse(service.awaitCoordinatedStart(5, TimeUnit.SECONDS)));
    }

    @Test
    void acceptsAnOrderedLatencyStreamAndAcknowledgesItsFinalDrain() throws Exception {
        final SbmParameters params = new SbmParameters("test", 0, 2, 0, null);
        params.parseArgs(new String[]{"-class", "file", "-action", "r", "-max", "2"});
        final CountConnections connections = mock(CountConnections.class);
        final SbmRegistry registry = mock(SbmRegistry.class);
        final SbmGrpcService service = new SbmGrpcService(params, new MilliSeconds(), 0, 1000,
                connections, registry);
        final CapturingEmptyObserver response = new CapturingEmptyObserver();
        final StreamObserver<MessageLatenciesRecord> stream = service.streamLatencies(response);
        final MessageLatenciesRecord first = MessageLatenciesRecord.newBuilder()
                .setClientID(1)
                .setSequenceNumber(1)
                .addLatencyValues(10)
                .addLatencyCounts(2)
                .build();
        final MessageLatenciesRecord second = MessageLatenciesRecord.newBuilder()
                .setClientID(1)
                .setSequenceNumber(2)
                .addLatencyValues(20)
                .addLatencyCounts(3)
                .build();

        stream.onNext(first);
        stream.onNext(second);
        stream.onCompleted();

        verify(registry).enQueue(first);
        verify(registry).enQueue(second);
        assertEquals(1, response.values);
        assertTrue(response.completed);
    }

    @Test
    void recordsAndAcknowledgesTerminalClientFailure() throws Exception {
        final SbmParameters params = new SbmParameters("test", 0, 1, 0, null);
        params.parseArgs(new String[]{"-class", "file", "-action", "r"});
        final SbmRegistry registry = mock(SbmRegistry.class);
        when(registry.getID()).thenReturn(7L);
        final SbmGrpcService service = new SbmGrpcService(params, new MilliSeconds(), 0, 1000,
                mock(CountConnections.class), registry);
        service.registerClient(Config.getDefaultInstance(), new CapturingObserver());
        final CapturingEmptyObserver response = new CapturingEmptyObserver();
        final ClientFailure report = ClientFailure.newBuilder()
                .setClientID(7)
                .setComponent("SBK")
                .setMessage("IOException: HTTP 503 Service Unavailable")
                .build();

        service.reportClientFailure(report, response);

        assertEquals(List.of(report), service.getClientFailures());
        assertEquals(1, response.values);
        assertTrue(response.completed);
    }

    @Test
    void rejectsTerminalFailureFromUnregisteredClient() throws Exception {
        final SbmParameters params = new SbmParameters("test", 0, 1, 0, null);
        params.parseArgs(new String[]{"-class", "file", "-action", "r"});
        final SbmGrpcService service = new SbmGrpcService(params, new MilliSeconds(), 0, 1000,
                mock(CountConnections.class), mock(SbmRegistry.class));
        final CapturingEmptyObserver response = new CapturingEmptyObserver();

        service.reportClientFailure(clientFailure(7, "SBK", "storage failure"), response);

        assertEquals(Status.Code.FAILED_PRECONDITION,
                Status.fromThrowable(response.failure).getCode());
        assertTrue(service.getClientFailures().isEmpty());
    }

    @Test
    void rejectsOversizedOrControlledTerminalFailureText() throws Exception {
        final SbmParameters params = new SbmParameters("test", 0, 1, 0, null);
        params.parseArgs(new String[]{"-class", "file", "-action", "r"});
        final SbmRegistry registry = mock(SbmRegistry.class);
        when(registry.getID()).thenReturn(7L);
        final SbmGrpcService service = new SbmGrpcService(params, new MilliSeconds(), 0, 1000,
                mock(CountConnections.class), registry);
        service.registerClient(Config.getDefaultInstance(), new CapturingObserver());
        final CapturingEmptyObserver oversizedComponent = new CapturingEmptyObserver();
        final CapturingEmptyObserver oversizedMessage = new CapturingEmptyObserver();
        final CapturingEmptyObserver controlledText = new CapturingEmptyObserver();

        service.reportClientFailure(clientFailure(7, "x".repeat(65), "storage failure"),
                oversizedComponent);
        service.reportClientFailure(clientFailure(7, "SBK", "x".repeat(4097)),
                oversizedMessage);
        service.reportClientFailure(clientFailure(7, "SBK\nforged-entry", "storage failure"),
                controlledText);

        assertEquals(Status.Code.INVALID_ARGUMENT,
                Status.fromThrowable(oversizedComponent.failure).getCode());
        assertEquals(Status.Code.INVALID_ARGUMENT,
                Status.fromThrowable(oversizedMessage.failure).getCode());
        assertEquals(Status.Code.INVALID_ARGUMENT,
                Status.fromThrowable(controlledText.failure).getCode());
        assertTrue(service.getClientFailures().isEmpty());
    }

    @Test
    void retainsAndLogsOnlyTheFirstTerminalFailurePerClient() throws Exception {
        final SbmParameters params = new SbmParameters("test", 0, 1, 0, null);
        params.parseArgs(new String[]{"-class", "file", "-action", "r"});
        final SbmRegistry registry = mock(SbmRegistry.class);
        when(registry.getID()).thenReturn(7L);
        final SbmGrpcService service = new SbmGrpcService(params, new MilliSeconds(), 0, 1000,
                mock(CountConnections.class), registry);
        service.registerClient(Config.getDefaultInstance(), new CapturingObserver());
        final ClientFailure first = clientFailure(7, "SBK", "first failure");
        final CapturingEmptyObserver firstResponse = new CapturingEmptyObserver();
        final CapturingEmptyObserver duplicateResponse = new CapturingEmptyObserver();

        service.reportClientFailure(first, firstResponse);
        service.reportClientFailure(clientFailure(7, "SBK", "duplicate failure"), duplicateResponse);

        assertEquals(List.of(first), service.getClientFailures());
        assertTrue(firstResponse.completed);
        assertTrue(duplicateResponse.completed);
    }

    @Test
    void boundsTerminalFailureRetentionAcrossSequentialClients() throws Exception {
        final SbmParameters params = new SbmParameters("test", 0, 1, 0, null);
        params.parseArgs(new String[]{"-class", "file", "-action", "r", "-max", "1"});
        final SbmRegistry registry = mock(SbmRegistry.class);
        when(registry.getID()).thenReturn(7L, 8L);
        final SbmGrpcService service = new SbmGrpcService(params, new MilliSeconds(), 0, 1000,
                mock(CountConnections.class), registry);
        service.registerClient(Config.getDefaultInstance(), new CapturingObserver());
        final ClientFailure retained = clientFailure(7, "SBK", "first failure");
        service.reportClientFailure(retained, new CapturingEmptyObserver());
        service.closeClient(ClientID.newBuilder().setId(7).build(), new CapturingEmptyObserver());
        service.registerClient(Config.getDefaultInstance(), new CapturingObserver());
        final CapturingEmptyObserver overflow = new CapturingEmptyObserver();

        service.reportClientFailure(clientFailure(8, "SBK", "second failure"), overflow);

        assertEquals(Status.Code.RESOURCE_EXHAUSTED,
                Status.fromThrowable(overflow.failure).getCode());
        assertEquals(List.of(retained), service.getClientFailures());
    }

    @Test
    void rejectsSequenceGapsBeforeTheyReachTheAggregator() throws Exception {
        final SbmParameters params = new SbmParameters("test", 0, 1, 0, null);
        params.parseArgs(new String[]{"-class", "file", "-action", "r"});
        final SbmRegistry registry = mock(SbmRegistry.class);
        final SbmGrpcService service = new SbmGrpcService(params, new MilliSeconds(), 0, 1000,
                mock(CountConnections.class), registry);
        final CapturingEmptyObserver response = new CapturingEmptyObserver();
        final StreamObserver<MessageLatenciesRecord> stream = service.streamLatencies(response);

        stream.onNext(MessageLatenciesRecord.newBuilder()
                .setClientID(1)
                .setSequenceNumber(2)
                .build());

        assertEquals(Status.Code.DATA_LOSS, Status.fromThrowable(response.failure).getCode());
    }

    @Test
    void rejectsMismatchedPackedLatencyArrays() throws Exception {
        final SbmParameters params = new SbmParameters("test", 0, 1, 0, null);
        params.parseArgs(new String[]{"-class", "file", "-action", "r"});
        final SbmRegistry registry = mock(SbmRegistry.class);
        final SbmGrpcService service = new SbmGrpcService(params, new MilliSeconds(), 0, 1000,
                mock(CountConnections.class), registry);
        final CapturingEmptyObserver response = new CapturingEmptyObserver();
        final StreamObserver<MessageLatenciesRecord> stream = service.streamLatencies(response);

        stream.onNext(MessageLatenciesRecord.newBuilder()
                .setClientID(1)
                .setSequenceNumber(1)
                .addLatencyValues(10)
                .build());

        assertEquals(Status.Code.INVALID_ARGUMENT, Status.fromThrowable(response.failure).getCode());
    }

    @Test
    void rejectsAClientIdChangeWithinOneStream() throws Exception {
        final SbmParameters params = new SbmParameters("test", 0, 1, 0, null);
        params.parseArgs(new String[]{"-class", "file", "-action", "r"});
        final SbmRegistry registry = mock(SbmRegistry.class);
        final SbmGrpcService service = new SbmGrpcService(params, new MilliSeconds(), 0, 1000,
                mock(CountConnections.class), registry);
        final CapturingEmptyObserver response = new CapturingEmptyObserver();
        final StreamObserver<MessageLatenciesRecord> stream = service.streamLatencies(response);

        stream.onNext(MessageLatenciesRecord.newBuilder()
                .setClientID(1)
                .setSequenceNumber(1)
                .build());
        stream.onNext(MessageLatenciesRecord.newBuilder()
                .setClientID(2)
                .setSequenceNumber(2)
                .build());

        assertEquals(Status.Code.INVALID_ARGUMENT,
                Status.fromThrowable(response.failure).getCode());
    }

    private static ClientFailure clientFailure(long clientID, String component, String message) {
        return ClientFailure.newBuilder()
                .setClientID(clientID)
                .setComponent(component)
                .setMessage(message)
                .build();
    }

    private static final class CapturingObserver implements StreamObserver<ClientID> {
        private final List<Long> values = new ArrayList<>();
        private boolean completed;
        private Throwable failure;

        @Override
        public void onNext(ClientID value) {
            values.add(value.getId());
        }

        @Override
        @SuppressFBWarnings(value = "EI_EXPOSE_REP2",
                justification = "The throwable is retained only for the test assertion")
        public void onError(Throwable throwable) {
            failure = throwable;
        }

        @Override
        public void onCompleted() {
            completed = true;
        }
    }

    private static final class CapturingEmptyObserver implements StreamObserver<Empty> {
        private int values;
        private boolean completed;
        private Throwable failure;

        @Override
        public void onNext(Empty value) {
            values++;
        }

        @Override
        @SuppressFBWarnings(value = "EI_EXPOSE_REP2",
                justification = "The throwable is retained only for the test assertion")
        public void onError(Throwable throwable) {
            failure = throwable;
        }

        @Override
        public void onCompleted() {
            completed = true;
        }
    }

    private static final class CapturingConfigObserver implements StreamObserver<Config> {
        private Config value;
        private boolean completed;

        @Override
        public void onNext(Config config) {
            value = config;
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
