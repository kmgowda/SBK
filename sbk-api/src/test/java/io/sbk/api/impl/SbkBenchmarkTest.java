/**
 * Copyright (c) KMG. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 */
package io.sbk.api.impl;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.sbk.api.AsyncReader;
import io.sbk.api.DataWriter;
import io.sbk.api.Storage;
import io.sbk.data.DataType;
import io.sbk.exception.BenchmarkCleanupTimeoutException;
import io.sbk.logger.RWLogger;
import io.sbk.params.impl.SbkParameters;
import io.time.MilliSeconds;
import io.time.Time;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.lang.reflect.Field;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTimeout;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.CALLS_REAL_METHODS;
import static org.mockito.Mockito.when;

/**
 * Tests benchmark lifecycle calculations that must be independent of a storage driver.
 */
final class SbkBenchmarkTest {

    @Test
    void virtualModeUsesVirtualThreadsWithoutAPlatformThreadPool() throws Exception {
        final SbkParameters params = new SbkParameters("virtual-executor-test");
        params.parseArgs(new String[]{"-writers", "1", "-size", "1", "-records", "1", "-thread", "v"});
        final SbkBenchmark benchmark = new SbkBenchmark(params, mock(Storage.class), mock(DataType.class),
                mock(RWLogger.class, CALLS_REAL_METHODS), new MilliSeconds());
        final ExecutorService executor = workerExecutor(benchmark);
        try {
            assertTrue(executor.submit(() -> Thread.currentThread().isVirtual()).get());
        } finally {
            executor.shutdownNow();
        }
    }

    @Test
    void distributesRemainderAcrossWorkers() {
        long[] shares = IntStream.range(0, 3)
                .mapToLong(index -> SbkBenchmark.recordsForWorker(10, 3, index))
                .toArray();

        assertArrayEquals(new long[]{4, 3, 3}, shares);
        assertEquals(10, IntStream.range(0, 3)
                .mapToLong(index -> SbkBenchmark.recordsForWorker(10, 3, index))
                .sum());
    }

    @Test
    void assignsRecordsWhenThereAreMoreWorkersThanRecords() {
        long[] shares = IntStream.range(0, 5)
                .mapToLong(index -> SbkBenchmark.recordsForWorker(2, 5, index))
                .toArray();

        assertArrayEquals(new long[]{1, 1, 0, 0, 0}, shares);
    }

    @Test
    void rejectsInvalidWorkerIndex() {
        assertThrows(IllegalArgumentException.class,
                () -> SbkBenchmark.recordsForWorker(1, 1, 1));
    }

    @Test
    void completesOnlyAfterAllWriterAndReaderWorkersExit() {
        final CompletableFuture<Void> writers = new CompletableFuture<>();
        final CompletableFuture<Void> readers = new CompletableFuture<>();
        final CompletableFuture<Void> allWorkers = SbkBenchmark.allWorkers(writers, readers);

        writers.complete(null);
        assertFalse(allWorkers.isDone());

        readers.complete(null);
        assertTrue(allWorkers.isDone());
    }

