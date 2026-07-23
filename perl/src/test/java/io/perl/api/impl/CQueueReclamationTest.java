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

import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTimeoutPreemptively;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Exercises batched node retirement while a producer holds a stale cursor.
 */
public class CQueueReclamationTest {

    private static final int STRESS_RECORDS = 250_000;

    @Test
    public void pausedProducerRecoversAfterConsumerDrains() {
        assertTimeoutPreemptively(Duration.ofSeconds(30), () -> {
            final CQueue<Integer> queue = new CQueue<>();
            final CountDownLatch producerPaused = new CountDownLatch(1);
            final CountDownLatch resumeProducer = new CountDownLatch(1);
            final ExecutorService executor = Executors.newSingleThreadExecutor();

            try {
                final Future<Boolean> pausedAdd = executor.submit(() ->
                        queue.add(-1, () -> {
                            producerPaused.countDown();
                            await(resumeProducer);
                        }));
                producerPaused.await();

                for (int record = 0; record < STRESS_RECORDS; record++) {
                    assertTrue(queue.add(record));
                    assertEquals(record, queue.poll());
                    assertTrue(queue.retainedRetiredNodeCount()
                                    < CQueue.RETIRE_BATCH_SIZE,
                            "Retired-node chain exceeded the batch bound");
                }

                resumeProducer.countDown();
                assertTrue(pausedAdd.get());
                assertEquals(-1, queue.poll());
                assertNull(queue.poll());
            } finally {
                resumeProducer.countDown();
                executor.shutdownNow();
            }
        });
    }

    @Test
    public void clearFlushesPartialRetirementBatch() {
        final CQueue<Integer> queue = new CQueue<>();
        for (int record = 0; record < CQueue.RETIRE_BATCH_SIZE - 1; record++) {
            queue.add(record);
            assertEquals(record, queue.poll());
        }

        assertEquals(CQueue.RETIRE_BATCH_SIZE - 1,
                queue.retainedRetiredNodeCount());
        queue.clear();
        assertEquals(0, queue.retainedRetiredNodeCount());
        assertNull(queue.poll());
    }

    private static void await(final CountDownLatch latch) {
        try {
            latch.await();
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Paused producer was interrupted", exception);
        }
    }
}
