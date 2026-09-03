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

import com.google.protobuf.BoolValue;
import com.google.protobuf.Empty;
import io.grpc.Status;
import io.grpc.stub.StreamObserver;
import io.sbp.api.Sbp;
import io.sbp.config.SbpVersion;
import io.sbp.config.SbpFailureLimits;
import io.sbp.grpc.ClientID;
import io.sbp.grpc.ClientFailure;
import io.sbp.grpc.Config;
import io.sbp.grpc.MessageLatenciesRecord;
import io.sbp.grpc.ServiceGrpc;
import io.sbm.logger.CountConnections;
import io.sbm.params.RamParameters;
import io.sbm.api.SbmRegistry;
import io.sbp.grpc.Version;
import io.time.Time;
import io.sbk.system.Printer;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * gRPC service implementation for SBM.
 *
 * <p>Exposes RPCs for clients to:
 * - query SBP version and compatibility,
 * - obtain server configuration,
 * - register/unregister a client ID,
 * - stream latency records for aggregation.
 *
 * <p>Tracks current and maximum connections via {@link CountConnections} and forwards
 * records to an {@link SbmRegistry} for queueing/processing.
 */
final public class SbmGrpcService extends ServiceGrpc.ServiceImplBase {
    private final AtomicInteger connections;
    private final Config config;
    private final CountConnections countConnections;
    private final SbmRegistry registry;
    private final RamParameters params;
    private final List<PendingRegistration> pendingRegistrations;
    private final Set<Long> registeredClientIDs;
    private final Map<Long, ClientFailure> clientFailures;
    private boolean startReleased;
    /** Whether every expected coordinated client has registered and is waiting for controller release. */
    private boolean startReady;
    private String registrationFailure;
    private int maximumRegisteredClients;


    /**
     * Constructor SbkGrpcService initializing all values.
     *
     * @param params                RamParameters
     * @param time                  Time
     * @param minLatency            long
     * @param maxLatency            long
     * @param countConnections      CountConnections
     * @param registry              RamRegistry
     */
    public SbmGrpcService(RamParameters params, Time time, long minLatency, long maxLatency,
                          CountConnections countConnections, SbmRegistry registry) {
        this(params, time, minLatency, maxLatency, countConnections, registry, false, 0);
    }

    /**
     * Create the SBP service with optional registration barrier support.
     *
     * @param params SBM parameters
     * @param time latency time implementation
     * @param minLatency minimum accepted latency
     * @param maxLatency maximum accepted latency
     * @param countConnections connection metrics
     * @param registry latency-record registry
     * @param coordinatedStart whether all expected clients must register before any registration completes
     */
    public SbmGrpcService(RamParameters params, Time time, long minLatency, long maxLatency,
                          CountConnections countConnections, SbmRegistry registry, boolean coordinatedStart) {
        this(params, time, minLatency, maxLatency, countConnections, registry, coordinatedStart, 0);
    }

    /**
     * Create the SBP service with registration-barrier and transport-size configuration.
     *
     * @param params SBM parameters
     * @param time latency time implementation
     * @param minLatency minimum accepted latency
     * @param maxLatency maximum accepted latency
     * @param countConnections connection metrics
     * @param registry latency-record registry
     * @param coordinatedStart whether all expected clients must register before any registration completes
     * @param maxRecordSizeBytes maximum accepted serialized latency-record size
     */
    public SbmGrpcService(RamParameters params, Time time, long minLatency, long maxLatency,
                          CountConnections countConnections, SbmRegistry registry, boolean coordinatedStart,
                          long maxRecordSizeBytes) {
        super();
        connections = new AtomicInteger(0);
        Config.Builder builder = Config.newBuilder();
        builder.setStorageName(params.getStorageName());
        builder.setActionValue(params.getAction().ordinal());
        builder.setTimeUnitValue(time.getTimeUnit().ordinal());
        builder.setMaxLatency(maxLatency);
        builder.setMinLatency(minLatency);
        builder.setMaxRecordSizeBytes(maxRecordSizeBytes);
        config = builder.build();
        this.params = params;
        this.countConnections = countConnections;
        this.registry = registry;
        this.pendingRegistrations = new ArrayList<>();
        this.registeredClientIDs = new HashSet<>();
        this.clientFailures = new LinkedHashMap<>();
        this.startReleased = !coordinatedStart;
        this.startReady = !coordinatedStart;
        this.registrationFailure = null;
        this.maximumRegisteredClients = 0;
    }

