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

        assertTrue(window.started.await(2, TimeUnit.SECONDS));
        final Thread consumer = window.consumer.get();
        assertNotNull(consumer);
        assertEquals(SbmLatencyBenchmark.CONSUMER_THREAD_NAME,
                consumer.getName());
        assertFalse(consumer.isVirtual());
        assertFalse(consumer instanceof ForkJoinWorkerThread);

        assertTimeoutPreemptively(Duration.ofSeconds(2), benchmark::stop);
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

        @Override
        public void record(long currentTime, MessageLatenciesRecord record) {
        }

        @Override
        public void startWindow(long startTime) {
        }

        @Override
        public long elapsedMilliSecondsWindow(long currentTime) {
            return 0;
        }

        @Override
        public void stopWindow(long stopTime) {
        }

        @Override
        public void start(long startTime) {
            consumer.set(Thread.currentThread());
            started.countDown();
        }

        @Override
        public void stop(long endTime) {
        }
    }
}
