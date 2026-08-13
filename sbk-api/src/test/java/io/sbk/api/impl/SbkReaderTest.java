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

import io.perl.api.PerlChannel;
import io.sbk.api.DataReader;
import io.sbk.api.Reader;
import io.sbk.logger.impl.SystemLogger;
import io.sbk.params.impl.SbkParameters;
import io.time.NanoSeconds;
import org.junit.jupiter.api.Test;

import java.io.EOFException;
import java.io.IOException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.LockSupport;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Tests driver-specific reader completion signals.
 */
final class SbkReaderTest {
    private static final PerlChannel CHANNEL = new PerlChannel() {
        @Override
        public void send(long startTime, long endTime, int records, int bytes) {
        }

        @Override
        public void throwException(Throwable ex) {
            throw new AssertionError("Worker failures must remain worker-local", ex);
        }
    };

    @Test
    void exitsWhenDriverThrowsEof() throws Exception {
        final DataReader<Object> reader = readerThatThrowsEof();
        final ExecutorService executor = Executors.newSingleThreadExecutor();

        try {
            createReader(reader, executor)
                    .run(60, 0).get(2, TimeUnit.SECONDS);
        } finally {
            executor.shutdownNow();
        }
    }

    @Test
    void treatsNoDataAsNonTerminal() throws Exception {
        final AtomicInteger readCalls = new AtomicInteger();
        final DataReader<Object> reader = readerThatReturnsNoData(readCalls);
        final ExecutorService executor = Executors.newSingleThreadExecutor();

        try {
            createReader(reader, executor)
                    .run(1, 0).get(5, TimeUnit.SECONDS);

            assertTrue(readCalls.get() > 1);
        } finally {
            executor.shutdownNow();
        }
    }

    @Test
    void propagatesStorageFailure() throws Exception {
        final DataReader<Object> reader = readerThatFails();
        final ExecutorService executor = Executors.newSingleThreadExecutor();
        final TestSystemLogger logger = new TestSystemLogger();

        try {
            final ExecutionException failure = assertThrows(ExecutionException.class,
                    () -> createReader(reader, logger, executor).run(1, 0).get(2, TimeUnit.SECONDS));

            assertInstanceOf(IOException.class, failure.getCause());
            assertEquals(0, logger.readersCount());
        } finally {
            executor.shutdownNow();
        }
    }

    private static SbkReader createReader(DataReader<Object> reader, ExecutorService executor) throws Exception {
        return createReader(reader, new SystemLogger(), executor);
    }

    private static SbkReader createReader(DataReader<Object> reader, SystemLogger logger,
                                          ExecutorService executor) throws Exception {
        final SbkParameters params = new SbkParameters("reader-completion-test");
        params.parseArgs(new String[]{"-readers", "1", "-size", "10", "-seconds", "1"});
        return new SbkReader(0, params, CHANNEL, null, new NanoSeconds(), reader,
                logger, null, executor);
    }

    private static Reader<Object> readerThatThrowsEof() {
        return new Reader<>() {
            @Override
            public Object read() throws EOFException {
                throw new EOFException("end of finite input");
            }

            @Override
            public void close() {
            }
        };
    }

    private static Reader<Object> readerThatReturnsNoData(AtomicInteger readCalls) {
        return new Reader<>() {
            @Override
            public Object read() {
                readCalls.incrementAndGet();
                LockSupport.parkNanos(TimeUnit.MILLISECONDS.toNanos(1));
                return null;
            }

            @Override
            public void close() {
            }
        };
    }

    private static Reader<Object> readerThatFails() {
        return new Reader<>() {
            @Override
            public Object read() throws IOException {
                throw new IOException("Disk I/O error");
            }

            @Override
            public void close() {
            }
        };
    }

    private static final class TestSystemLogger extends SystemLogger {
        private int readersCount() {
            return getReadersCount();
        }
    }
}