    @Override
    public void getVersion(com.google.protobuf.Empty request,
                           io.grpc.stub.StreamObserver<io.sbp.grpc.Version> responseObserver) {
        // Respond with the SBP protocol version supported by this server instance
        try {
            final SbpVersion version = Sbp.getVersion();
            final Version.Builder outVersion = Version.newBuilder();
            outVersion.setMajor(version.major);
            outVersion.setMinor(version.minor);
            responseObserver.onNext(outVersion.build());
            responseObserver.onCompleted();
        } catch (IOException e) {
            Status retError = Status.UNAVAILABLE.
                    withDescription("SBM, Could not get SBP version");
            responseObserver.onError(retError.asRuntimeException());
        }
    }

    @Override
    public void isVersionSupported(io.sbp.grpc.Version request,
                                   io.grpc.stub.StreamObserver<com.google.protobuf.BoolValue> responseObserver) {
        // Validate that the client's major version matches the server's supported major version
        try {
            final SbpVersion version = Sbp.getVersion();
            if (version.major == request.getMajor()) {
                responseObserver.onNext(BoolValue.of(true));
                responseObserver.onCompleted();
            } else {
                Status retError = Status.INVALID_ARGUMENT.
                        withDescription("SBM, SBP Version mismatch, received Major version: "+request.getMajor() +
                                ", Expected Major version: "+version.major);
                responseObserver.onNext(BoolValue.of(false));
                responseObserver.onError(retError.asRuntimeException());
            }
        } catch (IOException e) {
            Status retError = Status.UNAVAILABLE.
                    withDescription("SBM, Could not get SBP version");
            responseObserver.onError(retError.asRuntimeException());
        }
    }

    @Override
    public void getConfig(com.google.protobuf.Empty request,
                          io.grpc.stub.StreamObserver<io.sbp.grpc.Config> responseObserver) {
        // Provide configuration to clients while enforcing max connections
        if (connections.get() < params.getMaxConnections()) {
            responseObserver.onNext(config);
            responseObserver.onCompleted();
        } else {
            Status retError = Status.RESOURCE_EXHAUSTED.withDescription(
                    "SBM maximum client connections reached: " + params.getMaxConnections());
            responseObserver.onError(retError.asRuntimeException());
        }
    }

    @Override
    public void registerClient(io.sbp.grpc.Config request,
                               @NotNull io.grpc.stub.StreamObserver<io.sbp.grpc.ClientID> responseObserver) {
        ClientID completedClientID = null;
        Status failure = null;
        synchronized (this) {
            if (registrationFailure != null) {
                failure = Status.ABORTED.withDescription(registrationFailure);
            } else {
                final int registered = connections.incrementAndGet();
                if (registered > params.getMaxConnections()) {
                    connections.decrementAndGet();
                    failure = Status.RESOURCE_EXHAUSTED.withDescription(
                            "SBM maximum client connections reached: " + params.getMaxConnections());
                } else {
                    final ClientID clientID = ClientID.newBuilder().setId(registry.getID()).build();
                    registeredClientIDs.add(clientID.getId());
                    countConnections.incrementConnections();
                    maximumRegisteredClients = Math.max(maximumRegisteredClients, registered);
                    if (startReleased) {
                        completedClientID = clientID;
                    } else {
                        pendingRegistrations.add(new PendingRegistration(responseObserver, clientID));
                        if (registered >= params.getMaxConnections()) {
                            startReady = true;
                            notifyAll();
                        }
                    }
                }
            }
        }
        if (failure != null) {
            responseObserver.onError(failure.asRuntimeException());
        } else if (completedClientID != null) {
            completeRegistration(responseObserver, completedClientID);
        }
    }

