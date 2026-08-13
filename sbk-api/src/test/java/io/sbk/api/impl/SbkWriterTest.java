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
import io.sbk.api.Writer;
import io.sbk.data.DataType;
import io.sbk.logger.impl.SystemLogger;
import io.sbk.params.impl.SbkParameters;
import io.time.NanoSeconds;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Tests terminal storage failures reported by writer drivers.
 */
final class SbkWriterTest {
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
    void exitsAfterSynchronousDiskFailure() throws Exception {
        final AtomicInteger writeCalls = new AtomicInteger();
        final Writer<Object> driver = synchronousFailingWriter(writeCalls);
        final ExecutorService executor = Executors.newSingleThreadExecutor();
        final TestSystemLogger logger = new TestSystemLogger();

        try {
            final ExecutionException failure = assertThrows(ExecutionException.class,
                    () -> createWriter(driver, logger, executor).run(0, 100).get(2, TimeUnit.SECONDS));

            assertInstanceOf(IOException.class, failure.getCause());
            assertEquals(1, writeCalls.get());
            assertEquals(0, logger.writersCount());
        } finally {
            executor.shutdownNow();
        }
    }

    private static SbkWriter createWriter(Writer<Object> writer, SystemLogger logger,
                                          ExecutorService executor) throws Exception {
        final SbkParameters params = new SbkParameters("writer-completion-test");
        params.parseArgs(new String[]{"-writers", "1", "-size", "10", "-records", "100"});
        return new SbkWriter(0, params, CHANNEL, new ObjectDataType(), new NanoSeconds(), writer,
                logger, null, executor);
    }

    private static Writer<Object> synchronousFailingWriter(AtomicInteger writeCalls) {
        return new Writer<>() {
            @Override
            public CompletableFuture<?> writeAsync(Object data) throws IOException {
                writeCalls.incrementAndGet();
                throw new IOException("Disk I/O error");
            }

            @Override
            public void close() {
            }
        };
    }

    private static final class ObjectDataType implements DataType<Object> {
        @Override
        public Object allocate(int size) {
            return new Object();
        }

        @Override
        public Object create(int size) {
            return new Object();
        }

        @Override
        public int length(Object data) {
            return 10;
        }

        @Override
        public Object setTime(Object data, long time) {
            return data;
        }

        @Override
        public long getTime(Object data) {
            return 0;
        }

        @Override
        public int getWriteReadMinSize() {
            return 1;
        }
    }

    private static final class TestSystemLogger extends SystemLogger {
        private int writersCount() {
            return getWritersCount();
        }
    }
}
