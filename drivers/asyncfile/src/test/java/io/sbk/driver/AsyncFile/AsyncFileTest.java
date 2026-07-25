/**
 * Copyright (c) KMG. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 */
package io.sbk.driver.AsyncFile;

import io.perl.api.PerlChannel;
import io.sbk.api.Status;
import io.sbk.data.impl.NioByteBuffer;
import io.sbk.params.impl.SbkParameters;
import io.time.NanoSeconds;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.atomic.LongAdder;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * Integration tests for asynchronous file completion and measurement.
 */
final class AsyncFileTest {
    private static final int RECORDS = 25;
    private static final int RECORD_SIZE = 32;

    @TempDir
    private Path tempDir;

    @Test
    void fixedRecordWriteAndReadDrainEveryCompletion() throws Exception {
        Path file = tempDir.resolve("async-file.data");
        SbkParameters writeParams = parameters("-writers", "1");
        NioByteBuffer dataType = new NioByteBuffer();
        CountingChannel writes = new CountingChannel();
        ByteBuffer data = ByteBuffer.allocate(RECORD_SIZE);

        try (CloseableAsyncFileWriter writer = new CloseableAsyncFileWriter(
                new AsyncFileWriter(0, writeParams, file.toString()))) {
            Status status = new Status();
            for (int index = 0; index < RECORDS; index++) {
                writer.delegate.recordWrite(dataType, data, RECORD_SIZE,
                        new NanoSeconds(), status, writes);
            }
            writer.delegate.sync();
        }

        assertEquals(RECORDS, writes.records.sum());
        assertNull(writes.failure);
        assertEquals((long) RECORDS * RECORD_SIZE, Files.size(file));

        SbkParameters readParams = parameters("-readers", "1");
        CountingChannel reads = new CountingChannel();
        try (CloseableAsyncFileReader reader = new CloseableAsyncFileReader(
                new AsyncFileReader(0, readParams, file.toString()))) {
            Status status = new Status();
            for (int index = 0; index < RECORDS; index++) {
                reader.delegate.recordRead(dataType, RECORD_SIZE,
                        new NanoSeconds(), status, reads);
            }
        }

        assertEquals(RECORDS, reads.records.sum());
        assertNull(reads.failure);
    }

    private static SbkParameters parameters(String workerOption, String workerCount) throws Exception {
        SbkParameters params = new SbkParameters("async-file-test");
        params.parseArgs(new String[]{workerOption, workerCount, "-size",
                Integer.toString(RECORD_SIZE), "-records", Integer.toString(RECORDS)});
        return params;
    }

    private static final class CountingChannel implements PerlChannel {
        private final LongAdder records = new LongAdder();
        private volatile Throwable failure;

        @Override
        public void send(long startTime, long endTime, int count, int bytes) {
            records.add(count);
        }

        @Override
        public void throwException(Throwable ex) {
            failure = ex;
        }
    }

    private static final class CloseableAsyncFileWriter implements AutoCloseable {
        private final AsyncFileWriter delegate;

        private CloseableAsyncFileWriter(AsyncFileWriter delegate) {
            this.delegate = delegate;
        }

        @Override
        public void close() throws Exception {
            delegate.close();
        }
    }

    private static final class CloseableAsyncFileReader implements AutoCloseable {
        private final AsyncFileReader delegate;

        private CloseableAsyncFileReader(AsyncFileReader delegate) {
            this.delegate = delegate;
        }

        @Override
        public void close() throws Exception {
            delegate.close();
        }
    }
}
