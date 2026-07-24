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

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

import java.lang.ref.ReferenceQueue;
import java.lang.ref.WeakReference;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Verifies intrusive timestamp queue reclamation in a constrained child JVM.
 */
public final class TimeStampMpscQueueGcSoak {
    private static final int SOAK_RECORDS = 20_000_000;
    private static final int GC_ATTEMPTS = 10;

    private TimeStampMpscQueueGcSoak() {
    }

    /**
     * Runs the constrained-heap stale-producer and node-reclamation checks.
     *
     * @param args unused command-line arguments
     * @throws Exception if producer coordination fails
     */
    public static void main(String[] args) throws Exception {
        verifyWeakReferenceProbe();
        final TimeStampMpscQueue queue = new TimeStampMpscQueue();
        final CountDownLatch producerPaused = new CountDownLatch(1);
        final CountDownLatch resumeProducer = new CountDownLatch(1);
        final AtomicReference<Throwable> producerFailure =
                new AtomicReference<>();
        final TimeStampNode pausedNode = node(-1);
        final Thread producer = Thread.ofPlatform()
                .name("timestamp-mpsc-stale-producer")
                .unstarted(() -> {
                    try {
                        queue.add(pausedNode, () -> {
                            producerPaused.countDown();
                            await(resumeProducer);
                        });
                    } catch (Throwable throwable) {
                        producerFailure.set(throwable);
                    }
                });

        producer.start();
        require(producerPaused.await(10, TimeUnit.SECONDS),
                "Producer did not reach the stale-tail checkpoint");

        for (int record = 0; record < SOAK_RECORDS; record++) {
            final TimeStampNode current = node(record);
            require(queue.add(current), "Queue rejected a soak node");
            require(queue.poll() == current,
                    "Queue returned an unexpected soak node");
            if ((record & 1023) == 0) {
                require(queue.retainedRetiredNodeCount()
                                < TimeStampMpscQueue.RETIRE_BATCH_SIZE,
                        "Retired-node chain exceeded the batch bound");
            }
        }

        resumeProducer.countDown();
        producer.join(TimeUnit.SECONDS.toMillis(10));
        require(!producer.isAlive(), "Stale producer did not recover");
        require(producerFailure.get() == null,
                "Stale producer failed: " + producerFailure.get());
        require(queue.poll() == pausedNode,
                "Recovered producer node was not observable");
        require(queue.poll() == null,
                "Queue was not empty after the soak");

        final ReferenceQueue<TimeStampNode> collectedNodes =
                new ReferenceQueue<>();
        final WeakReference<TimeStampNode> consumed =
                consumeNodeOnCompletedThread(queue, collectedNodes);
        awaitCollection(consumed, collectedNodes);
        System.out.printf(
                "TimeStamp MPSC GC verified with %,d records in a %,d MB heap; "
                        + "retired nodes remained bounded and consumed nodes "
                        + "were reclaimed%n",
                SOAK_RECORDS,
                Runtime.getRuntime().maxMemory() / (1024 * 1024));
    }

    private static void verifyWeakReferenceProbe()
            throws InterruptedException {
        final ReferenceQueue<TimeStampNode> collectedNodes =
                new ReferenceQueue<>();
        final AtomicReference<WeakReference<TimeStampNode>> result =
                new AtomicReference<>();
        final Thread owner = Thread.ofPlatform()
                .unstarted(() -> {
                    TimeStampNode value = node(Integer.MIN_VALUE);
                    result.set(new WeakReference<>(value, collectedNodes));
                    value = null;
                });
        owner.start();
        owner.join();
        awaitCollection(result.get(), collectedNodes);
    }

    private static WeakReference<TimeStampNode> consumeNodeOnCompletedThread(
            TimeStampMpscQueue queue,
            ReferenceQueue<TimeStampNode> collectedNodes)
            throws InterruptedException {
        final AtomicReference<WeakReference<TimeStampNode>> result =
                new AtomicReference<>();
        final Thread consumer = Thread.ofPlatform()
                .name("timestamp-mpsc-reclamation-probe")
                .unstarted(() -> result.set(
                        consumeNode(queue, collectedNodes)));
        consumer.start();
        consumer.join(TimeUnit.SECONDS.toMillis(10));
        require(!consumer.isAlive(),
                "Reclamation probe thread did not complete");
        require(result.get() != null,
                "Reclamation probe did not return a weak reference");
        return result.get();
    }

    private static WeakReference<TimeStampNode> consumeNode(
            TimeStampMpscQueue queue,
            ReferenceQueue<TimeStampNode> collectedNodes) {
        TimeStampNode value = node(Integer.MAX_VALUE);
        final WeakReference<TimeStampNode> reference =
                new WeakReference<>(value, collectedNodes);
        require(queue.add(value), "Queue rejected the weakly referenced node");
        require(queue.poll() == value, "Queue returned an unexpected node");

        for (int index = 0;
             index < TimeStampMpscQueue.RETIRE_BATCH_SIZE * 2;
             index++) {
            final TimeStampNode replacement = node(index);
            require(queue.add(replacement), "Queue rejected a replacement node");
            require(queue.poll() == replacement,
                    "Queue returned an unexpected replacement node");
        }
        queue.clear();
        value = null;
        return reference;
    }

    @SuppressFBWarnings(value = "DM_GC",
            justification = "The isolated 32 MB test JVM requires a "
                    + "deterministic full-GC reclamation assertion")
    private static void awaitCollection(
            WeakReference<TimeStampNode> reference,
            ReferenceQueue<TimeStampNode> collectedNodes)
            throws InterruptedException {
        final byte[][] pressure = new byte[8][];
        boolean collected = false;
        for (int attempt = 0; attempt < GC_ATTEMPTS && !collected; attempt++) {
            pressure[attempt % pressure.length] = new byte[1024 * 1024];
            System.gc();
            collected = collectedNodes.remove(1000) == reference;
        }
        require(collected,
                "Consumed intrusive node remained reachable");
    }

    private static TimeStampNode node(int value) {
        return new TimeStampNode(value, value + 1L, 1, 100);
    }

    private static void await(CountDownLatch latch) {
        try {
            latch.await();
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException(
                    "Stale producer was interrupted", exception);
        }
    }

    private static void require(boolean condition, String failureMessage) {
        if (!condition) {
            throw new AssertionError(failureMessage);
        }
    }
}