    @Test
    @SuppressWarnings("unchecked")
    void closesAsyncReadersBeforeStoppingPerformanceRecorder() throws Exception {
        final AtomicBoolean totalPrinted = new AtomicBoolean();
        final SbkParameters params = new SbkParameters("async-reader-drain-test");
        params.parseArgs(new String[]{"-readers", "1", "-size", "1", "-records", "1",
                "-thread", "p"});
        final Storage<Object> storage = mock(Storage.class);
        final DataType<Object> dataType = mock(DataType.class);
        final RWLogger logger = mock(RWLogger.class, invocation -> {
            if ("printTotal".equals(invocation.getMethod().getName())) {
                totalPrinted.set(true);
            }
            return CALLS_REAL_METHODS.answer(invocation);
        });
        final CompletableFuture<Object> readCompletion = new CompletableFuture<>();
        final AsyncReader<Object> reader = new AsyncReader<>() {
            @Override
            @SuppressFBWarnings(value = "EI_EXPOSE_REP",
                    justification = "The shared future is the completion barrier under test")
            public CompletableFuture<Object> readAsync(int size) {
                return readCompletion;
            }

            @Override
            public void close() throws IOException {
                if (totalPrinted.get()) {
                    throw new IOException("performance recorder stopped before async reader close");
                }
                readCompletion.complete(new Object());
            }
        };
        when(storage.createReader(0, params)).thenReturn(reader);
        when(dataType.length(org.mockito.ArgumentMatchers.any())).thenReturn(1);
        final SbkBenchmark benchmark = new SbkBenchmark(params, storage, dataType,
                logger, new MilliSeconds());

        benchmark.start().get(5, TimeUnit.SECONDS);

        assertTrue(totalPrinted.get());
    }

    @Test
    @SuppressWarnings("unchecked")
    void publishesTotalAndWarnsWhenDriverCloseBlocksUntilHardStop() throws Exception {
        final AtomicBoolean totalPrinted = new AtomicBoolean();
        final CountDownLatch releaseClose = new CountDownLatch(1);
        final SbkParameters params = new SbkParameters("blocked-close-total-test");
        params.parseArgs(new String[]{"-writers", "1", "-size", "1", "-records", "1",
                "-thread", "p"});
        final Storage<Object> storage = mock(Storage.class);
        final DataType<Object> dataType = mock(DataType.class);
        final RWLogger logger = mock(RWLogger.class, invocation -> {
            if ("printTotal".equals(invocation.getMethod().getName())) {
                totalPrinted.set(true);
            }
            return CALLS_REAL_METHODS.answer(invocation);
        });
        when(storage.createWriter(0, params)).thenReturn(new TestDataWriter(() -> {
            while (releaseClose.getCount() > 0) {
                try {
                    releaseClose.await();
                } catch (InterruptedException ignored) {
                    // Model a driver close that cannot finish before the hard deadline.
                }
            }
        }));
        when(dataType.create(1)).thenReturn(new Object());
        final SbkBenchmark benchmark = new SbkBenchmark(params, storage, dataType,
                logger, new MilliSeconds());

        try {
            assertDoesNotThrow(() -> benchmark.start().get(6, TimeUnit.SECONDS));
            assertTrue(totalPrinted.get(),
                    "the final-result reserve must publish Total before hard-stop completion");
        } finally {
            releaseClose.countDown();
        }
    }

    @Test
    void requestsShutdownBeforePropagatingWorkerStartFailure() {
        final IOException failure = new IOException("worker start failed");
        final Throwable[] shutdownFailure = new Throwable[1];

        final CompletionException exception = assertThrows(CompletionException.class,
                () -> SbkBenchmark.startWorker(() -> {
                    throw failure;
                }, shutdown -> shutdownFailure[0] = shutdown));

        assertEquals(failure, shutdownFailure[0]);
        assertEquals(failure, exception.getCause());
    }

    @Test
    void closesDriverOnlyAfterInterruptedWorkersExit() throws Exception {
        final AtomicBoolean workerExited = new AtomicBoolean();
        final AtomicBoolean closedAfterWorkerExit = new AtomicBoolean();
        final CountDownLatch workerStarted = new CountDownLatch(1);
        final SbkBenchmark benchmark = benchmarkWithWriter(() ->
                closedAfterWorkerExit.set(workerExited.get()));
        final ExecutorService executor = workerExecutor(benchmark);
        setWorkerCompletion(benchmark, CompletableFuture.runAsync(() -> {
            workerStarted.countDown();
            try {
                Thread.sleep(TimeUnit.MINUTES.toMillis(1));
            } catch (InterruptedException ignored) {
                Thread.currentThread().interrupt();
            } finally {
                workerExited.set(true);
            }
        }, executor));
        assertTrue(workerStarted.await(1, TimeUnit.SECONDS));

        benchmark.stop();

        assertTrue(workerExited.get());
        assertTrue(closedAfterWorkerExit.get());
    }

