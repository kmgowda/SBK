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
import io.sbk.data.impl.ByteArray;
import io.time.NanoSeconds;
import org.junit.jupiter.api.Test;

import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Verifies that common asynchronous adapters measure callback completion.
 */
final class AsyncOperationTimingTest {

    @Test
    void writerReportsOnlyAfterFutureCompletion() throws Exception {
        CompletableFuture<Void> completion = new CompletableFuture<>();
        Writer<byte[]> writer = new Writer<>() {
            @Override
            public CompletableFuture<?> writeAsync(byte[] data) {
                return completion;
            }

            @Override
            public void close() {
            }
        };
        CapturingChannel channel = new CapturingChannel();
        Status status = new Status();

        writer.recordWrite(new ByteArray(), new byte[16], 16,
                new NanoSeconds(), status, channel);

        assertEquals(0, channel.records);
        status.records = 99;
        completion.complete(null);
        assertEquals(1, channel.records);
        assertEquals(16, channel.bytes);
        assertTrue(channel.endTime >= channel.startTime);
    }

    @Test
    void readerReportsOnlyAfterFutureCompletion() throws Exception {
        CompletableFuture<byte[]> completion = new CompletableFuture<>();
        AsyncReader<byte[]> reader = new AsyncReader<>() {
            @Override
            @SuppressFBWarnings(value = "EI_EXPOSE_REP",
                    justification = "The shared future is the callback-completion signal under test")
            public CompletableFuture<byte[]> readAsync(int size) {
                return completion;
            }
        };
        CapturingChannel channel = new CapturingChannel();
        Status status = new Status();

        reader.recordRead(new ByteArray(), 16, new NanoSeconds(), status, channel);

        assertEquals(0, channel.records);
        status.records = 99;
        completion.complete(new byte[16]);
        assertEquals(1, channel.records);
        assertEquals(16, channel.bytes);
        assertTrue(channel.endTime >= channel.startTime);
    }

    private static final class CapturingChannel implements PerlChannel {
        private long startTime;
        private long endTime;
        private int records;
        private int bytes;

        @Override
        public void send(long start, long end, int recordCount, int byteCount) {
            startTime = start;
            endTime = end;
            records = recordCount;
            bytes = byteCount;
        }

        @Override
        public void throwException(Throwable ex) {
            throw new IllegalStateException(ex);
        }
    }
}
