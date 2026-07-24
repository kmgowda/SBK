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

import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Level;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.TearDown;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Soaks the retired-node path while a producer holds a stale queue cursor.
 */
@BenchmarkMode(Mode.Throughput)
@OutputTimeUnit(TimeUnit.SECONDS)
public class CQueueRetentionBenchmark {

    private static final Integer VALUE = 1;

    /**
     * Queue and paused-producer state shared by one benchmark thread.
     */
    @State(Scope.Thread)
    public static class RetentionState {
        private final CQueue<Integer> queue = new CQueue<>();
        private final CountDownLatch producerPaused = new CountDownLatch(1);
        private final CountDownLatch resumeProducer = new CountDownLatch(1);
        private final AtomicReference<Throwable> failure = new AtomicReference<>();
        private Thread producer;

        /**
         * Starts a producer and pauses it after it captures the initial tail.
         *
         * @throws InterruptedException if setup is interrupted
         * @throws IllegalStateException if the producer cannot be paused
         */
        @Setup(Level.Trial)
        public void setUp() throws InterruptedException {
            producer = new Thread(() -> {
                try {
                    queue.add(VALUE, () -> {
                        producerPaused.countDown();
                        await(resumeProducer);
                    });
                } catch (Throwable throwable) {
                    failure.set(throwable);
                }
            }, "cqueue-paused-producer");
            producer.start();
            if (!producerPaused.await(10, TimeUnit.SECONDS)) {
                throw new IllegalStateException("Producer did not reach the paused-tail state");
            }
        }

        /**
         * Releases the producer and verifies that it recovered from the retired
         * cursor.
         *
         * @throws InterruptedException if teardown is interrupted
         * @throws IllegalStateException if the producer cannot recover or
         * publish its record
         */
        @TearDown(Level.Trial)
        public void tearDown() throws InterruptedException {
            resumeProducer.countDown();
            producer.join(TimeUnit.SECONDS.toMillis(10));
            if (producer.isAlive()) {
                producer.interrupt();
                throw new IllegalStateException("Producer did not recover from its stale cursor");
            }
            if (failure.get() != null) {
                throw new IllegalStateException("Paused producer failed", failure.get());
            }
            if (queue.poll() == null) {
                throw new IllegalStateException("Recovered producer did not publish its record");
            }
            queue.clear();
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

    /**
     * Performs allocation and reclamation while checking that the retained
     * retired-node chain remains smaller than one retirement batch.
     *
     * @param state paused-producer queue state
     * @return current number of retained retired nodes
     * @throws IllegalStateException if publication, consumption, or the
     * retained-node bound fails
     */
    @Benchmark
    public int boundedRetiredHeap(final RetentionState state) {
        state.queue.add(VALUE);
        if (state.queue.poll() == null) {
            throw new IllegalStateException("Consumer failed to read the published record");
        }
        final int retainedNodes = state.queue.retainedRetiredNodeCount();
        if (retainedNodes >= CQueue.RETIRE_BATCH_SIZE) {
            throw new IllegalStateException(
                    "Retired-node chain exceeded the configured batch");
        }
        return retainedNodes;
    }
}