    @Test
    void closesDriverToUnblockAnInterruptIgnoringWorkerWithinBound() throws Exception {
        final CountDownLatch releaseWorker = new CountDownLatch(1);
        final CountDownLatch workerExited = new CountDownLatch(1);
        final CountDownLatch workerStarted = new CountDownLatch(1);
        final SbkBenchmark benchmark = benchmarkWithWriter(releaseWorker::countDown);
        final ExecutorService executor = workerExecutor(benchmark);
        setWorkerCompletion(benchmark, CompletableFuture.runAsync(() -> {
            workerStarted.countDown();
            while (releaseWorker.getCount() > 0) {
                try {
                    releaseWorker.await();
                } catch (InterruptedException ignored) {
                    // Deliberately model an SDK operation that ignores interruption.
                }
            }
            workerExited.countDown();
        }, executor));
        assertTrue(workerStarted.await(1, TimeUnit.SECONDS));

        assertTimeout(Duration.ofSeconds(3), benchmark::stop);

        assertEquals(0, releaseWorker.getCount());
        assertTrue(workerExited.await(1, TimeUnit.SECONDS));
    }

    @Test
    void usesRemainingCleanupDeadlineAfterDriverClose() throws Exception {
        final CountDownLatch releaseWorker = new CountDownLatch(1);
        final CountDownLatch workerStarted = new CountDownLatch(1);
        final ScheduledExecutorService delayedRelease = Executors.newSingleThreadScheduledExecutor();
        final SbkBenchmark benchmark = benchmarkWithWriter(() -> delayedRelease.schedule(
                releaseWorker::countDown, 1250, TimeUnit.MILLISECONDS));
        final ExecutorService executor = workerExecutor(benchmark);
        setWorkerCompletion(benchmark, CompletableFuture.runAsync(() -> {
            workerStarted.countDown();
            while (releaseWorker.getCount() > 0) {
                try {
                    releaseWorker.await();
                } catch (InterruptedException ignored) {
                    // Model a driver operation that finishes after close, but before the hard deadline.
                }
            }
        }, executor));
        assertTrue(workerStarted.await(1, TimeUnit.SECONDS));

        try {
            assertTimeout(Duration.ofSeconds(4), benchmark::stop);
            assertDoesNotThrow(() -> completionFuture(benchmark).join());
        } finally {
            delayedRelease.shutdownNow();
        }
    }

    @Test
    void hardDeadlineReleasesBenchmarkWhenDriverCloseIsStuck() throws Exception {
        final CountDownLatch releaseClose = new CountDownLatch(1);
        final SbkBenchmark benchmark = benchmarkWithWriter(() -> {
            while (releaseClose.getCount() > 0) {
                try {
                    releaseClose.await();
                } catch (InterruptedException ignored) {
                    // Deliberately model a driver close that ignores interruption.
                }
            }
        });
        setWorkerCompletion(benchmark, new CompletableFuture<>());

        try {
            assertTimeout(Duration.ofSeconds(6), benchmark::stop);
            assertDoesNotThrow(() -> completionFuture(benchmark).join());
        } finally {
            releaseClose.countDown();
        }
    }

    @Test
    void forcedCleanupFailsCompletionAndPreservesTheInitiatingFailure() throws Exception {
        final IOException initiatingFailure = new IOException("remote operation failed");
        final SbkBenchmark benchmark = benchmarkWithWriter(() -> { });

        final CompletionException completionFailure = assertThrows(CompletionException.class,
                () -> benchmark.forceShutdownCompletion(initiatingFailure).join());

        final BenchmarkCleanupTimeoutException timeoutFailure = assertInstanceOf(
                BenchmarkCleanupTimeoutException.class, completionFailure.getCause());
        assertSame(initiatingFailure, timeoutFailure.getCause());
        assertTrue(timeoutFailure.getMessage().contains("cleanup exceeded 5 seconds"));
        assertTrue(timeoutFailure.getMessage().contains("after a benchmark failure"));
    }

