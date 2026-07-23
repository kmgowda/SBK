/**
 * Copyright (c) KMG. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.sbk.driver.MinIO;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InterruptedIOException;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Unit tests for the operation-independent MinIO driver support classes.
 */
public class S3OperationSupportTest {

    @Test
    public void operationAliasesAndCategoriesAreResolved() {
        assertEquals(S3Operation.PUT, S3Operation.fromString("create"));
        assertEquals(S3Operation.UPDATE, S3Operation.fromString("overwrite"));
        assertEquals(S3Operation.STAT, S3Operation.fromString("head"));
        assertEquals(S3Operation.RANGE_GET, S3Operation.fromString("range-read"));
        assertTrue(S3Operation.BUCKET_DELETE.isWriterOperation());
        assertFalse(S3Operation.BUCKET_LIST.isWriterOperation());
        assertTrue(S3Operation.COPY.requiresObjectCatalog());
        assertFalse(S3Operation.LIST.requiresObjectCatalog());
        assertThrows(IllegalArgumentException.class, () -> S3Operation.fromString("compose"));
    }

    @Test
    public void catalogPartitionsObjectsAndClaimsDeletesOnce() throws Exception {
        S3ObjectRef first = new S3ObjectRef("first", null, 100, 0);
        S3ObjectRef second = new S3ObjectRef("second", "v2", 200, 0);
        S3ObjectCatalog catalog = new S3ObjectCatalog(List.of(first, second));

        assertEquals(first, catalog.nextForReader(0, 2, 0));
        assertEquals(second, catalog.nextForReader(1, 2, 0));
        assertEquals(first, catalog.nextForReader(0, 2, 1));
        assertEquals(first, catalog.claimDelete());
        assertEquals(second, catalog.claimDelete());
        assertNull(catalog.claimDelete());

        S3ObjectRef published = new S3ObjectRef("new", null, 300, 1234);
        catalog.publish(published);
        assertEquals(published, catalog.pollPublished(10, TimeUnit.MILLISECONDS));
    }

    @Test
    public void rangeSelectionSkipsObjectsThatCannotSatisfyTheOffset() {
        S3ObjectRef tooSmall = new S3ObjectRef("small", null, 64, 0);
        S3ObjectRef eligible = new S3ObjectRef("large", null, 4096, 0);
        S3ObjectCatalog catalog = new S3ObjectCatalog(List.of(tooSmall, eligible));

        assertTrue(catalog.hasObjectLargerThan(1024));
        assertEquals(eligible, catalog.nextForReader(0, 1, 0, 1024));
        assertFalse(catalog.hasObjectLargerThan(4096));
        assertNull(catalog.nextForReader(0, 1, 0, 4096));
    }

    @Test
    public void generatedBucketNamesAreValidUniqueAndBounded() {
        S3BucketName generator = new S3BucketName(
                "An Invalid Prefix With Spaces And A Very Long Name Repeated Repeated Repeated",
                "run-token", 7);
        String first = generator.next();
        String second = generator.next();

        assertNotEquals(first, second);
        assertTrue(first.length() <= 63);
        assertTrue(first.matches("[a-z0-9][a-z0-9.-]*[a-z0-9]"));
    }

    @Test
    public void reusableDataBufferIsFullyRefilled() {
        S3DataGenerator generator = new S3DataGenerator(100, true);
        byte[] data = new byte[8192];
        java.util.Arrays.fill(data, (byte) 1);

        generator.fill(data, 0, data.length);

        assertArrayEquals(new byte[data.length], data);
    }

    @Test
    public void seededDataAndWeightedOperationMixesAreReproducible() {
        S3DataGenerator first = new S3DataGenerator(25, false, 12345);
        S3DataGenerator second = new S3DataGenerator(25, false, 12345);
        first.newObject();
        second.newObject();
        assertArrayEquals(first.generate(8192), second.generate(8192));

        S3OperationMix mix = S3OperationMix.parse("put=3,copy=1",
                S3Operation.PUT, true, 0);
        assertEquals(S3Operation.PUT, mix.next());
        assertEquals(S3Operation.PUT, mix.next());
        assertEquals(S3Operation.PUT, mix.next());
        assertEquals(S3Operation.COPY, mix.next());
        assertEquals(S3Operation.PUT, mix.next());
        assertTrue(mix.requiresObjectCatalog());
    }

    @Test
    public void asyncExecutorSurfacesSdkFailures() throws Exception {
        S3AsyncExecutor executor = new S3AsyncExecutor(1);
        executor.acquire();
        executor.track(CompletableFuture.failedFuture(new IllegalStateException("SDK failure")));

        IOException failure = assertThrows(IOException.class, executor::await);

        assertTrue(failure.getMessage().contains("SDK failure"));
    }

    @Test
    public void asyncExecutorTreatsInterruptedSdkCallsAsCleanShutdown() throws Exception {
        S3AsyncExecutor executor = new S3AsyncExecutor(1);
        executor.acquire();
        executor.track(CompletableFuture.failedFuture(
                new InterruptedIOException("benchmark stopped")));

        executor.await();
        assertEquals(0, executor.pendingCount());
    }

    @Test
    public void asyncExecutorsShareTheProcessWideLimit() throws Exception {
        Semaphore global = new Semaphore(1);
        S3AsyncExecutor first = new S3AsyncExecutor(2, global);
        S3AsyncExecutor second = new S3AsyncExecutor(2, global);
        first.acquire();
        CompletableFuture<Void> secondAcquired = CompletableFuture.runAsync(() -> {
            try {
                second.acquire();
                second.releaseFailedStart();
            } catch (IOException ex) {
                throw new java.util.concurrent.CompletionException(ex);
            }
        });

        assertThrows(TimeoutException.class,
                () -> secondAcquired.get(50, TimeUnit.MILLISECONDS));
        first.releaseFailedStart();
        secondAcquired.get(5, TimeUnit.SECONDS);
        assertEquals(1, global.availablePermits());
    }

    @Test
    public void retryPolicyRetriesOnlyWithinItsBound() throws Exception {
        AtomicInteger synchronousAttempts = new AtomicInteger();
        S3RetryPolicy policy = new S3RetryPolicy(3, 0);
        String result = policy.execute(() -> {
            if (synchronousAttempts.incrementAndGet() < 3) {
                throw new IOException("temporary");
            }
            return "ok";
        });
        assertEquals("ok", result);
        assertEquals(3, synchronousAttempts.get());

        AtomicInteger asynchronousAttempts = new AtomicInteger();
        String asyncResult = policy.executeAsync(() ->
                asynchronousAttempts.incrementAndGet() < 2
                        ? CompletableFuture.failedFuture(new IOException("temporary"))
                        : CompletableFuture.completedFuture("ok")).get(5, TimeUnit.SECONDS);
        assertEquals("ok", asyncResult);
        assertEquals(2, asynchronousAttempts.get());
    }
}
