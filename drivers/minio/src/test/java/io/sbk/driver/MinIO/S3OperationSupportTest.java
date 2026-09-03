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
import java.net.SocketTimeoutException;
import java.nio.charset.StandardCharsets;
import java.util.HexFormat;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
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
        assertTrue(S3Operation.LIST.usesMainBucket());
        assertFalse(S3Operation.BUCKET_CREATE.usesMainBucket());
        assertThrows(IllegalArgumentException.class, () -> S3Operation.fromString("compose"));
    }

    @Test
    public void checksumAlgorithmsMatchStandardKnownVectors() {
        byte[] input = "123456789".getBytes(StandardCharsets.US_ASCII);

        assertArrayEquals(HexFormat.of().parseHex("cbf43926"),
                S3ChecksumUtil.compute(input, S3ChecksumUtil.Algorithm.CRC32));
        assertArrayEquals(HexFormat.of().parseHex("e3069283"),
                S3ChecksumUtil.compute(input, S3ChecksumUtil.Algorithm.CRC32C));
        assertArrayEquals(HexFormat.of().parseHex("f7c3bc1d808e04732adf679965ccc34ca7ae3441"),
                S3ChecksumUtil.compute(input, S3ChecksumUtil.Algorithm.SHA1));
        assertArrayEquals(HexFormat.of().parseHex(
                        "15e2b0d3c33891ebb0f1ef609ec419420c20e320ce94c65fbc8c3312448eb225"),
                S3ChecksumUtil.compute(input, S3ChecksumUtil.Algorithm.SHA256));
        assertArrayEquals(HexFormat.of().parseHex("ae8b14860a799888"),
                S3ChecksumUtil.compute(input, S3ChecksumUtil.Algorithm.CRC64NVME));
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
    public void rangeOffsetsSupportFixedSequentialAndSeededRandomSelection() {
        S3RangeOffsetSelector fixed = new S3RangeOffsetSelector("fixed", 1024, 4096, 512, 7);
        assertEquals(1024, fixed.next(16384, 1024));

        S3RangeOffsetSelector sequential = new S3RangeOffsetSelector(
                "sequential", 1024, 1536, 512, 7);
        assertEquals(List.of(1024L, 1536L, 2048L, 1024L),
                java.util.stream.IntStream.range(0, 4)
                        .mapToObj(ignored -> sequential.next(16384, 1024)).toList());

        S3RangeOffsetSelector random = new S3RangeOffsetSelector(
                "random", 0, 8192, 4096, 42);
        S3RangeOffsetSelector sameSeed = new S3RangeOffsetSelector(
                "random", 0, 8192, 4096, 42);
        for (int sample = 0; sample < 100; sample++) {
            long offset = random.next(16384, 1024);
            assertEquals(offset, sameSeed.next(16384, 1024));
            assertTrue(offset == 0 || offset == 4096);
        }
        assertThrows(ArithmeticException.class,
                () -> new S3RangeOffsetSelector("fixed", Long.MAX_VALUE, 2, 1, 0));
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
        assertTrue(mix.usesMainBucket());
        assertEquals(2, mix.countOccurrences(S3Operation.COPY, 8, 0));
        assertEquals(2, mix.countOccurrences(S3Operation.COPY, 8, 1));
        assertEquals(1, mix.countOccurrences(S3Operation.COPY, 5, 1));
    }

    @Test
    public void weightedOccurrenceCountingHandlesWrappedIntervals() {
        S3OperationMix mix = S3OperationMix.parse("put=3,copy=3,delete=2",
                S3Operation.PUT, true, 0);

        assertEquals(6, mix.countOccurrences(S3Operation.PUT, 16, 0));
        assertEquals(6, mix.countOccurrences(S3Operation.COPY, 16, 0));
        assertEquals(4, mix.countOccurrences(S3Operation.DELETE, 16, 0));
        assertEquals(3, mix.countOccurrences(S3Operation.PUT, 7, 7));
        assertEquals(0, mix.countOccurrences(S3Operation.GET, Long.MAX_VALUE, 0));
    }

    @Test
    public void tagParsingRejectsMalformedAndDuplicateEntries() {
        assertEquals(java.util.Map.of("team", "storage", "phase", "test"),
                MinIOWriter.parseTags("team=storage,phase=test"));
        assertThrows(IllegalArgumentException.class,
                () -> MinIOWriter.parseTags("team=storage,broken"));
        assertThrows(IllegalArgumentException.class,
                () -> MinIOWriter.parseTags("team=storage,team=duplicate"));
    }

    @Test
    public void uniformObjectSizesAreReproducibleAndSampleTheCompleteRange() {
        final int minimum = 1024;
        final int maximum = 1048576;
        final int samples = 100000;
        S3ObjectSizeSelector first = S3ObjectSizeSelector.parse(
                "uniform:" + minimum + ":" + maximum, 7);
        S3ObjectSizeSelector second = S3ObjectSizeSelector.parse(
                "uniform:" + minimum + ":" + maximum, 7);
        S3ObjectSizeSelector otherWorker = S3ObjectSizeSelector.parse(
                "uniform:" + minimum + ":" + maximum, 8);
        long sum = 0;
        int observedMinimum = Integer.MAX_VALUE;
        int observedMaximum = Integer.MIN_VALUE;
        boolean workersDiffer = false;
        for (int sample = 0; sample < samples; sample++) {
            int value = first.next(1);
            assertEquals(value, second.next(1));
            workersDiffer |= value != otherWorker.next(1);
            assertTrue(value >= minimum && value <= maximum);
            sum += value;
            observedMinimum = Math.min(observedMinimum, value);
            observedMaximum = Math.max(observedMaximum, value);
        }
        double expectedMean = (minimum + maximum) / 2.0;
        assertEquals(expectedMean, sum / (double) samples, expectedMean * 0.01);
        assertTrue(observedMinimum < minimum + (maximum - minimum) / 100);
        assertTrue(observedMaximum > maximum - (maximum - minimum) / 100);
        assertTrue(workersDiffer);
        assertEquals(maximum, first.maximum(1));
    }

    @Test
    public void sweepAndWeightedObjectSizesRetainDeterministicCycles() {
        S3ObjectSizeSelector sweep = S3ObjectSizeSelector.parse("sweep:10:12", 0);
        assertEquals(List.of(10, 11, 12, 10),
                java.util.stream.IntStream.range(0, 4).map(ignored -> sweep.next(1))
                        .boxed().toList());
        assertEquals(12, sweep.maximum(1));

        S3ObjectSizeSelector weighted = S3ObjectSizeSelector.parse(
                "weighted:64=2,1024=1", 0);
        assertEquals(List.of(64, 64, 1024, 64),
                java.util.stream.IntStream.range(0, 4).map(ignored -> weighted.next(1))
                        .boxed().toList());
        assertEquals(1024, weighted.maximum(1));
        assertThrows(IllegalArgumentException.class,
                () -> S3ObjectSizeSelector.parse("uniform:12:10", 0));
        assertThrows(IllegalArgumentException.class,
                () -> S3ObjectSizeSelector.parse("sweep:12:10", 0));
    }

    @Test
    public void generatedKeysSupportHashedRandomAndPartitionPrefixes() {
        MinIOConfig config = new MinIOConfig();
        config.bucketName = "bucket";
        config.prefix = "objects";
        config.partitionCount = 4;
        config.partitionIndex = 2;
        config.partitionByPrefix = true;
        config.keyDistribution = "hashed";
        config.dataSeed = 42;
        S3ObjectKey hashed = new S3ObjectKey(config, 0, "run");
        assertTrue(hashed.next().startsWith("objects/partition-2/"));
        assertEquals("objects/partition-2/", S3ObjectKey.partitionPrefix(config));

        config.keyDistribution = "random";
        S3ObjectKey random = new S3ObjectKey(config, 0, "run");
        assertNotEquals(random.next(), random.next());
        assertThrows(IllegalArgumentException.class,
                () -> S3ObjectKey.validateDistribution("zipf"));
    }

    @Test
    public void endpointMetricsAttributeCompletionsRetriesAndFailures() {
        S3EndpointMetrics metrics = new S3EndpointMetrics("http://node:9020");
        metrics.success(1024);
        metrics.retry();
        metrics.failure();

        assertEquals("endpoint=http://node:9020, operations=1, bytes=1024, retries=1, failures=1",
                metrics.summary());
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
    public void asyncExecutorDoesNotHideSocketTimeoutsAsCleanShutdown() throws Exception {
        S3AsyncExecutor executor = new S3AsyncExecutor(1);
        executor.acquire();
        executor.track(CompletableFuture.failedFuture(
                new SocketTimeoutException("active request timed out")));

        IOException failure = assertThrows(IOException.class, executor::await);
        assertTrue(failure.getCause() instanceof SocketTimeoutException);
    }

    @Test
    public void asyncExecutorAwaitsTheMeasurementCallback() throws Exception {
        S3AsyncExecutor executor = new S3AsyncExecutor(1);
        CompletableFuture<String> sdkFuture = new CompletableFuture<>();
        CountDownLatch callbackStarted = new CountDownLatch(1);
        CountDownLatch releaseCallback = new CountDownLatch(1);
        executor.acquire();
        executor.track(sdkFuture, (result, thrown) -> {
            callbackStarted.countDown();
            try {
                releaseCallback.await();
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
                throw new java.util.concurrent.CompletionException(ex);
            }
        });

        CompletableFuture<Void> sdkCompletion = CompletableFuture.runAsync(() -> sdkFuture.complete("done"));
        assertTrue(callbackStarted.await(5, TimeUnit.SECONDS));
        CompletableFuture<Void> awaitCompletion = CompletableFuture.runAsync(() -> {
            try {
                executor.await();
            } catch (IOException ex) {
                throw new java.util.concurrent.CompletionException(ex);
            }
        });

        assertThrows(TimeoutException.class,
                () -> awaitCompletion.get(50, TimeUnit.MILLISECONDS));
        assertEquals(1, executor.pendingCount());
        releaseCallback.countDown();
        sdkCompletion.get(5, TimeUnit.SECONDS);
        awaitCompletion.get(5, TimeUnit.SECONDS);
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
        AtomicInteger retries = new AtomicInteger();
        S3RetryPolicy policy = new S3RetryPolicy(3, 0, retries::incrementAndGet);
        String result = policy.execute(() -> {
            if (synchronousAttempts.incrementAndGet() < 3) {
                throw new IOException("temporary");
            }
            return "ok";
        });
        assertEquals("ok", result);
        assertEquals(3, synchronousAttempts.get());
        assertEquals(2, retries.get());

        AtomicInteger asynchronousAttempts = new AtomicInteger();
        String asyncResult = policy.executeAsync(() ->
                asynchronousAttempts.incrementAndGet() < 2
                        ? CompletableFuture.failedFuture(new IOException("temporary"))
                        : CompletableFuture.completedFuture("ok")).get(5, TimeUnit.SECONDS);
        assertEquals("ok", asyncResult);
        assertEquals(2, asynchronousAttempts.get());

        AtomicInteger timeoutAttempts = new AtomicInteger();
        String timeoutResult = policy.execute(() -> {
            if (timeoutAttempts.incrementAndGet() < 2) {
                throw new SocketTimeoutException("temporary socket timeout");
            }
            return "ok";
        });
        assertEquals("ok", timeoutResult);
        assertEquals(2, timeoutAttempts.get());
    }
}
