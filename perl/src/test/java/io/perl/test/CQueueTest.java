/**
 * Copyright (c) KMG. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 */
package io.perl.test;

import io.perl.api.impl.CQueue;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.Arrays;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.LockSupport;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTimeoutPreemptively;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Correctness and MPSC stress tests for {@link CQueue}.
 */
public class CQueueTest {

    private static final int PRODUCERS = 4;
    private static final int RECORDS_PER_PRODUCER = 25_000;

    @Test
    public void addsAndPollsInFifoOrder() {
        final CQueue<Integer> queue = new CQueue<>();

        assertTrue(queue.add(10));
        assertTrue(queue.add(20));
        assertTrue(queue.add(30));
        assertEquals(10, queue.poll());
        assertEquals(20, queue.poll());
        assertEquals(30, queue.poll());
        assertNull(queue.poll());
    }

    @Test
    public void rejectsNullElements() {
        final CQueue<Integer> queue = new CQueue<>();

        assertThrows(NullPointerException.class, () -> queue.add(null));
    }

    @Test
    public void clearDrainsAndKeepsQueueReusable() {
        final CQueue<Integer> queue = new CQueue<>();
        queue.add(1);
        queue.add(2);
        assertEquals(1, queue.poll());

        queue.clear();
        assertNull(queue.poll());
        assertTrue(queue.add(3));
        assertEquals(3, queue.poll());
        assertNull(queue.poll());
    }

    @Test
    public void multipleProducersDeliverEveryRecordInProducerOrder() {
        assertTimeoutPreemptively(Duration.ofSeconds(30), () -> {
            final CQueue<Integer> queue = new CQueue<>();
            final CountDownLatch ready = new CountDownLatch(PRODUCERS);
            final CountDownLatch start = new CountDownLatch(1);
            final CountDownLatch completed = new CountDownLatch(PRODUCERS);
            final AtomicReference<Throwable> producerFailure = new AtomicReference<>();
            final ExecutorService executor = Executors.newFixedThreadPool(PRODUCERS);

            try {
                for (int producer = 0; producer < PRODUCERS; producer++) {
                    final int producerId = producer;
                    executor.execute(() -> produce(queue, ready, start, completed,
                            producerFailure, producerId));
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

    private static void produce(final CQueue<Integer> queue, final CountDownLatch ready,
                                final CountDownLatch start, final CountDownLatch completed,
                                final AtomicReference<Throwable> producerFailure,
                                final int producerId) {
        ready.countDown();
        try {
            start.await();
            final int base = producerId * RECORDS_PER_PRODUCER;
            for (int sequence = 0; sequence < RECORDS_PER_PRODUCER; sequence++) {
                queue.add(base + sequence);
            }
        } catch (Throwable throwable) {
            producerFailure.compareAndSet(null, throwable);
        } finally {
            completed.countDown();
        }
    }

    private static void consumeAndVerify(final CQueue<Integer> queue,
                                         final CountDownLatch completed,
                                         final AtomicReference<Throwable> producerFailure) {
        final int totalRecords = PRODUCERS * RECORDS_PER_PRODUCER;
        final boolean[] seen = new boolean[totalRecords];
        final int[] lastSequence = new int[PRODUCERS];
        Arrays.fill(lastSequence, -1);

        int consumed = 0;
        while (consumed < totalRecords) {
            Integer value = queue.poll();
            if (value == null) {
                assertNull(producerFailure.get(), "A producer failed");
                if (completed.getCount() == 0) {
                    value = queue.poll();
                    assertFalse(value == null,
                            "Producers completed before every record was consumed");
                }
            }
            if (value == null) {
                LockSupport.parkNanos(1_000L);
                continue;
            }

            assertTrue(value >= 0 && value < totalRecords, "Record is outside the expected range");
            assertFalse(seen[value], "Duplicate record " + value);
            seen[value] = true;
            final int producerId = value / RECORDS_PER_PRODUCER;
            final int sequence = value % RECORDS_PER_PRODUCER;
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
}