    /**
     * Abort clients waiting at the coordinated-start barrier.
     *
     * <p>This is a control-plane operation used by SBK-GEM when a remote SBK
     * process fails before all expected clients have registered.
     *
     * @param reason host-tagged distributed-run failure
     * @return number of pending registrations failed
     */
    public int abortPendingRegistrations(String reason) {
        final List<PendingRegistration> abortedRegistrations;
        synchronized (this) {
            if (startReleased || registrationFailure != null) {
                return 0;
            }
            registrationFailure = reason;
            abortedRegistrations = List.copyOf(pendingRegistrations);
            for (PendingRegistration registration : abortedRegistrations) {
                registeredClientIDs.remove(registration.clientID().getId());
                countConnections.decrementConnections();
            }
            connections.addAndGet(-abortedRegistrations.size());
            pendingRegistrations.clear();
            notifyAll();
        }
        final Status status = Status.ABORTED.withDescription(reason);
        for (PendingRegistration registration : abortedRegistrations) {
            registration.observer().onError(status.asRuntimeException());
        }
        final int aborted = abortedRegistrations.size();
        return aborted;
    }

    /**
     * Wait for all coordinated clients to register or for startup to fail.
     *
     * @param timeout maximum wait duration
     * @param unit timeout unit
     * @return true when every expected client is registered and waiting for release
     * @throws InterruptedException if the waiting controller thread is interrupted
     */
    public synchronized boolean awaitCoordinatedStart(long timeout, TimeUnit unit) throws InterruptedException {
        final long timeoutNanos = unit.toNanos(timeout);
        final long started = System.nanoTime();
        long remaining = timeoutNanos;
        while (!startReady && registrationFailure == null && remaining > 0) {
            TimeUnit.NANOSECONDS.timedWait(this, remaining);
            remaining = timeoutNanos - (System.nanoTime() - started);
        }
        return startReady && registrationFailure == null;
    }

    /**
     * Release every prepared client after the controller has started latency aggregation.
     *
     * @return number of coordinated clients released
     * @throws IllegalStateException when all expected clients have not registered
     */
    public int releaseCoordinatedStart() {
        final List<PendingRegistration> releasedRegistrations;
        synchronized (this) {
            if (registrationFailure != null) {
                throw new IllegalStateException("SBM coordinated start was aborted: " + registrationFailure);
            }
            if (!startReady) {
                throw new IllegalStateException("SBM coordinated start is not ready for release");
            }
            if (startReleased) {
                return 0;
            }
            startReleased = true;
            releasedRegistrations = List.copyOf(pendingRegistrations);
            pendingRegistrations.clear();
            notifyAll();
        }
        for (PendingRegistration registration : releasedRegistrations) {
            completeRegistration(registration.observer(), registration.clientID());
        }
        final int released = releasedRegistrations.size();
        return released;
    }

    /**
     * Return the largest number of clients registered concurrently.
     *
     * @return maximum registered clients observed by this SBM service
     */
    public synchronized int getMaximumRegisteredClients() {
        return maximumRegisteredClients;
    }

    /**
     * Return a coordinated-start failure, if one was recorded.
     *
     * @return failure description, or {@code null} when startup has not failed
     */
    public synchronized String getRegistrationFailure() {
        return registrationFailure;
    }

    /**
     * Records and acknowledges a terminal failure reported by an SBK client.
     *
     * @param request client-tagged terminal failure
     * @param responseObserver acknowledgement observer
     */
    @Override
    public void reportClientFailure(ClientFailure request,
                                    StreamObserver<Empty> responseObserver) {
        final long clientID = request.getClientID();
        Status failure = null;
        boolean logFailure = false;
        synchronized (this) {
            if (!registeredClientIDs.contains(clientID)) {
                failure = Status.FAILED_PRECONDITION
                        .withDescription("SBM terminal failure has an unregistered client ID");
            } else if (!isValidFailureText(request.getComponent(), SbpFailureLimits.COMPONENT_CHARACTERS)
                    || !isValidFailureText(request.getMessage(), SbpFailureLimits.MESSAGE_CHARACTERS)) {
                failure = Status.INVALID_ARGUMENT
                        .withDescription("SBM terminal failure contains invalid or oversized text");
            } else if (!clientFailures.containsKey(clientID)) {
                if (clientFailures.size() >= params.getMaxConnections()) {
                    failure = Status.RESOURCE_EXHAUSTED
                            .withDescription("SBM terminal failure retention limit reached");
                } else {
                    clientFailures.put(clientID, request);
                    logFailure = true;
                }
            }
        }
        if (failure != null) {
            responseObserver.onError(failure.asRuntimeException());
        } else {
            if (logFailure) {
                Printer.log.error("SBM received terminal failure from " + request.getComponent()
                        + " client " + clientID + ": " + request.getMessage());
            }
            acknowledge(responseObserver);
        }
    }

