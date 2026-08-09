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

import org.junit.jupiter.api.Test;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests benchmark lifecycle calculations that must be independent of a storage driver.
 */
final class SbkBenchmarkTest {

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
    void stopsAndDrainsRecordersWhenWorkersFinishEarly() {
        final CompletableFuture<Void> workers = new CompletableFuture<>();
        final CompletableFuture<Void> readerRecorder = new CompletableFuture<>();
        final AtomicInteger stopCalls = new AtomicInteger();

        final CompletableFuture<Void> completion = SbkBenchmark.drainRecordersAfterWorkers(
                workers, null, readerRecorder, stopCalls::incrementAndGet);

        assertFalse(completion.isDone());
        workers.complete(null);
        assertEquals(1, stopCalls.get());
        assertFalse(completion.isDone());

        readerRecorder.complete(null);
        assertTrue(completion.isDone());
    }
}
