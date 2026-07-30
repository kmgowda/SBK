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
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.grpc.stub.ClientCallStreamObserver;
import io.grpc.stub.ClientResponseObserver;
import io.perl.exception.ExceptionHandler;
import io.sbp.grpc.MessageLatenciesRecord;
import io.sbp.grpc.ServiceGrpc;

import java.io.IOException;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.LockSupport;

/**
 * Sends immutable latency batches over one flow-controlled client stream.
 *
 * <p>A bounded queue isolates protobuf serialization and network progress from
 * the PerL consumer. The sender parks when HTTP/2 flow control reports that the
 * stream is not ready; it never spin-waits and never grows memory without a
 * configured bound.
 */
final class GrpcStreamSender implements AutoCloseable {
    private static final Object END = new Object();
    private static final long READY_PARK_NANOS = TimeUnit.MILLISECONDS.toNanos(1);
    private final ArrayBlockingQueue<Object> batches;
    private final CountDownLatch responseCompleted;
    private final ExceptionHandler exceptionHandler;
    private final Thread senderThread;
    private volatile ClientCallStreamObserver<MessageLatenciesRecord> requestStream;
    private volatile Throwable failure;
    private volatile boolean closed;

    /**
     * Opens one client-streaming RPC and starts its dedicated sender thread.
     *
     * @param stub asynchronous service stub
     * @param maximumPendingBatches maximum number of retained immutable batches
     * @param exceptionHandler benchmark shutdown callback
     */
    GrpcStreamSender(ServiceGrpc.ServiceStub stub, int maximumPendingBatches,
                     ExceptionHandler exceptionHandler) {
        this.batches = new ArrayBlockingQueue<>(maximumPendingBatches);
        this.responseCompleted = new CountDownLatch(1);
        this.exceptionHandler = exceptionHandler;
        this.closed = false;
        final ClientResponseObserver<MessageLatenciesRecord, Empty> responseObserver =
                new ClientResponseObserver<>() {
                    @Override
                    @SuppressFBWarnings(value = "EI_EXPOSE_REP2",
                            justification = "The gRPC request stream is the transport handle owned by this sender")
                    public void beforeStart(ClientCallStreamObserver<MessageLatenciesRecord> stream) {
                        requestStream = stream;
                        stream.setOnReadyHandler(() -> LockSupport.unpark(senderThread));
                    }

                    @Override
                    public void onNext(Empty value) {
                    }

                    @Override
                    public void onError(Throwable throwable) {
                        reportFailure(throwable);
                        responseCompleted.countDown();
                    }

                    @Override
                    public void onCompleted() {
                        responseCompleted.countDown();
                    }
                };
        this.senderThread = Thread.ofPlatform()
                .name("sbk-grpc-stream-sender")
                .daemon(true)
                .unstarted(this::sendLoop);
        stub.streamLatencies(responseObserver);
        senderThread.start();
    }

    /**
     * Offers a completed immutable protobuf batch without waiting.
     *
     * @param record latency batch
     * @throws IOException when the stream failed or its bounded queue is full
     */
    void send(MessageLatenciesRecord record) throws IOException {
        checkFailure();
        if (closed) {
            throw new IOException("SBK gRPC latency stream is already closed");
        }
        if (!batches.offer(record)) {
            final IOException exception = new IOException(
                    "SBK gRPC latency stream is overloaded: pending batch queue is full");
            reportFailure(exception);
            throw exception;
        }
    }

    private void sendLoop() {
        try {
            while (true) {
                final Object next = batches.take();
                if (next == END) {
                    requestStream.onCompleted();
                    return;
                }
                awaitReady();
                requestStream.onNext((MessageLatenciesRecord) next);
            }
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            reportFailure(exception);
        } catch (IOException exception) {
            reportFailure(exception);
        } catch (RuntimeException exception) {
            reportFailure(exception);
        }
    }

    private void awaitReady() throws InterruptedException, IOException {
        while (!requestStream.isReady()) {
            checkFailure();
            LockSupport.parkNanos(READY_PARK_NANOS);
            if (Thread.interrupted()) {
                throw new InterruptedException("SBK gRPC stream sender interrupted");
            }
        }
    }

    private void reportFailure(Throwable throwable) {
        if (failure == null) {
            failure = throwable;
            LockSupport.unpark(senderThread);
            if (exceptionHandler != null) {
                exceptionHandler.throwException(throwable);
            }
        }
    }

    private void checkFailure() throws IOException {
        final Throwable currentFailure = failure;
        if (currentFailure != null) {
            throw new IOException("SBK gRPC latency stream failed", currentFailure);
        }
    }

    /**
     * Drains queued batches, half-closes the request stream, and waits for the
     * server acknowledgement.
     *
     * @throws IOException when draining or server acknowledgement fails
     */
    @Override
    public void close() throws IOException {
        if (closed) {
            checkFailure();
            return;
        }
        closed = true;
        try {
            if (!batches.offer(END, 5, TimeUnit.SECONDS)) {
                throw new IOException("Timed out while scheduling the final gRPC stream drain");
            }
            senderThread.join(TimeUnit.SECONDS.toMillis(5));
            if (senderThread.isAlive()) {
                throw new IOException("Timed out while draining the gRPC latency stream");
            }
            if (!responseCompleted.await(5, TimeUnit.SECONDS)) {
                throw new IOException("Timed out waiting for the SBM gRPC stream acknowledgement");
            }
            checkFailure();
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IOException("Interrupted while closing the gRPC latency stream", exception);
        }
    }
}
