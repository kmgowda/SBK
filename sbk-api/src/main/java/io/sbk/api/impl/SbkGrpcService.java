/**
 * Copyright (c) KMG. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 */

package io.sbk.api.impl;

import com.google.protobuf.Empty;
import io.sbk.api.Action;
import io.sbk.api.ConnectionsCount;
import io.sbk.api.TransactionRecord;
import io.sbk.perl.Time;

import java.security.InvalidKeyException;
import java.util.Hashtable;
import java.util.Queue;
import java.util.concurrent.atomic.AtomicInteger;

public class SbkGrpcService extends ServiceGrpc.ServiceImplBase {
    private final AtomicInteger clientID;
    private final Hashtable<Integer, TransactionRecord> table;
    private final Config config;
    private final ConnectionsCount connectionsCount;
    private final Queue<TransactionRecord> outQueue;


    public SbkGrpcService(String storageName, Action action, Time time, long minLatency, long maxLatency,
                          ConnectionsCount connectionsCount, Queue<TransactionRecord> outQueue) {
        super();
        clientID = new AtomicInteger(0);
        table = new Hashtable<>();
        Config.Builder builder = Config.newBuilder();
        builder.setStorageName(storageName);
        builder.setActionValue(action.ordinal());
        builder.setTimeUnitValue(time.getTimeUnit().ordinal());
        builder.setMaxLatency(maxLatency);
        builder.setMinLatency(minLatency);
        config = builder.build();
        this.connectionsCount = connectionsCount;
        this.outQueue = outQueue;

    }

    @Override
    public void getConfig(com.google.protobuf.Empty request,
                          io.grpc.stub.StreamObserver<io.sbk.api.impl.Config> responseObserver) {
        responseObserver.onNext(config);
        responseObserver.onCompleted();
    }

    @Override
    public void registerClient(io.sbk.api.impl.Config request,
                               io.grpc.stub.StreamObserver<io.sbk.api.impl.ClientID> responseObserver) {
        responseObserver.onNext(ClientID.newBuilder().setId(clientID.incrementAndGet()).build());
        responseObserver.onCompleted();
        connectionsCount.incrementConnections(1);
    }

    @Override
    public void startTransaction(io.sbk.api.impl.Transaction request,
                                 io.grpc.stub.StreamObserver<com.google.protobuf.Empty> responseObserver) {
        TransactionRecord trans = table.get(request.getClientID());
        if (trans != null) {
            trans.list.clear();
            table.remove(request.getClientID());
        }

        table.put(request.getClientID(), new TransactionRecord(request.getTransID()));
        if (responseObserver != null) {
            responseObserver.onNext(Empty.newBuilder().build());
            responseObserver.onCompleted();
        }
    }

    @Override
    public void addLatenciesRecord(io.sbk.api.impl.LatenciesRecord request,
                                   io.grpc.stub.StreamObserver<com.google.protobuf.Empty> responseObserver) {
        TransactionRecord trans = table.getOrDefault(request.getTransaction().getClientID(), null);
        if (trans != null && trans.transID != request.getTransaction().getTransID()) {
            trans.record.totalBytes += request.getTotalBytes();
            trans.record.totalRecords += request.getTotalRecords();
            trans.record.totalLatency += request.getTotalLatency();
            trans.record.maxLatency = Math.max(trans.record.maxLatency, request.getMaxLatency());
            trans.record.higherLatencyDiscardRecords += request.getHigherLatencyDiscardRecords();
            trans.record.lowerLatencyDiscardRecords += request.getLowerLatencyDiscardRecords();
            trans.record.invalidLatencyRecords += request.getInvalidLatencyRecords();
            trans.record.validLatencyRecords += request.getValidLatencyRecords();
            trans.writers = request.getWriters();
            trans.readers = request.getReaders();
            trans.maxReaders = Math.max(trans.maxReaders, request.getMaxReaders());
            trans.maxWriters = Math.max(trans.maxWriters, request.getMaxWriters());

            if (responseObserver != null) {
                responseObserver.onNext(Empty.newBuilder().build());
                responseObserver.onCompleted();
            }
        } else {
            if (responseObserver != null) {
                responseObserver.onError(new InvalidKeyException());
            }
        }
    }

    @Override
    public void addLatenciesList(io.sbk.api.impl.LatenciesList request,
                                 io.grpc.stub.StreamObserver<com.google.protobuf.Empty> responseObserver) {
        TransactionRecord trans = table.getOrDefault(request.getTransaction().getClientID(), null);
        if (trans != null && trans.transID != request.getTransaction().getTransID()) {
            trans.list.add(request.getLatenciesMap());
            if (responseObserver != null) {
                responseObserver.onNext(Empty.newBuilder().build());
                responseObserver.onCompleted();
            }
        } else {
            if (responseObserver != null) {
                responseObserver.onError(new InvalidKeyException());
            }
        }
    }

    @Override
    public void endTransaction(io.sbk.api.impl.Transaction request,
                               io.grpc.stub.StreamObserver<com.google.protobuf.Empty> responseObserver) {
        TransactionRecord trans = table.get(request.getClientID());
        try {
            outQueue.add(trans);
        } catch (IllegalStateException ex) {
            ex.printStackTrace();
            if (responseObserver != null) {
                responseObserver.onError(ex);
            }
        }
        if (responseObserver != null) {
            responseObserver.onNext(Empty.newBuilder().build());
            responseObserver.onCompleted();
        }
    }

    @Override
    public void closeClient(io.sbk.api.impl.ClientID request,
                            io.grpc.stub.StreamObserver<com.google.protobuf.Empty> responseObserver) {
        TransactionRecord trans = table.get(request.getId());
        trans.list.clear();
        table.remove(request.getId());
        connectionsCount.decrementConnections(1);
        if (responseObserver != null) {
            responseObserver.onNext(Empty.newBuilder().build());
            responseObserver.onCompleted();
        }
    }
}
