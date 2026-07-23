/**
 * Copyright (c) KMG. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 */
package io.perl.api.impl;

import java.lang.ref.WeakReference;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Runs deterministic CQueue reclamation checks in a constrained child JVM.
 *
 * <p>The Gradle {@code cqueueGcTest} task starts this class with a 32 MB heap.
 * The test creates substantially more node garbage than that heap can hold,
 * while a producer retains a stale traversal cursor. Completion without an
 * out-of-memory error, together with the explicit retired-chain bound, proves
 * that consumed nodes do not accumulate behind that producer. A weak-reference
 * check separately verifies that a consumed node releases its payload.</p>
 */
public final class CQueueGcSoak {

    private static final int SOAK_RECORDS = 20_000_000;
    private static final int PAYLOAD_BYTES = 4 * 1024 * 1024;
    private static final int GC_ATTEMPTS = 100;

    private CQueueGcSoak() {
    }

    /**
     * Executes the constrained-heap reclamation verification.
     *
     * @param args unused command-line arguments
     * @throws Exception if a producer cannot be coordinated or recovered
     */
    public static void main(final String[] args) throws Exception {
        final CQueue<Object> queue = new CQueue<>();
        final CountDownLatch producerPaused = new CountDownLatch(1);
        final CountDownLatch resumeProducer = new CountDownLatch(1);
        final AtomicReference<Throwable> producerFailure = new AtomicReference<>();
        final Thread producer = Thread.ofPlatform()
                .name("cqueue-stale-producer")
                .unstarted(() -> {
                    try {
                        queue.add(-1, () -> {
                            producerPaused.countDown();
                            await(resumeProducer);
                        });
                    } catch (Throwable throwable) {
                        producerFailure.set(throwable);
                    }
                });

        producer.start();
        require(producerPaused.await(10, TimeUnit.SECONDS),
                "Producer did not reach the stale-cursor checkpoint");

        for (int record = 0; record < SOAK_RECORDS; record++) {
            require(queue.add(record), "Queue rejected a soak record");
            require(Integer.valueOf(record).equals(queue.poll()),
                    "Queue returned an unexpected soak record");
            if ((record & 1023) == 0) {
                require(queue.retainedRetiredNodeCount()
                                < CQueue.RETIRE_BATCH_SIZE,
                        "Retired-node chain exceeded the batch bound");
            }
        }

        resumeProducer.countDown();
        producer.join(TimeUnit.SECONDS.toMillis(10));
        require(!producer.isAlive(), "Stale producer did not recover");
        require(producerFailure.get() == null,
                "Stale producer failed: " + producerFailure.get());
        require(Integer.valueOf(-1).equals(queue.poll()),
                "Recovered producer record was not observable");
        require(queue.poll() == null, "Queue was not empty after the soak");

        final WeakReference<byte[]> payload = consumePayload(queue);
        awaitPayloadCollection(payload);
        System.out.printf(
                "CQueue GC verified with %,d records in a %,d MB heap; "
                        + "retired nodes remained bounded and payload was reclaimed%n",
                SOAK_RECORDS, Runtime.getRuntime().maxMemory() / (1024 * 1024));
    }

    private static WeakReference<byte[]> consumePayload(
            final CQueue<Object> queue) {
        final byte[] value = new byte[PAYLOAD_BYTES];
        final WeakReference<byte[]> reference = new WeakReference<>(value);
        require(queue.add(value), "Queue rejected the payload");
        require(queue.poll() == value, "Queue returned an unexpected payload");
        require(queue.poll() == null, "Queue retained an unexpected record");
        return reference;
    }

    private static void awaitPayloadCollection(
            final WeakReference<byte[]> payload) {
        final byte[][] pressure = new byte[16][];
        for (int attempt = 0;
             attempt < GC_ATTEMPTS && payload.get() != null;
             attempt++) {
            pressure[attempt % pressure.length] = new byte[1024 * 1024];
        }
        require(payload.get() == null,
                "Consumed queue node retained its payload");
    }

    private static void await(final CountDownLatch latch) {
        try {
            latch.await();
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException(
                    "Stale producer was interrupted", exception);
        }
    }

    private static void require(
            final boolean condition, final String failureMessage) {
        if (!condition) {
            throw new AssertionError(failureMessage);
        }
    }
}
