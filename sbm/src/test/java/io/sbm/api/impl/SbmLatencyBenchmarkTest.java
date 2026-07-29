/**
 * Copyright (c) KMG. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 */
package io.sbm.api.impl;

import io.sbm.api.SbmPeriodicRecorder;
import io.sbp.grpc.MessageLatenciesRecord;
import io.time.MilliSeconds;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ForkJoinWorkerThread;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTimeoutPreemptively;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests the dedicated SBM latency-consumer execution lifecycle.
 */
final class SbmLatencyBenchmarkTest {

    /**
     * Verifies that the queue consumer runs on its named platform thread
     * instead of the common fork-join pool and exits during shutdown.
     *
     * @throws Exception if startup or completion exceeds its timeout
     */
    @Test
    void usesDedicatedPlatformThreadAndStopsIt() throws Exception {
        final CapturingWindow window = new CapturingWindow();
        final SbmLatencyBenchmark benchmark = new SbmLatencyBenchmark(
                1, 1, new MilliSeconds(), window, 5_000);

        final CompletableFuture<Void> completion = benchmark.start();
        Thread consumer = null;

        try {
            assertTrue(window.started.await(2, TimeUnit.SECONDS));
            consumer = window.consumer.get();
            assertNotNull(consumer);
            assertEquals(SbmLatencyBenchmark.CONSUMER_THREAD_NAME,
                    consumer.getName());
            assertFalse(consumer.isVirtual());
            assertFalse(consumer instanceof ForkJoinWorkerThread);
        } finally {
            assertTimeoutPreemptively(Duration.ofSeconds(2), benchmark::stop);
        }
        completion.get(2, TimeUnit.SECONDS);
        awaitThreadExit(consumer);
        assertFalse(consumer.isAlive());
    }

    private void awaitThreadExit(Thread thread) throws InterruptedException {
        final long deadline = System.nanoTime()
                + TimeUnit.SECONDS.toNanos(2);
        while (thread.isAlive() && System.nanoTime() < deadline) {
            Thread.sleep(10);
        }
    }

    private static final class CapturingWindow
            implements SbmPeriodicRecorder {
        private final CountDownLatch started = new CountDownLatch(1);
        private final AtomicReference<Thread> consumer =
                new AtomicReference<>();

        /**
         * Accepts a latency record for the current reporting window.
         *
         * @param currentTime current time supplied by the consumer
         * @param record      latency record being aggregated
         */
        @Override
        public void record(long currentTime, MessageLatenciesRecord record) {
        }

        /**
         * Starts a reporting window.
         *
         * @param startTime reporting-window start time
         */
        @Override
        public void startWindow(long startTime) {
        }

        /**
         * Returns the elapsed duration of the current reporting window.
         *
         * @param currentTime current time supplied by the consumer
         * @return elapsed reporting-window duration in milliseconds
         */
        @Override
        public long elapsedMilliSecondsWindow(long currentTime) {
            return 0;
        }

        /**
         * Stops the current reporting window.
         *
         * @param stopTime reporting-window stop time
         */
        @Override
        public void stopWindow(long stopTime) {
        }

        /**
         * Records the consumer thread when aggregation starts.
         *
         * @param startTime benchmark start time
         */
        @Override
        public void start(long startTime) {
            consumer.set(Thread.currentThread());
            started.countDown();
        }

        /**
         * Stops benchmark-wide aggregation.
         *
         * @param endTime benchmark end time
         */
        @Override
        public void stop(long endTime) {
        }
    }
}
