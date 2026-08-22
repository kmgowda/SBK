/**
 * Copyright (c) KMG. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 */
package io.sbm.api.impl;

import io.perl.config.PerlConfig;
import io.perl.exception.BenchmarkIdleTimeoutException;
import io.sbm.api.SbmPeriodicRecorder;
import io.sbp.grpc.MessageLatenciesRecord;
import io.time.MilliSeconds;
import io.time.Time;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ForkJoinWorkerThread;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
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
                1, 1, new MilliSeconds(), window,
                PerlConfig.DEFAULT_PRINTING_INTERVAL_SECONDS * Time.MS_PER_SEC, 600);

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

    /**
     * Verifies that SBM does not print timer-created empty windows before a
     * remote SBK batch arrives, while still printing a received batch whose
     * counters may legitimately be zero.
     *
     * @throws Exception if startup, batch processing, or shutdown times out
     */
    @Test
    void printsOnlyWindowsContainingARemoteSbkBatch() throws Exception {
        final CapturingWindow window = new CapturingWindow(true);
        final SbmLatencyBenchmark benchmark = new SbmLatencyBenchmark(
                1, 1, new MilliSeconds(), window, 0, 600);
        final CompletableFuture<Void> completion = benchmark.start();

        try {
            assertTrue(window.emptyIntervalRotated.await(2, TimeUnit.SECONDS));
            assertEquals(0, window.stoppedWindows.get());

            benchmark.enQueue(MessageLatenciesRecord.newBuilder()
                    .setSequenceNumber(1)
                    .build());

            assertTrue(window.batchRecorded.await(2, TimeUnit.SECONDS));
            assertTrue(window.batchWindowPrinted.await(2, TimeUnit.SECONDS));
            assertEquals(1, window.stoppedWindows.get());
        } finally {
            assertTimeoutPreemptively(Duration.ofSeconds(2), benchmark::stop);
        }
        completion.get(2, TimeUnit.SECONDS);
    }

    /**
     * Verifies that an aggregation failure terminates the consumer with a
     * diagnostic future instead of allowing SBM to report a clean shutdown.
     *
     * @throws Exception if consumer startup or failure propagation times out
     */
    @Test
    void propagatesLatencyWindowFailures() throws Exception {
        final FailingWindow window = new FailingWindow();
        final SbmLatencyBenchmark benchmark = new SbmLatencyBenchmark(
                1, 1, new MilliSeconds(), window,
                PerlConfig.DEFAULT_PRINTING_INTERVAL_SECONDS * Time.MS_PER_SEC, 600);
        final CompletableFuture<Void> completion = benchmark.start();

        try {
            assertTrue(window.started.await(2, TimeUnit.SECONDS));
            benchmark.enQueue(MessageLatenciesRecord.newBuilder()
                    .setClientID(10)
                    .setSequenceNumber(1)
                    .build());

            final ExecutionException failure = assertThrows(ExecutionException.class,
                    () -> completion.get(2, TimeUnit.SECONDS));
            assertTrue(failure.getCause().getMessage().contains("client 10 at sequence 1"));
            assertEquals("test recorder failure", failure.getCause().getCause().getMessage());
        } finally {
            assertTimeoutPreemptively(Duration.ofSeconds(2), benchmark::stop);
        }
    }

    /**
     * Verifies that an empty SBM input queue terminates with the shared idle-timeout diagnostic.
     *
     * @throws Exception if consumer startup or failure propagation times out
     */
    @Test
    void failsWhenNoPerformanceBatchArrivesBeforeIdleTimeout() throws Exception {
        final CapturingWindow window = new CapturingWindow();
        final SbmLatencyBenchmark benchmark = new SbmLatencyBenchmark(
                1, 1, new MilliSeconds(), window, 5_000, 1, true);

        benchmark.enQueue(MessageLatenciesRecord.newBuilder()
                .setSequenceNumber(1)
                .setTotalRecords(0)
                .build());

        final ExecutionException failure = assertThrows(ExecutionException.class,
                () -> benchmark.start().get(4, TimeUnit.SECONDS));

        assertInstanceOf(BenchmarkIdleTimeoutException.class, failure.getCause());
        assertEquals("No performance benchmarking event was received for 1 seconds",
                failure.getCause().getMessage());
    }

    /**
     * Verifies that inactivity does not terminate SBM unless fixed-record mode was selected.
     *
     * @throws Exception if consumer startup or shutdown times out
     */
    @Test
    void doesNotApplyIdleTimeoutOutsideFixedRecordMode() throws Exception {
        final CapturingWindow window = new CapturingWindow();
        final SbmLatencyBenchmark benchmark = new SbmLatencyBenchmark(
                1, 1, new MilliSeconds(), window, 5_000, 1, false);
        final CompletableFuture<Void> completion = benchmark.start();

        try {
            assertTrue(window.started.await(2, TimeUnit.SECONDS));
            Thread.sleep(1_250);
            assertFalse(completion.isDone());
        } finally {
            assertTimeoutPreemptively(Duration.ofSeconds(2), benchmark::stop);
        }
        completion.get(2, TimeUnit.SECONDS);
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
        private final CountDownLatch emptyIntervalRotated =
                new CountDownLatch(1);
        private final CountDownLatch batchRecorded = new CountDownLatch(1);
        private final CountDownLatch batchWindowPrinted =
                new CountDownLatch(1);
        private final AtomicReference<Thread> consumer =
                new AtomicReference<>();
        private final AtomicInteger startedWindows = new AtomicInteger();
        private final AtomicInteger stoppedWindows = new AtomicInteger();
        private final boolean expireImmediately;

        private CapturingWindow() {
            this(false);
        }

        private CapturingWindow(boolean expireImmediately) {
            this.expireImmediately = expireImmediately;
        }

        /**
         * Accepts a latency record for the current reporting window.
         *
         * @param currentTime current time supplied by the consumer
         * @param record      latency record being aggregated
         */
        @Override
        public void record(long currentTime, MessageLatenciesRecord record) {
            batchRecorded.countDown();
        }

        /**
         * Starts a reporting window.
         *
         * @param startTime reporting-window start time
         */
        @Override
        public void startWindow(long startTime) {
            if (startedWindows.incrementAndGet() >= 2) {
                emptyIntervalRotated.countDown();
            }
        }

        /**
         * Returns the elapsed duration of the current reporting window.
         *
         * @param currentTime current time supplied by the consumer
         * @return elapsed reporting-window duration in milliseconds
         */
        @Override
        public long elapsedMilliSecondsWindow(long currentTime) {
            return expireImmediately ? 1 : 0;
        }

        /**
         * Stops the current reporting window.
         *
         * @param stopTime reporting-window stop time
         */
        @Override
        public void stopWindow(long stopTime) {
            stoppedWindows.incrementAndGet();
            batchWindowPrinted.countDown();
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

    private static final class FailingWindow implements SbmPeriodicRecorder {
        private final CountDownLatch started = new CountDownLatch(1);

        /**
         * Rejects every latency batch to exercise consumer failure propagation.
         *
         * @param currentTime current time supplied by the consumer
         * @param record latency record being aggregated
         * @throws IllegalArgumentException for every supplied record
         */
        @Override
        public void record(long currentTime, MessageLatenciesRecord record) {
            throw new IllegalArgumentException("test recorder failure");
        }

        /**
         * Starts the failing recorder's reporting window.
         *
         * @param startTime reporting-window start time
         */
        @Override
        public void startWindow(long startTime) {
        }

        /**
         * Returns the elapsed reporting-window duration.
         *
         * @param currentTime current time supplied by the consumer
         * @return zero because this test fails before window rotation
         */
        @Override
        public long elapsedMilliSecondsWindow(long currentTime) {
            return 0;
        }

        /**
         * Stops the failing recorder's reporting window.
         *
         * @param stopTime reporting-window stop time
         */
        @Override
        public void stopWindow(long stopTime) {
        }

        /**
         * Signals that benchmark-wide aggregation has started.
         *
         * @param startTime benchmark start time
         */
        @Override
        public void start(long startTime) {
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
