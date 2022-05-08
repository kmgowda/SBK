/**
 * Copyright (c) KMG. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.sbk.ram.impl;

import com.google.protobuf.Empty;
import io.grpc.Status;
import io.sbk.grpc.ClientID;
import io.sbk.grpc.Config;
import io.sbk.grpc.ServiceGrpc;
import io.sbk.logger.CountConnections;
import io.sbk.params.RamParameters;
import io.sbk.ram.RamRegistry;
import io.time.Time;
import org.jetbrains.annotations.NotNull;

import java.security.InvalidKeyException;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Class SbkGrpcService.
 */
final public class SbkGrpcService extends ServiceGrpc.ServiceImplBase {
    private final AtomicInteger connections;
    private final Config config;
    private final CountConnections countConnections;
    private final RamRegistry registry;
    private final RamParameters params;


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
    public SbkGrpcService(RamParameters params, Time time, long minLatency, long maxLatency,
                          CountConnections countConnections, RamRegistry registry) {
        super();
        connections = new AtomicInteger(0);
        Config.Builder builder = Config.newBuilder();
        builder.setStorageName(params.getStorageName());
        builder.setActionValue(params.getAction().ordinal());
        builder.setTimeUnitValue(time.getTimeUnit().ordinal());
        builder.setMaxLatency(maxLatency);
        builder.setMinLatency(minLatency);
        config = builder.build();
        this.params = params;
        this.countConnections = countConnections;
        this.registry = registry;
    }

    @Override
    public void getConfig(com.google.protobuf.Empty request,
                          io.grpc.stub.StreamObserver<io.sbk.grpc.Config> responseObserver) {
        if (connections.get() < params.getMaxConnections()) {
            responseObserver.onNext(config);
            responseObserver.onCompleted();
        } else {
            Status retError = Status.RESOURCE_EXHAUSTED.withDescription("SBK GRPC Server, Maximum clients Exceeded");
            responseObserver.onError(retError.asRuntimeException());
        }
    }

    @Override
    public void registerClient(io.sbk.grpc.Config request,
                               @NotNull io.grpc.stub.StreamObserver<io.sbk.grpc.ClientID> responseObserver) {
        responseObserver.onNext(ClientID.newBuilder().setId(registry.getID()).build());
        responseObserver.onCompleted();
        countConnections.incrementConnections();
        connections.incrementAndGet();
    }


    @Override
    public void addLatenciesRecord(io.sbk.grpc.LatenciesRecord request,
                                   io.grpc.stub.StreamObserver<com.google.protobuf.Empty> responseObserver) {
        try {
            registry.enQueue(request);
            if (responseObserver != null) {
                responseObserver.onNext(Empty.newBuilder().build());
                responseObserver.onCompleted();
            }
        } catch (IllegalStateException ex) {
            ex.printStackTrace();
            if (responseObserver != null) {
                responseObserver.onError(new InvalidKeyException());
            }
        }
    }

    @Override
    public void closeClient(io.sbk.grpc.ClientID request,
                            io.grpc.stub.StreamObserver<com.google.protobuf.Empty> responseObserver) {
        countConnections.decrementConnections();
        connections.decrementAndGet();
        if (responseObserver != null) {
            responseObserver.onNext(Empty.newBuilder().build());
            responseObserver.onCompleted();
        }
    }
}
