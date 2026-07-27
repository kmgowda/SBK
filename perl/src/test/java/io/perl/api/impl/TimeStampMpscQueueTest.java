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

import io.perl.api.PerlChannel;
import io.perl.api.TimeStamp;
import io.perl.api.TimeStampNode;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.LockSupport;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTimeoutPreemptively;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Correctness, identity, MPSC, and reclamation tests for
 * {@link TimeStampMpscQueue}.
 */
public class TimeStampMpscQueueTest {
    private static final int PRODUCERS = 4;
    private static final int RECORDS_PER_PRODUCER = 25_000;
    private static final int RECLAMATION_RECORDS = 250_000;

    /**
     * Verifies that timestamp specialization is restricted to the intrusive
     * queue node.
     */
    @Test
    public void timeStampPermitsOnlyTimeStampNode() {
        assertTrue(TimeStamp.class.isSealed());
        assertArrayEquals(
                new Class<?>[]{TimeStampNode.class},
                TimeStamp.class.getPermittedSubclasses());
    }

    /**
     * Verifies wrapper-free identity and FIFO delivery.
     */
    @Test
    public void returnsTheSameNodeInFifoOrderWithoutAWrapper() {
        final TimeStampMpscQueue queue = new TimeStampMpscQueue();
        final TimeStampNode first = node(0, 10);
        final TimeStampNode second = node(0, 20);

        assertTrue(queue.add(first));
        assertTrue(queue.add(second));
        assertSame(first, queue.poll(),
                "The queue must return the producer node rather than a wrapper");
        assertSame(second, queue.poll(),
                "The queue must preserve node identity and FIFO order");
        assertNull(queue.poll());
    }

    /**
     * Verifies that null nodes cannot enter the intrusive queue.
     */
    @Test
    public void rejectsNullNodes() {
        final TimeStampMpscQueue queue = new TimeStampMpscQueue();

        assertThrows(NullPointerException.class, () -> queue.add(null));
    }

    /**
     * Verifies that concurrency tests cannot inject an invalid batch size.
     */
    @Test
    public void rejectsNonPositiveRetirementBatchSize() {
        assertThrows(IllegalArgumentException.class,
                () -> new TimeStampMpscQueue(0));
    }

    /**
     * Verifies that the queue array rejects non-intrusive timestamps.
     */
    @Test
    public void queueArrayRejectsNonIntrusiveTimestamp() {
        final TimeStampMpscQueueArray queues =
                new TimeStampMpscQueueArray(1);

        assertThrows(IllegalArgumentException.class,
                () -> queues.add(0, new TimeStamp()));
    }

    /**
     * Verifies that the fallback channel retains the JDK queue data path.
     */
    @Test
    public void originalChannelRetainsJdkQueueAndTimestampPath() {
        final CQueuePerl.CQueueChannel channel =
                new CQueuePerl.CQueueChannel(1, throwable -> {
                    throw new AssertionError(throwable);
                });
        final PerlChannel producer = channel.getPerlChannel();

        producer.send(10, 20, 1, 100);

        final TimeStamp received = channel.receive(0);
        assertEquals(TimeStamp.class, received.getClass());
        assertEquals(10, received.startTime);
        assertEquals(20, received.endTime);
    }

    /**
     * Verifies that the optimized channel transports intrusive nodes.
     */
    @Test
    public void intrusiveChannelUsesTimestampNodePath() {
        final CQueuePerl.TimeStampMpscQueueChannel channel =
                new CQueuePerl.TimeStampMpscQueueChannel(1, throwable -> {
                    throw new AssertionError(throwable);
                });
        final PerlChannel producer = channel.getPerlChannel();

        producer.send(10, 20, 1, 100);

        final TimeStamp received = channel.receive(0);
        assertEquals(TimeStampNode.class, received.getClass());
        assertEquals(10, received.startTime);
        assertEquals(20, received.endTime);
    }