    /**
     * Returns a stable snapshot of terminal client failures received by SBM.
     *
     * @return terminal client-failure reports
     */
    public synchronized List<ClientFailure> getClientFailures() {
        return List.copyOf(clientFailures.values());
    }

    private static boolean isValidFailureText(String value, int maximumCharacters) {
        if (value.isBlank() || value.length() > maximumCharacters) {
            return false;
        }
        return value.chars().noneMatch(Character::isISOControl);
    }

    private static void acknowledge(StreamObserver<Empty> responseObserver) {
        responseObserver.onNext(Empty.getDefaultInstance());
        responseObserver.onCompleted();
    }


    /**
     * Opens a persistent ordered latency stream for an SBK client.
     *
     * @param responseObserver final stream acknowledgement observer
     * @return observer accepting ordered latency records
     */
    @Override
    public StreamObserver<MessageLatenciesRecord> streamLatencies(
            StreamObserver<Empty> responseObserver) {
        return new StreamObserver<>() {
            private long clientId = -1;
            private long expectedSequence = 1;
            private boolean terminated;

            @Override
            public void onNext(MessageLatenciesRecord record) {
                if (terminated) {
                    return;
                }
                final long recordClientId = record.getClientID();
                final long sequenceNumber = record.getSequenceNumber();
                if (clientId < 0) {
                    clientId = recordClientId;
                }
                if (recordClientId != clientId) {
                    fail(Status.INVALID_ARGUMENT.withDescription(
                            "SBM latency stream changed client ID from " + clientId
                                    + " to " + recordClientId));
                } else if (sequenceNumber != expectedSequence) {
                    fail(Status.DATA_LOSS.withDescription(
                            "SBM latency stream sequence mismatch for client " + clientId
                                    + ": expected " + expectedSequence + " but received "
                                    + sequenceNumber));
                } else {
                    try {
                        validateLatencyFields(record);
                        registry.enQueue(record);
                        expectedSequence++;
                    } catch (IllegalArgumentException exception) {
                        fail(Status.INVALID_ARGUMENT
                                .withDescription(exception.getMessage())
                                .withCause(exception));
                    } catch (IllegalStateException exception) {
                        fail(Status.RESOURCE_EXHAUSTED
                                .withDescription("SBM latency queue rejected a streamed record")
                                .withCause(exception));
                    }
                }
            }

            @Override
            public void onError(Throwable throwable) {
                terminated = true;
            }

            @Override
            public void onCompleted() {
                if (!terminated) {
                    terminated = true;
                    responseObserver.onNext(Empty.getDefaultInstance());
                    responseObserver.onCompleted();
                }
            }

            private void fail(Status status) {
                terminated = true;
                responseObserver.onError(status.asRuntimeException());
            }
        };
    }

    private static void validateLatencyFields(MessageLatenciesRecord record) {
        final int latencyValuesCount = record.getLatencyValuesCount();
        final int latencyCountsCount = record.getLatencyCountsCount();
        if (latencyValuesCount != latencyCountsCount) {
            throw new IllegalArgumentException("SBM packed latency values/counts have different lengths: "
                    + latencyValuesCount + " and " + latencyCountsCount);
        }
    }

    @Override
    public void closeClient(io.sbp.grpc.ClientID request,
                            io.grpc.stub.StreamObserver<com.google.protobuf.Empty> responseObserver) {
        // Decrement counters upon client disconnect and acknowledge
        synchronized (this) {
            if (registeredClientIDs.remove(request.getId())) {
                countConnections.decrementConnections();
                connections.decrementAndGet();
            }
        }
        if (responseObserver != null) {
            responseObserver.onNext(Empty.getDefaultInstance());
            responseObserver.onCompleted();
        }
    }

    private static void completeRegistration(io.grpc.stub.StreamObserver<ClientID> observer, ClientID clientID) {
        observer.onNext(clientID);
        observer.onCompleted();
    }

    private record PendingRegistration(io.grpc.stub.StreamObserver<ClientID> observer, ClientID clientID) {
    }
}
