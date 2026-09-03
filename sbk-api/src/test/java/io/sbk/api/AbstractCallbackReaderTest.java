/**
 * Copyright (c) KMG. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 */
package io.sbk.api;

import io.perl.api.PerlChannel;
import io.sbk.data.impl.ByteArray;
import io.sbk.logger.ReadRequestsLogger;
import io.time.MicroSeconds;
import io.time.MilliSeconds;
import io.time.NanoSeconds;
import io.time.Time;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;

/**
 * Verifies callback-reader count and duration completion semantics.
 */
final class AbstractCallbackReaderTest {

    @Test
    void completesAfterAllBatchedRecordsAreRead() throws Exception {
        CapturingChannel channel = new CapturingChannel();
        TestCallbackReader reader = new TestCallbackReader();
        reader.initialize(new TestWorker(channel), 0, 5,
                new ByteArray(), new NanoSeconds(), data -> { });
        CompletableFuture<Void> completion = waitForCompletion(reader);

        reader.recordBenchmark(1, 2, 20, 2);
        reader.recordBenchmark(2, 3, 20, 2);

        assertFalse(completion.isDone());
        assertEquals(4, channel.records.get());

        reader.recordBenchmark(3, 4, 10, 1);

        completion.get(1, TimeUnit.SECONDS);
        assertEquals(5, channel.records.get());
        assertEquals(50, channel.bytes.get());
    }

    @Test
    void checksDurationInTheConfiguredTimeUnit() throws Exception {
        verifyDuration(new MilliSeconds(), Time.MS_PER_SEC);
        verifyDuration(new MicroSeconds(), Time.MICROS_PER_SEC);
        verifyDuration(new NanoSeconds(), Time.NS_PER_SEC);
    }

    @Test
    void durationModeRecordsBatchesWithoutCountBasedCompletion() throws Exception {
        final CapturingChannel channel = new CapturingChannel();
        final TestCallbackReader reader = new TestCallbackReader();
        final Time time = new NanoSeconds();
        reader.initialize(new TestWorker(channel), 60, 0,
                new ByteArray(), time, data -> { });
        final long currentTime = time.getCurrentTime();

        reader.recordBenchmark(currentTime, currentTime + 1, 10, 7);

        assertEquals(7, channel.records.get());
    }

    @Test
    void rejectsReadRequestLoggingInsteadOfSilentlyIgnoringIt() {
        final TestCallbackReader callbackReader = new TestCallbackReader();
        final TestWorker worker = new TestWorker(new CapturingChannel());
        final ByteArray dataType = new ByteArray();
        final Time time = new NanoSeconds();
        final ReadRequestsLogger logger = mock(ReadRequestsLogger.class);

        assertUnsupported("read-request logging",
                () -> callbackReader.RecordsReader(worker, 1, dataType, time, logger));
        assertUnsupported("read-request logging",
                () -> callbackReader.RecordsReaderRW(worker, 1, dataType, time, logger));
        assertUnsupported("read-request logging",
                () -> callbackReader.RecordsTimeReader(worker, 1, dataType, time, logger));
        assertUnsupported("read-request logging",
                () -> callbackReader.RecordsTimeReaderRW(worker, 1, dataType, time, logger));
        assertEquals(0, callbackReader.startCalls.get());
        verifyNoInteractions(logger);
    }

    @Test
    void rejectsRateControlInsteadOfSilentlyIgnoringIt() {
        final TestCallbackReader callbackReader = new TestCallbackReader();
        final TestWorker worker = new TestWorker(new CapturingChannel());
        final ByteArray dataType = new ByteArray();
        final Time time = new NanoSeconds();
        final RateController rateController = mock(RateController.class);

        assertUnsupported("rate control",
                () -> callbackReader.RecordsReaderRateControl(worker, 1, dataType, time, rateController));
        assertUnsupported("rate control",
                () -> callbackReader.RecordsReaderRWRateControl(worker, 1, dataType, time, rateController));
        assertUnsupported("rate control",
                () -> callbackReader.RecordsTimeReaderRateControl(worker, 1, dataType, time, rateController));
        assertUnsupported("rate control",
                () -> callbackReader.RecordsTimeReaderRWRateControl(worker, 1, dataType, time, rateController));
        assertEquals(0, callbackReader.startCalls.get());
        verifyNoInteractions(rateController);
    }

