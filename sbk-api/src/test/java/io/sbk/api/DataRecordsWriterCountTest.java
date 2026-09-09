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
import io.sbk.data.DataType;
import io.sbk.logger.WriteRequestsLogger;
import io.sbk.params.impl.SbkParameters;
import io.time.NanoSeconds;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;

/**
 * Verifies that duration-based rate-controlled writers retain cumulative counts beyond {@code int} range.
 */
final class DataRecordsWriterCountTest {
    private static final long EXPECTED_COUNT = (long) Integer.MAX_VALUE + 1L;
    private static final PerlChannel CHANNEL = new PerlChannel() {
        @Override
        public void send(long startTime, long endTime, int records, int bytes) {
        }

        @Override
        public void throwException(Throwable ex) {
            throw new AssertionError(ex);
        }
    };

    @Test
    void durationRateControlRetainsLongCount() throws Exception {
        final CapturingRateController controller = new CapturingRateController();

        assertThrows(CountCaptured.class, () -> new LargeBatchWriter().RecordsWriterTimeSync(
                worker(), 60, new ObjectDataType(), new Object(), 1, new NanoSeconds(), controller));

        assertEquals(List.of((long) Integer.MAX_VALUE, EXPECTED_COUNT), controller.counts);
    }

    @Test
    void loggedDurationRateControlRetainsLongCount() throws Exception {
        final CapturingRateController controller = new CapturingRateController();

        assertThrows(CountCaptured.class, () -> new LargeBatchWriter().RecordsWriterTimeSync(
                worker(), 60, new ObjectDataType(), new Object(), 1, new NanoSeconds(), controller,
                mock(WriteRequestsLogger.class)));

        assertEquals(List.of((long) Integer.MAX_VALUE, EXPECTED_COUNT), controller.counts);
    }

    private static Worker worker() throws Exception {
        final SbkParameters params = new SbkParameters("writer-count-test");
        params.parseArgs(new String[]{"-writers", "1", "-size", "1", "-seconds", "60", "-throughput", "1"});
        return new Worker(0, params, CHANNEL) { };
    }

    private static final class CapturingRateController implements RateController {
        private final List<Long> counts = new ArrayList<>();

        @Override
        public void start(int recordsPerSec) {
        }

        @Override
        public void control(long records, double elapsedSec) {
            counts.add(records);
            if (counts.size() == 2) {
                throw new CountCaptured();
            }
        }
    }

    private static final class CountCaptured extends RuntimeException {
        private static final long serialVersionUID = 1L;
    }

    private static final class LargeBatchWriter implements Writer<Object> {
        private int calls;

        @Override
        public CompletableFuture<?> write(DataType<Object> dType, Object data, int size,
                                          io.time.Time time, Status status) {
            status.bytes = size;
            status.records = calls++ == 0 ? Integer.MAX_VALUE : 1;
            status.startTime = time.getCurrentTime();
            return null;
        }

        @Override
        public CompletableFuture<?> write(DataType<Object> dType, Object data, int size,
                                          io.time.Time time, Status status, int id,
                                          WriteRequestsLogger logger) {
            logger.recordWriteRequests(id, time.getCurrentTime(), size, 1);
            return write(dType, data, size, time, status);
        }

        @Override
        public CompletableFuture<?> writeAsync(Object data) {
            return null;
        }

        @Override
        public void close() {
        }
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
            return 1;
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