    /**
     * Verifies that clearing drains and leaves the queue reusable.
     */
    @Test
    public void clearDrainsAndKeepsQueueReusable() {
        final TimeStampMpscQueue queue = new TimeStampMpscQueue();
        queue.add(node(0, 1));
        queue.add(node(0, 2));
        assertEquals(1, queue.poll().records);

        queue.clear();
        assertEquals(0, queue.retainedRetiredNodeCount());
        assertNull(queue.poll());
        final TimeStampNode next = node(0, 3);
        assertTrue(queue.add(next));
        assertSame(next, queue.poll());
        assertNull(queue.poll());
    }

    /**
     * Verifies loss-free MPSC delivery and per-producer FIFO ordering.
     */
    @Test
    public void multipleProducersDeliverEveryRecordInProducerOrder() {
        assertTimeoutPreemptively(Duration.ofSeconds(30), () -> {
            final TimeStampMpscQueue queue = new TimeStampMpscQueue();
            final CountDownLatch ready = new CountDownLatch(PRODUCERS);
            final CountDownLatch start = new CountDownLatch(1);
            final CountDownLatch completed = new CountDownLatch(PRODUCERS);
            final AtomicReference<Throwable> producerFailure =
                    new AtomicReference<>();
            final ExecutorService executor =
                    Executors.newFixedThreadPool(PRODUCERS);

            try {
                for (int producer = 0; producer < PRODUCERS; producer++) {
                    final int producerId = producer;
                    executor.execute(() -> produce(queue, ready, start,
                            completed, producerFailure, producerId));
                }

                ready.await();
                start.countDown();
                consumeAndVerify(queue, completed, producerFailure);
                assertNull(producerFailure.get(), "A producer failed");
            } finally {
                start.countDown();
                executor.shutdownNow();
            }
        });
    }

    /**
     * Verifies recovery when a producer retains a retired tail reference.
     */
    @Test
    public void pausedProducerRecoversAfterConsumerRetiresItsStaleTail() {
        assertTimeoutPreemptively(Duration.ofSeconds(30), () -> {
            final TimeStampMpscQueue queue = new TimeStampMpscQueue();
            final CountDownLatch producerPaused = new CountDownLatch(1);
            final CountDownLatch resumeProducer = new CountDownLatch(1);
            final ExecutorService executor = Executors.newSingleThreadExecutor();

            try {
                final TimeStampNode pausedNode = node(-1, 1);
                final Future<Boolean> pausedAdd = executor.submit(() ->
                        queue.add(pausedNode, () -> {
                            producerPaused.countDown();
                            await(resumeProducer);
                        }));
                producerPaused.await();

                for (int record = 0;
                     record < RECLAMATION_RECORDS;
                     record++) {
                    final TimeStampNode current = node(0, record);
                    assertTrue(queue.add(current));
                    assertSame(current, queue.poll());
                    assertTrue(queue.retainedRetiredNodeCount()
                                    < TimeStampMpscQueue.RETIRE_BATCH_SIZE,
                            "Retired-node chain exceeded the batch bound");
                }

                resumeProducer.countDown();
                assertTrue(pausedAdd.get());
                assertSame(pausedNode, queue.poll());
                assertNull(queue.poll());
            } finally {
                resumeProducer.countDown();
                executor.shutdownNow();
            }
        });
    }

