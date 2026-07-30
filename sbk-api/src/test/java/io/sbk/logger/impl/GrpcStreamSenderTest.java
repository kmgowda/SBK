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

import com.google.protobuf.Empty;
import io.grpc.ManagedChannel;
import io.grpc.Server;
import io.grpc.inprocess.InProcessChannelBuilder;
import io.grpc.inprocess.InProcessServerBuilder;
import io.grpc.stub.StreamObserver;
import io.sbp.grpc.MessageLatenciesRecord;
import io.sbp.grpc.ServiceGrpc;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * Verifies ordered streaming and final acknowledgement of queued latency
 * batches.
 */
final class GrpcStreamSenderTest {
    @Test
    void drainsAllBatchesBeforeCompletingTheStream() throws Exception {
        final String serverName = InProcessServerBuilder.generateName();
        final List<Long> sequences = new ArrayList<>();
        final Server server = InProcessServerBuilder.forName(serverName)
                .directExecutor()
                .addService(new ServiceGrpc.ServiceImplBase() {
                    @Override
                    public StreamObserver<MessageLatenciesRecord> streamLatencies(
                            StreamObserver<Empty> responseObserver) {
                        return new StreamObserver<>() {
                            @Override
                            public void onNext(MessageLatenciesRecord value) {
                                sequences.add(value.getSequenceNumber());
                            }

                            @Override
                            public void onError(Throwable throwable) {
                            }

                            @Override
                            public void onCompleted() {
                                responseObserver.onNext(Empty.getDefaultInstance());
                                responseObserver.onCompleted();
                            }
                        };
                    }
                })
                .build()
                .start();
        final ManagedChannel channel = InProcessChannelBuilder.forName(serverName)
                .directExecutor()
                .build();
        final AtomicReference<Throwable> failure = new AtomicReference<>();

        try {
            final GrpcStreamSender sender = new GrpcStreamSender(
                    ServiceGrpc.newStub(channel), 4, failure::set);
            sender.send(MessageLatenciesRecord.newBuilder().setSequenceNumber(1).build());
            sender.send(MessageLatenciesRecord.newBuilder().setSequenceNumber(2).build());
            sender.close();

            assertEquals(List.of(1L, 2L), sequences);
            assertNull(failure.get());
        } finally {
            channel.shutdownNow();
            server.shutdownNow();
        }
    }
}
