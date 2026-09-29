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

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.perl.api.PerlChannel;
import io.sbk.data.DataType;
import io.sbk.data.impl.ByteArray;
import io.sbk.logger.ReadRequestsLogger;
import io.time.NanoSeconds;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import java.io.IOException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeoutException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/** Tests completion semantics for every async read adapter. */
final class AsyncReaderCompletionTest {
    enum Mode {
        READ, READ_LOGGED, TIMESTAMP, TIMESTAMP_LOGGED
    }

    @ParameterizedTest
    @EnumSource(Mode.class)
    @SuppressWarnings("unchecked")
    void emptyCompletionDoesNotReportARecordOrFail(Mode mode) throws Exception {
        for (boolean immediate : new boolean[]{false, true}) {
            final CompletableFuture<byte[]> future = new CompletableFuture<>();
            final Sink sink = new Sink();
            final Status status = new Status();
            if (immediate) {
                future.complete(null);
            }
            // A permissive type must not turn null into a phantom zero-byte record.
            final DataType<byte[]> type = mock(DataType.class);
            dispatch(mode, size -> future, type, status, sink);
            status.records = 99;
            future.complete(null);
            assertEquals(0, sink.records);
            assertNull(sink.failure);
            assertEquals(logged(mode) ? 1 : 0, sink.timeouts);
            assertEquals(99, status.records);
        }
    }

    @ParameterizedTest
    @EnumSource(Mode.class)
    void successfulCompletionUsesCapturedCount(Mode mode) throws Exception {
        for (boolean immediate : new boolean[]{false, true}) {
            final ByteArray type = new ByteArray();
            final byte[] data = type.setTime(new byte[16], 123);
            final CompletableFuture<byte[]> future = new CompletableFuture<>();
            final Sink sink = new Sink();
            final Status status = new Status();
            if (immediate) {
                future.complete(data);
            }
            dispatch(mode, size -> future, type, status, sink);
            status.records = 99;
            future.complete(data);
            assertEquals(1, sink.records);
            assertEquals(16, sink.bytes);
            if (mode == Mode.TIMESTAMP || mode == Mode.TIMESTAMP_LOGGED) {
                assertEquals(123, sink.startTime);
            }
            assertEquals(logged(mode) ? 1 : 0, sink.requests);
            assertEquals(0, sink.timeouts);
            assertNull(sink.failure);
        }
    }

    @ParameterizedTest
    @EnumSource(Mode.class)
    void exceptionalCompletionReachesChannel(Mode mode) throws Exception {
        final IOException failure = new IOException("storage failure");
        final Sink sink = new Sink();
        dispatch(mode, size -> CompletableFuture.failedFuture(failure), new ByteArray(), new Status(), sink);
        assertSame(failure, sink.failure);
        assertEquals(0, sink.records);
    }

    @ParameterizedTest
    @EnumSource(Mode.class)
    void timeoutKeepsExistingRouting(Mode mode) throws Exception {
        final TimeoutException failure = new TimeoutException("empty backend");
        final Sink sink = new Sink();
        dispatch(mode, size -> CompletableFuture.failedFuture(failure), new ByteArray(), new Status(), sink);
        assertEquals(logged(mode) ? 1 : 0, sink.timeouts);
        assertSame(logged(mode) ? null : failure, sink.failure);
        assertEquals(0, sink.records);
    }

    @ParameterizedTest
    @EnumSource(Mode.class)
    @SuppressWarnings("unchecked")
    void processingFailureIsNotLostInDiscardedDependentFuture(Mode mode) throws Exception {
        final DataType<byte[]> type = mock(DataType.class);
        final IllegalArgumentException failure = new IllegalArgumentException("malformed payload");
        when(type.length(any())).thenThrow(failure);
        when(type.getTime(any())).thenThrow(failure);
        for (boolean immediate : new boolean[]{false, true}) {
            final CompletableFuture<byte[]> future = new CompletableFuture<>();
            final Sink sink = new Sink();
            if (immediate) {
                future.complete(new byte[16]);
            }
            dispatch(mode, size -> future, type, new Status(), sink);
            future.complete(new byte[16]);
            assertSame(failure, sink.failure);
            assertEquals(0, sink.records);
        }
    }

    @ParameterizedTest
    @EnumSource(Mode.class)
    void nullFutureRemainsAnIoFailure(Mode mode) {
        assertThrows(IOException.class,
                () -> dispatch(mode, size -> null, new ByteArray(), new Status(), new Sink()));
    }

    private static boolean logged(Mode mode) {
        return mode == Mode.READ_LOGGED || mode == Mode.TIMESTAMP_LOGGED;
    }

    private static void dispatch(Mode mode, AsyncReader<byte[]> reader, DataType<byte[]> type,
                                 Status status, Sink sink) throws IOException {
        final NanoSeconds time = new NanoSeconds();
        switch (mode) {
            case READ -> reader.recordRead(type, 16, time, status, sink);
            case READ_LOGGED -> reader.recordRead(type, 16, time, status, sink, 0, sink);
            case TIMESTAMP -> reader.recordReadTime(type, 16, time, status, sink);
            case TIMESTAMP_LOGGED -> reader.recordReadTime(type, 16, time, status, sink, 0, sink);
        }
    }

    private static final class Sink implements PerlChannel, ReadRequestsLogger {
        private long startTime;
        private int records;
        private int bytes;
        private long requests;
        private long timeouts;
        private Throwable failure;

        @Override
        public void send(long start, long end, int count, int size) {
            startTime = start;
            records += count;
            bytes += size;
        }

        @Override
        @SuppressFBWarnings(value = "EI_EXPOSE_REP2",
                justification = "Tests assert the exact failure delivered to the channel")
        public void throwException(Throwable ex) {
            failure = ex;
        }

        @Override
        public void recordReadRequests(int id, long start, long size, long count) {
            requests += count;
        }

        @Override
        public void recordReadTimeoutEvents(int id, long start, long count) {
            timeouts += count;
        }

        @Override
        public int getMaxReaderIDs() {
            return 1;
        }
    }
}
