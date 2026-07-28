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
import io.sbk.data.DataType;
import io.sbk.params.impl.SbkParameters;
import io.time.NanoSeconds;
import io.time.Time;
import org.junit.jupiter.api.Test;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

/**
 * Verifies the legacy callback reader's count and duration completion.
 */
@SuppressWarnings("deprecation")
final class SbkCallbackReaderTest {

    @Test
    void completesAfterAllBatchedRecordsAreRead() throws Exception {
        SbkParameters parameters = parameters("-readers", "1", "-size",
                "8", "-records", "5");
        CapturingChannel channel = new CapturingChannel();
        SbkCallbackReader reader = new SbkCallbackReader(0, parameters,
                channel, 1, new TestDataType(), new NanoSeconds());
        CompletableFuture<Void> completion = reader.start();

        reader.record(1, 2, 20, 2);
        reader.record(2, 3, 20, 2);

        assertFalse(completion.isDone());
        assertEquals(4, channel.records.get());

        reader.record(3, 4, 10, 1);

        completion.get(1, TimeUnit.SECONDS);
        assertEquals(5, channel.records.get());
        assertEquals(50, channel.bytes.get());
    }

    @Test
    void convertsNanosecondsBeforeCheckingDuration() throws Exception {
        SbkParameters parameters = parameters("-readers", "1", "-size",
                "8", "-seconds", "1");
        CapturingChannel channel = new CapturingChannel();
        NanoSeconds time = new NanoSeconds();
        SbkCallbackReader reader = new SbkCallbackReader(0, parameters,
                channel, 1, new TestDataType(), time);
        CompletableFuture<Void> completion = reader.start();
        long currentTime = time.getCurrentTime();

        reader.record(currentTime,
                currentTime + 10L * Time.NS_PER_MS, 1, 1);

        assertFalse(completion.isDone());

        reader.record(currentTime,
                currentTime + Time.NS_PER_SEC, 1, 1);

        completion.get(1, TimeUnit.SECONDS);
    }

    private static SbkParameters parameters(String... arguments)
            throws Exception {
        SbkParameters parameters = new SbkParameters("callback-reader-test");
        parameters.parseArgs(arguments);
        return parameters;
    }

    private static final class CapturingChannel implements PerlChannel {
        private final AtomicInteger records = new AtomicInteger();
        private final AtomicInteger bytes = new AtomicInteger();

        @Override
        public void send(long startTime, long endTime, int events,
                         int dataSize) {
            records.addAndGet(events);
            bytes.addAndGet(dataSize);
        }

        @Override
        public void throwException(Throwable ex) {
            throw new IllegalStateException(ex);
        }
    }

    private static final class TestDataType implements DataType<Object> {
        @Override
        public Object allocate(int size) {
            return new byte[size];
        }

        @Override
        public Object create(int size) {
            return allocate(size);
        }

        @Override
        public int length(Object data) {
            return ((byte[]) data).length;
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
}