    /**
     * Verifies stale producers recover across several retirement generations.
     */
    @Test
    public void producersPausedAtDifferentRetirementGenerationsRecover() {
        assertTimeoutPreemptively(Duration.ofSeconds(30), () -> {
            final TimeStampMpscQueue queue = new TimeStampMpscQueue();
            final int pausedProducerCount = 3;
            final CountDownLatch[] producerPaused =
                    new CountDownLatch[pausedProducerCount];
            final CountDownLatch resumeProducers = new CountDownLatch(1);
            final ExecutorService executor =
                    Executors.newFixedThreadPool(pausedProducerCount);
            final TimeStampNode[] pausedNodes =
                    new TimeStampNode[pausedProducerCount];

            try {
                @SuppressWarnings("unchecked")
                final Future<Boolean>[] pausedAdds =
                        new Future[pausedProducerCount];
                for (int producer = 0;
                     producer < pausedProducerCount;
                     producer++) {
                    final int producerIndex = producer;
                    producerPaused[producer] = new CountDownLatch(1);
                    pausedNodes[producer] = node(-(producer + 1), producer);
                    pausedAdds[producer] = executor.submit(() ->
                            queue.add(pausedNodes[producerIndex], () -> {
                                producerPaused[producerIndex].countDown();
                                await(resumeProducers);
                            }));
                    producerPaused[producer].await();

                    for (int record = 0;
                         record < TimeStampMpscQueue.RETIRE_BATCH_SIZE;
                         record++) {
                        final TimeStampNode current =
                                node(producer, record);
                        assertTrue(queue.add(current));
                        assertSame(current, queue.poll());
                    }
                }

                for (int record = 0;
                     record < RECLAMATION_RECORDS;
                     record++) {
                    final TimeStampNode current = node(0, record);
                    assertTrue(queue.add(current));
                    assertSame(current, queue.poll());
                    assertTrue(queue.retainedRetiredNodeCount()
                                    < TimeStampMpscQueue.RETIRE_BATCH_SIZE,
                            "Retired-node chain exceeded the batch bound");
                }

                resumeProducers.countDown();
                for (Future<Boolean> pausedAdd : pausedAdds) {
                    assertTrue(pausedAdd.get());
                }

                final Set<TimeStampNode> recoveredNodes = new HashSet<>();
                for (int producer = 0;
                     producer < pausedProducerCount;
                     producer++) {
                    recoveredNodes.add(queue.poll());
                }
                assertEquals(Set.of(pausedNodes), recoveredNodes);
                assertNull(queue.poll());
            } finally {
                resumeProducers.countDown();
                executor.shutdownNow();
            }
        });
    }

    private static void produce(
            final TimeStampMpscQueue queue,
            final CountDownLatch ready,
            final CountDownLatch start,
            final CountDownLatch completed,
            final AtomicReference<Throwable> producerFailure,
            final int producerId) {
        ready.countDown();
        try {
            start.await();
            for (int sequence = 0;
                 sequence < RECORDS_PER_PRODUCER;
                 sequence++) {
                queue.add(node(producerId, sequence));
            }
        } catch (Throwable throwable) {
            producerFailure.compareAndSet(null, throwable);
        } finally {
            completed.countDown();
        }
    }

    private static void consumeAndVerify(
            final TimeStampMpscQueue queue,
            final CountDownLatch completed,
            final AtomicReference<Throwable> producerFailure) {
        final int totalRecords = PRODUCERS * RECORDS_PER_PRODUCER;
        final boolean[] seen = new boolean[totalRecords];
        final int[] lastSequence = new int[PRODUCERS];
        Arrays.fill(lastSequence, -1);

        int consumed = 0;
        while (consumed < totalRecords) {
            TimeStampNode value = queue.poll();
            if (value == null) {
                assertNull(producerFailure.get(), "A producer failed");
                if (completed.getCount() == 0) {
                    value = queue.poll();
                    assertFalse(value == null,
                            "Producers completed before every node was consumed");
                }
            }
            if (value == null) {
                LockSupport.parkNanos(1_000L);
                continue;
            }

            final int producerId = (int) value.startTime;
            final int sequence = value.records;
            final int index = producerId * RECORDS_PER_PRODUCER + sequence;
            assertTrue(index >= 0 && index < totalRecords,
                    "Record is outside the expected range");
            assertFalse(seen[index], "Duplicate record " + index);
            seen[index] = true;
            assertEquals(lastSequence[producerId] + 1, sequence,
                    "Records from producer " + producerId + " are out of order");
            lastSequence[producerId] = sequence;
            consumed++;
        }

        final boolean[] expected = new boolean[totalRecords];
        Arrays.fill(expected, true);
        assertArrayEquals(expected, seen);
        assertNull(queue.poll());
    }

    private static TimeStampNode node(int producer, int sequence) {
        return new TimeStampNode(producer, sequence, sequence, 100);
    }

    private static void await(CountDownLatch latch) {
        try {
            latch.await();
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException(
                    "Paused producer was interrupted", exception);
        }
    }
}