    @Test
    void orderlyCleanupTimeoutCompletesWithWarning() throws Exception {
        final SbkBenchmark benchmark = benchmarkWithWriter(() -> { });

        assertDoesNotThrow(() -> benchmark.forceShutdownCompletion(null).join());
    }

    @Test
    void hardDeadlineFailsWhenFinalTotalRemainsPending() throws Exception {
        final SbkBenchmark benchmark = benchmarkWithWriter(() -> { });
        setPerlCompletion(benchmark, new CompletableFuture<>());

        final CompletionException completionFailure = assertThrows(CompletionException.class,
                () -> benchmark.forceShutdownCompletion(null).join());

        final BenchmarkCleanupTimeoutException timeoutFailure = assertInstanceOf(
                BenchmarkCleanupTimeoutException.class, completionFailure.getCause());
        assertTrue(timeoutFailure.getMessage().contains("before the final Total was published"));
        assertInstanceOf(IllegalStateException.class, timeoutFailure.getCause());
    }

    @SuppressWarnings("unchecked")
    private static SbkBenchmark benchmarkWithWriter(IoCloseAction closeAction) throws Exception {
        final SbkParameters params = new SbkParameters("shutdown-order-test");
        params.parseArgs(new String[]{"-writers", "1", "-size", "10", "-seconds", "60",
                "-thread", "p"});
        final Storage<Object> storage = mock(Storage.class);
        final DataType<Object> dataType = mock(DataType.class);
        final Time time = new MilliSeconds();
        final RWLogger logger = mock(RWLogger.class, CALLS_REAL_METHODS);
        final SbkBenchmark benchmark = new SbkBenchmark(params, storage, dataType, logger, time);
        final Field writersField = SbkBenchmark.class.getDeclaredField("writers");
        writersField.setAccessible(true);
        final List<DataWriter<Object>> writers = (List<DataWriter<Object>>) writersField.get(benchmark);
        writers.add(new TestDataWriter(closeAction));
        return benchmark;
    }

    private static ExecutorService workerExecutor(SbkBenchmark benchmark) throws Exception {
        final Field executorField = SbkBenchmark.class.getDeclaredField("executor");
        executorField.setAccessible(true);
        return (ExecutorService) executorField.get(benchmark);
    }

    private static void setWorkerCompletion(SbkBenchmark benchmark,
                                            CompletableFuture<Void> completion) throws Exception {
        final Field completionField = SbkBenchmark.class.getDeclaredField("workerCompletion");
        completionField.setAccessible(true);
        completionField.set(benchmark, completion);
    }

    private static void setPerlCompletion(SbkBenchmark benchmark,
                                          CompletableFuture<Void> completion) throws Exception {
        final Field completionField = SbkBenchmark.class.getDeclaredField("writePerlCompletion");
        completionField.setAccessible(true);
        completionField.set(benchmark, completion);
    }

    @SuppressWarnings("unchecked")
    private static CompletableFuture<Void> completionFuture(SbkBenchmark benchmark) throws Exception {
        final Field completionField = SbkBenchmark.class.getDeclaredField("retFuture");
        completionField.setAccessible(true);
        return (CompletableFuture<Void>) completionField.get(benchmark);
    }

    @FunctionalInterface
    private interface IoCloseAction {
        void close() throws IOException;
    }

    private static final class TestDataWriter implements io.sbk.api.Writer<Object> {
        private final IoCloseAction closeAction;

        private TestDataWriter(IoCloseAction closeAction) {
            this.closeAction = closeAction;
        }

        @Override
        public CompletableFuture<?> writeAsync(Object data) {
            return null;
        }

        @Override
        public void close() throws IOException {
            closeAction.close();
        }
    }
}
