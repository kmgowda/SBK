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
import io.grpc.Status;
import io.grpc.ManagedChannel;
import io.grpc.Server;
import io.grpc.inprocess.InProcessChannelBuilder;
import io.grpc.inprocess.InProcessServerBuilder;
import io.grpc.stub.ClientCallStreamObserver;
import io.grpc.stub.ClientResponseObserver;
import io.grpc.stub.StreamObserver;
import io.sbp.grpc.MessageLatenciesRecord;
import io.sbp.grpc.ServiceGrpc;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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
                    ServiceGrpc.newStub(channel), 4, 5, 30, failure::set);
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

    @Test
    @SuppressWarnings("unchecked")
    void rejectsBatchesWhenTheBoundedQueueIsFull() throws Exception {
        final ServiceGrpc.ServiceStub stub = mock(ServiceGrpc.ServiceStub.class);
        final ClientCallStreamObserver<MessageLatenciesRecord> requestStream =
                mock(ClientCallStreamObserver.class);
        when(requestStream.isReady()).thenReturn(false);
        when(stub.streamLatencies(any())).thenAnswer(invocation -> {
            final ClientResponseObserver<MessageLatenciesRecord, Empty> responseObserver =
                    invocation.getArgument(0);
            responseObserver.beforeStart(requestStream);
            return requestStream;
        });
        final AtomicReference<Throwable> failure = new AtomicReference<>();
        final GrpcStreamSender sender = new GrpcStreamSender(stub, 1, 5, 30, failure::set);

        sender.send(MessageLatenciesRecord.getDefaultInstance());
        verify(requestStream, timeout(1_000).atLeastOnce()).isReady();
        sender.send(MessageLatenciesRecord.getDefaultInstance());

        final IOException exception = assertThrows(IOException.class,
                () -> sender.send(MessageLatenciesRecord.getDefaultInstance()));
        assertTrue(exception.getMessage().contains("pending batch queue is full"));
        assertEquals(exception, failure.get());
    }

    @Test
    void closeSurfacesServerStreamFailure() throws Exception {
        final String serverName = InProcessServerBuilder.generateName();
        final Server server = InProcessServerBuilder.forName(serverName)
                .directExecutor()
                .addService(new ServiceGrpc.ServiceImplBase() {
                    @Override
                    public StreamObserver<MessageLatenciesRecord> streamLatencies(
                            StreamObserver<Empty> responseObserver) {
                        return new StreamObserver<>() {
                            @Override
                            public void onNext(MessageLatenciesRecord value) {
                                responseObserver.onError(Status.UNAVAILABLE
                                        .withDescription("test stream failure")
                                        .asRuntimeException());
                            }

                            @Override
                            public void onError(Throwable throwable) {
                            }

                            @Override
                            public void onCompleted() {
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
        final CountDownLatch failureReported = new CountDownLatch(1);

        try {
            final GrpcStreamSender sender = new GrpcStreamSender(
                    ServiceGrpc.newStub(channel), 4, 5, 30, throwable -> {
                        failure.set(throwable);
                        failureReported.countDown();
                    });
            sender.send(MessageLatenciesRecord.getDefaultInstance());

            assertTrue(failureReported.await(2, TimeUnit.SECONDS));
            final IOException exception = assertThrows(IOException.class, sender::close);
            assertEquals(failure.get(), exception.getCause());
        } finally {
            channel.shutdownNow();
            server.shutdownNow();
        }
    }

    @Test
    void serverShutdownFailsTheActiveStream() throws Exception {
        final String serverName = InProcessServerBuilder.generateName();
        final CountDownLatch batchReceived = new CountDownLatch(1);
        final Server server = InProcessServerBuilder.forName(serverName)
                .directExecutor()
                .addService(new ServiceGrpc.ServiceImplBase() {
                    @Override
                    public StreamObserver<MessageLatenciesRecord> streamLatencies(
                            StreamObserver<Empty> responseObserver) {
                        return new StreamObserver<>() {
                            @Override
                            public void onNext(MessageLatenciesRecord value) {
                                batchReceived.countDown();
                            }

                            @Override
                            public void onError(Throwable throwable) {
                            }

                            @Override
                            public void onCompleted() {
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
        final CountDownLatch failureReported = new CountDownLatch(1);

        try {
            final GrpcStreamSender sender = new GrpcStreamSender(
                    ServiceGrpc.newStub(channel), 4, 5, 30, throwable -> {
                        failure.set(throwable);
                        failureReported.countDown();
                    });
            sender.send(MessageLatenciesRecord.getDefaultInstance());
            assertTrue(batchReceived.await(2, TimeUnit.SECONDS));

            server.shutdownNow();

            assertTrue(failureReported.await(2, TimeUnit.SECONDS));
            assertNotNull(failure.get());
            assertThrows(IOException.class, sender::close);
        } finally {
            channel.shutdownNow();
            server.shutdownNow();
        }
    }

    @Test
    @SuppressWarnings("unchecked")
    void unexpectedServerCompletionFailsTheActiveBenchmark() throws Exception {
        final ServiceGrpc.ServiceStub stub = mock(ServiceGrpc.ServiceStub.class);
        final ClientCallStreamObserver<MessageLatenciesRecord> requestStream =
                mock(ClientCallStreamObserver.class);
        final AtomicReference<ClientResponseObserver<MessageLatenciesRecord, Empty>> response =
                new AtomicReference<>();
        when(requestStream.isReady()).thenReturn(true);
        when(stub.streamLatencies(any())).thenAnswer(invocation -> {
            final ClientResponseObserver<MessageLatenciesRecord, Empty> responseObserver =
                    invocation.getArgument(0);
            response.set(responseObserver);
            responseObserver.beforeStart(requestStream);
            return requestStream;
        });
        final AtomicReference<Throwable> failure = new AtomicReference<>();
        final CountDownLatch failureReported = new CountDownLatch(1);
        final GrpcStreamSender sender = new GrpcStreamSender(stub, 4, 5, 30, throwable -> {
            failure.set(throwable);
            failureReported.countDown();
        });

        response.get().onCompleted();

        assertTrue(failureReported.await(2, TimeUnit.SECONDS));
        assertTrue(failure.get().getMessage().contains("while the benchmark was active"));
        assertThrows(IOException.class, sender::close);
    }

    @Test
    @SuppressWarnings("unchecked")
    void continuousFlowControlStallFailsTheActiveBenchmark() throws Exception {
        final ServiceGrpc.ServiceStub stub = mock(ServiceGrpc.ServiceStub.class);
        final ClientCallStreamObserver<MessageLatenciesRecord> requestStream =
                mock(ClientCallStreamObserver.class);
        when(requestStream.isReady()).thenReturn(false);
        when(stub.streamLatencies(any())).thenAnswer(invocation -> {
            final ClientResponseObserver<MessageLatenciesRecord, Empty> responseObserver =
                    invocation.getArgument(0);
            responseObserver.beforeStart(requestStream);
            return requestStream;
        });
        final AtomicReference<Throwable> failure = new AtomicReference<>();
        final CountDownLatch failureReported = new CountDownLatch(1);
        final GrpcStreamSender sender = new GrpcStreamSender(stub, 2, 5, 1, throwable -> {
            failure.set(throwable);
            failureReported.countDown();
        });

        sender.send(MessageLatenciesRecord.getDefaultInstance());

        assertTrue(failureReported.await(2, TimeUnit.SECONDS));
        assertTrue(failure.get().getMessage().contains("remained stalled for 1 second(s)"));
        assertThrows(IOException.class, sender::close);
    }

    @Test
    @SuppressWarnings("unchecked")
    void closeImmediatelySurfacesSenderThreadFailure() throws Exception {
        final ServiceGrpc.ServiceStub stub = mock(ServiceGrpc.ServiceStub.class);
        final ClientCallStreamObserver<MessageLatenciesRecord> requestStream =
                mock(ClientCallStreamObserver.class);
        final IllegalStateException transportFailure =
                new IllegalStateException("test request-stream failure");
        when(requestStream.isReady()).thenReturn(true);
        doThrow(transportFailure).when(requestStream).onNext(any());
        when(stub.streamLatencies(any())).thenAnswer(invocation -> {
            final ClientResponseObserver<MessageLatenciesRecord, Empty> responseObserver =
                    invocation.getArgument(0);
            responseObserver.beforeStart(requestStream);
            return requestStream;
        });
        final AtomicReference<Throwable> failure = new AtomicReference<>();
        final CountDownLatch failureReported = new CountDownLatch(1);
        final GrpcStreamSender sender = new GrpcStreamSender(stub, 1, 5, 30, throwable -> {
            failure.set(throwable);
            failureReported.countDown();
        });

        sender.send(MessageLatenciesRecord.getDefaultInstance());

        assertTrue(failureReported.await(2, TimeUnit.SECONDS));
        final IOException exception = assertThrows(IOException.class, sender::close);
        assertEquals(transportFailure, failure.get());
        assertEquals(transportFailure, exception.getCause());
    }
}