    @Test
    void rejectsCombinedRateControlAndRequestLogging() {
        final TestCallbackReader callbackReader = new TestCallbackReader();
        final TestWorker worker = new TestWorker(new CapturingChannel());
        final ByteArray dataType = new ByteArray();
        final Time time = new NanoSeconds();
        final RateController rateController = mock(RateController.class);
        final ReadRequestsLogger logger = mock(ReadRequestsLogger.class);

        assertUnsupported("rate control", () -> callbackReader.RecordsReaderRateControl(
                worker, 1, dataType, time, rateController, logger));
        assertUnsupported("rate control", () -> callbackReader.RecordsReaderRWRateControl(
                worker, 1, dataType, time, rateController, logger));
        assertUnsupported("rate control", () -> callbackReader.RecordsTimeReaderRateControl(
                worker, 1, dataType, time, rateController, logger));
        assertUnsupported("rate control", () -> callbackReader.RecordsTimeReaderRWRateControl(
                worker, 1, dataType, time, rateController, logger));
        assertEquals(0, callbackReader.startCalls.get());
        verifyNoInteractions(rateController, logger);
    }

    private static void assertUnsupported(String control, ThrowingOperation operation) {
        final IOException exception = assertThrows(IOException.class, operation::run);
        assertEquals("Callback readers do not support " + control, exception.getMessage());
    }

    private static void verifyDuration(Time time, long unitsPerSecond)
            throws Exception {
        CapturingChannel channel = new CapturingChannel();
        TestCallbackReader reader = new TestCallbackReader();
        reader.initialize(new TestWorker(channel), 1, 0,
                new ByteArray(), time, data -> { });
        CompletableFuture<Void> completion = waitForCompletion(reader);
        long currentTime = time.getCurrentTime();

        reader.recordBenchmark(currentTime,
                currentTime + unitsPerSecond / 100, 1, 1);

        assertFalse(completion.isDone());

        reader.recordBenchmark(currentTime,
                currentTime + unitsPerSecond, 1, 1);

        completion.get(1, TimeUnit.SECONDS);
    }

    private static CompletableFuture<Void> waitForCompletion(
            TestCallbackReader reader) {
        return CompletableFuture.runAsync(() -> {
            try {
                reader.waitToComplete();
            } catch (IOException exception) {
                throw new IllegalStateException(exception);
            }
        });
    }

    @FunctionalInterface
    private interface ThrowingOperation {
        void run() throws IOException;
    }

    private static final class TestCallbackReader
            extends AbstractCallbackReader<byte[]> {
        private final AtomicInteger startCalls = new AtomicInteger();

        /**
         * Accepts the callback without starting an external consumer.
         *
         * @param callback callback supplied by the abstract reader
         */
        @Override
        public void start(Callback<byte[]> callback) {
            startCalls.incrementAndGet();
        }

        /**
         * Stops the test reader; no external resource requires cleanup.
         */
        @Override
        public void stop() {
        }
    }

    private static final class TestWorker extends Worker {
        private TestWorker(PerlChannel channel) {
            super(0, null, channel);
        }
    }

    private static final class CapturingChannel implements PerlChannel {
        private final AtomicInteger records = new AtomicInteger();
        private final AtomicInteger bytes = new AtomicInteger();

        /**
         * Accumulates records and bytes sent by the callback reader.
         *
         * @param startTime operation start time
         * @param endTime operation completion time
         * @param events completed record count
         * @param dataSize completed byte count
         */
        @Override
        public void send(long startTime, long endTime, int events,
                         int dataSize) {
            records.addAndGet(events);
            bytes.addAndGet(dataSize);
        }

        /**
         * Converts an unexpected channel failure into a test failure.
         *
         * @param ex unexpected channel failure
         */
        @Override
        public void throwException(Throwable ex) {
            throw new IllegalStateException(ex);
        }
    }
}
