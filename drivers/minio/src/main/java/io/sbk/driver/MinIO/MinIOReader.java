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

import io.minio.BucketExistsArgs;
import io.minio.GetObjectArgs;
import io.minio.GetObjectResponse;
import io.minio.GetObjectTagsArgs;
import io.minio.ListObjectsArgs;
import io.minio.MinioAsyncClient;
import io.minio.MinioClient;
import io.minio.Result;
import io.minio.StatObjectArgs;
import io.minio.messages.Item;
import io.perl.api.PerlChannel;
import io.sbk.api.Reader;
import io.sbk.api.Status;
import io.sbk.data.DataType;
import io.sbk.logger.ReadRequestsLogger;
import io.sbk.params.ParameterOptions;
import io.time.Time;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.LongAdder;

/**
 * Per-worker MinIO SDK reader for GET, range GET, stat, tagging, and listing.
 *
 * <p>The startup object catalog provides object size and version information,
 * so a normal GET performs exactly one S3 request rather than a HEAD followed
 * by a GET. Read-only workers partition the catalog; mixed PUT/GET workers
 * consume objects published by completed writers.
 */
public class MinIOReader implements Reader<byte[]> {
    private final int id;
    private final int readerCount;
    private final int configuredSize;
    private final boolean mixedWorkload;
    private final MinIOConfig config;
    private final S3Operation operation;
    private final S3OperationMix operationMix;
    private final MinioClient client;
    private final MinioAsyncClient asyncClient;
    private final S3ObjectCatalog catalog;
    private final List<String> bucketTargets;
    private final List<String> listPrefixes;
    private final ListObjectsArgs listArgs;
    private final S3RangeOffsetSelector rangeOffsetSelector;
    private final S3AsyncExecutor asyncExecutor;
    private final S3RetryPolicy retryPolicy;
    private final S3EndpointMetrics endpointMetrics;
    private final ExecutorService responseExecutor;
    private long objectSequence;
    private long bucketSequence;
    private final byte[] drainBuffer;
    private final ArrayBlockingQueue<byte[]> asyncDrainBuffers;

    /**
     * Create an S3 reader worker.
     *
     * @param id reader id
     * @param params parsed SBK parameters
     * @param config MinIO configuration
     * @param operation reader operation
     * @param client synchronous MinIO client
     * @param asyncClient asynchronous MinIO client, or {@code null}
     * @param catalog shared object catalog
     * @param bucketTargets optional explicit bucket targets
     * @param globalAsyncPermits shared process-wide async permits
     * @param endpointMetrics optional endpoint attribution
     * @param retryCount process-wide retry counter updated only on retry paths
     */
    public MinIOReader(int id, ParameterOptions params, MinIOConfig config, S3Operation operation,
                       MinioClient client, MinioAsyncClient asyncClient, S3ObjectCatalog catalog,
                       List<String> bucketTargets, Semaphore globalAsyncPermits,
                       S3EndpointMetrics endpointMetrics, LongAdder retryCount) {
        this.id = id;
        readerCount = Math.max(1, params.getReadersCount());
        configuredSize = params.getRecordSize();
        S3OperationMix writerMix = S3OperationMix.parse(config.writeMix,
                S3Operation.fromString(config.writeOperation), true, id);
        mixedWorkload = params.getWritersCount() > 0
                && S3MixedReadSource.parse(config.mixedReadSource) == S3MixedReadSource.PUBLISHED
                && (writerMix.contains(S3Operation.PUT) || writerMix.contains(S3Operation.COPY));
        this.config = config;
        this.operation = operation;
        operationMix = S3OperationMix.parse(config.readMix, operation, false, id);
        this.client = client;
        this.asyncClient = asyncClient;
        this.catalog = catalog;
        this.bucketTargets = bucketTargets;
        this.endpointMetrics = endpointMetrics;
        listPrefixes = parseList(config.listPrefixes);
        listArgs = buildListArgs();
        long rangeSeed = config.dataSeed == 0 ? System.nanoTime() : config.dataSeed + id;
        rangeOffsetSelector = new S3RangeOffsetSelector(config.rangeOffsetDistribution,
                config.rangeOffset, config.rangeWindowLength, config.rangeAlignment, rangeSeed);
        asyncExecutor = config.async
                ? new S3AsyncExecutor(config.asyncDepth, globalAsyncPermits) : null;
        retryPolicy = new S3RetryPolicy(config.retryMaxAttempts, config.retryBackoffMs,
                S3RetryPolicy.Strategy.parse(config.retryStrategy), config.retryMaxBackoffMs,
                config.retryJitter,
                () -> {
                    retryCount.increment();
                    if (endpointMetrics != null) {
                        endpointMetrics.retry();
                    }
                });
        responseExecutor = config.async
                ? Executors.newThreadPerTaskExecutor(Thread.ofVirtual()
                        .name("sbk-minio-response-" + id + "-", 0).factory())
                : null;
        objectSequence = 0;
        bucketSequence = 0;
        drainBuffer = new byte[MinIO.RESPONSE_BUFFER_BYTES];
        asyncDrainBuffers = config.async ? new ArrayBlockingQueue<>(config.asyncDepth) : null;
        if (asyncDrainBuffers != null) {
            for (int i = 0; i < config.asyncDepth; i++) {
                asyncDrainBuffers.add(new byte[MinIO.RESPONSE_BUFFER_BYTES]);
            }
        }
    }

    @Override
    public void recordRead(DataType<byte[]> dataType, int size, Time time, Status status,
                           PerlChannel channel) throws IOException {
        record(size, time, status, channel, null, false);
    }

    @Override
    public void recordRead(DataType<byte[]> dataType, int size, Time time, Status status,
                           PerlChannel channel, int readerId, ReadRequestsLogger logger) throws IOException {
        record(size, time, status, channel, logger, false);
    }

    @Override
    public void recordReadTime(DataType<byte[]> dataType, int size, Time time, Status status,
                               PerlChannel channel) throws IOException {
        record(size, time, status, channel, null, true);
    }

    @Override
    public void recordReadTime(DataType<byte[]> dataType, int size, Time time, Status status,
                               PerlChannel channel, int readerId, ReadRequestsLogger logger) throws IOException {
        record(size, time, status, channel, logger, true);
    }

    private void record(int size, Time time, Status status, PerlChannel channel,
                        ReadRequestsLogger logger, boolean endToEnd) throws IOException {
        PreparedOperation prepared = prepare(size);
        if (prepared == null) {
            status.records = 0;
            status.bytes = 0;
            status.startTime = time.getCurrentTime();
            status.endTime = status.startTime;
            return;
        }

        if (config.async) {
            try {
                asyncExecutor.acquire();
            } catch (IOException ex) {
                if (S3AsyncExecutor.isCleanShutdown(ex)) {
                    markStopped(status, time);
                    return;
                }
                throw ex;
            }
        }
        long requestStartTime = time.getCurrentTime();
        long measurementStartTime = endToEnd && prepared.createdTime > 0
                ? prepared.createdTime : requestStartTime;
        status.startTime = measurementStartTime;
        status.records = 1;
        status.bytes = prepared.bytes;
        if (logger != null) {
            logger.recordReadRequests(id, requestStartTime, prepared.bytes, 1);
        }

        if (!config.async) {
            try {
                int bytes = executeSync(prepared);
                status.bytes = bytes;
                status.endTime = time.getCurrentTime();
                channel.send(status.startTime, status.endTime, 1, bytes);
                recordSuccess(bytes);
            } catch (Exception ex) {
                if (S3AsyncExecutor.isCleanShutdown(ex)) {
                    markStopped(status, time);
                    return;
                }
                recordFailure();
                throw operationFailure(prepared.operation, ex);
            }
            return;
        }

        final long startTime = measurementStartTime;
        try {
            asyncExecutor.track(executeAsync(prepared), (bytes, thrown) -> {
                if (thrown != null && !S3AsyncExecutor.isCleanShutdown(thrown)) {
                    recordFailure();
                    channel.throwException(thrown);
                } else if (thrown == null) {
                    channel.send(startTime, time.getCurrentTime(), 1, bytes);
                    recordSuccess(bytes);
                }
            });
        } catch (Exception ex) {
            asyncExecutor.releaseFailedStart();
            if (S3AsyncExecutor.isCleanShutdown(ex)) {
                markStopped(status, time);
                return;
            }
            recordFailure();
            throw operationFailure(prepared.operation, ex);
        }
        status.endTime = time.getCurrentTime();
    }

    private PreparedOperation prepare(int requestedSize) throws IOException {
        S3Operation selected = operationMix.next();
        if (selected == S3Operation.LIST || selected == S3Operation.BUCKET_LIST) {
            return new PreparedOperation(selected, null, null, 0, 0, 0);
        }
        if (selected == S3Operation.BUCKET_STAT) {
            String bucket = nextBucketTarget();
            return new PreparedOperation(selected, bucket, null, 0, 0, 0);
        }
        S3ObjectRef object;
        if (mixedWorkload) {
            try {
                object = catalog.pollPublished(100, TimeUnit.MILLISECONDS);
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
                throw new IOException("Interrupted while waiting for a completed S3 PUT", ex);
            }
        } else {
            object = selected == S3Operation.RANGE_GET
                    ? catalog.nextForReader(id, readerCount, objectSequence++, config.rangeOffset)
                    : catalog.nextForReader(id, readerCount, objectSequence++);
        }
        if (object == null) {
            return null;
        }
        long bytes = object.size();
        long offset = 0;
        if (selected == S3Operation.RANGE_GET) {
            long requestedLength = config.rangeLength > 0 ? config.rangeLength
                    : (requestedSize > 0 ? requestedSize : configuredSize);
            offset = rangeOffsetSelector.next(object.size(), requestedLength);
            bytes = Math.max(0, Math.min(requestedLength, object.size() - offset));
        }
        return new PreparedOperation(selected, object.key(), object.versionId(), offset,
                safeBytes(bytes), object.createdTime());
    }

    private int executeSync(PreparedOperation prepared) throws Exception {
        return retryPolicy.execute(() -> executeSyncOnce(prepared));
    }

    private int executeSyncOnce(PreparedOperation prepared) throws Exception {
        int result = switch (prepared.operation) {
            case GET, RANGE_GET -> {
                try (GetObjectResponse response = client.getObject(getArgs(prepared).build())) {
                    yield drain(response);
                }
            }
            case STAT -> {
                client.statObject(statArgs(prepared).build());
                yield 0;
            }
            case TAG_GET -> {
                client.getObjectTags(getTagArgs(prepared).build());
                yield 0;
            }
            case LIST -> listObjects();
            case BUCKET_STAT -> {
                requireExistingBucket(prepared.key, client.bucketExists(
                        BucketExistsArgs.builder().bucket(prepared.key).build()));
                yield 0;
            }
            case BUCKET_LIST -> {
                client.listBuckets();
                yield 0;
            }
            default -> throw new IllegalStateException("Unsupported reader operation "
                    + prepared.operation);
        };
        verifyBytes(prepared, result);
        return result;
    }

    private CompletableFuture<Integer> executeAsync(PreparedOperation prepared) throws Exception {
        return retryPolicy.executeAsync(() -> executeAsyncOnce(prepared));
    }

    private CompletableFuture<Integer> executeAsyncOnce(
            PreparedOperation prepared) throws Exception {
        CompletableFuture<Integer> result = switch (prepared.operation) {
            case GET, RANGE_GET -> asyncClient.getObject(getArgs(prepared).build())
                    .thenApplyAsync(response -> {
                        try (response) {
                            return drain(response);
                        } catch (IOException ex) {
                            throw new S3CompletionException(ex);
                        }
                    }, responseExecutor);
            case STAT -> asyncClient.statObject(statArgs(prepared).build()).thenApply(ignored -> 0);
            case TAG_GET -> asyncClient.getObjectTags(getTagArgs(prepared).build()).thenApply(ignored -> 0);
            case LIST -> CompletableFuture.supplyAsync(() -> {
                try {
                    return listObjects();
                } catch (Exception ex) {
                    throw new S3CompletionException(ex);
                }
            }, responseExecutor);
            case BUCKET_STAT -> asyncClient.bucketExists(
                    BucketExistsArgs.builder().bucket(prepared.key).build()).thenApply(exists -> {
                        requireExistingBucket(prepared.key, exists);
                        return 0;
                    });
            case BUCKET_LIST -> asyncClient.listBuckets().thenApply(ignored -> 0);
            default -> throw new IllegalStateException("Unsupported reader operation "
                    + prepared.operation);
        };
        return config.verifyReadSize
                ? result.thenApply(bytes -> {
                    verifyBytes(prepared, bytes);
                    return bytes;
                }) : result;
    }

    private GetObjectArgs.Builder getArgs(PreparedOperation prepared) {
        GetObjectArgs.Builder builder = GetObjectArgs.builder()
                .bucket(config.bucketName).object(prepared.key);
        if (prepared.versionId != null && !prepared.versionId.isEmpty()) {
            builder.versionId(prepared.versionId);
        }
        if (prepared.operation == S3Operation.RANGE_GET) {
            builder.offset(prepared.offset).length((long) prepared.bytes);
        }
        return builder;
    }

    private StatObjectArgs.Builder statArgs(PreparedOperation prepared) {
        StatObjectArgs.Builder builder = StatObjectArgs.builder()
                .bucket(config.bucketName).object(prepared.key);
        if (prepared.versionId != null && !prepared.versionId.isEmpty()) {
            builder.versionId(prepared.versionId);
        }
        return builder;
    }

    private GetObjectTagsArgs.Builder getTagArgs(PreparedOperation prepared) {
        GetObjectTagsArgs.Builder builder = GetObjectTagsArgs.builder()
                .bucket(config.bucketName).object(prepared.key);
        if (prepared.versionId != null && !prepared.versionId.isEmpty()) {
            builder.versionId(prepared.versionId);
        }
        return builder;
    }

    private int listObjects() throws Exception {
        int count = 0;
        Iterable<Result<Item>> results = config.async
                ? asyncClient.listObjects(listArgs) : client.listObjects(listArgs);
        for (Result<Item> result : results) {
            result.get();
            if (++count >= config.listMaxEntries) {
                break;
            }
        }
        return 0;
    }

    private ListObjectsArgs buildListArgs() {
        ListObjectsArgs.Builder builder = ListObjectsArgs.builder()
                .bucket(config.bucketName)
                .maxKeys(config.listMaxKeys)
                .includeVersions(config.versioningEnabled)
                .useApiVersion1(config.listApiVersion == 1);
        if (config.listApiVersion == 2) {
            builder.fetchOwner(config.listFetchOwner)
                    .includeUserMetadata(config.listIncludeUserMetadata);
        }
        String selectedPrefix = listPrefixes.isEmpty()
                ? config.prefix : listPrefixes.get(Math.floorMod(id, listPrefixes.size()));
        if (selectedPrefix != null && !selectedPrefix.isEmpty()) {
            builder.prefix(selectedPrefix);
        }
        if (config.listDelimiter == null || config.listDelimiter.isEmpty()) {
            builder.recursive(true);
        } else {
            builder.delimiter(config.listDelimiter);
        }
        if (config.listStartAfter != null && !config.listStartAfter.isEmpty()) {
            if (config.listApiVersion == 1) {
                builder.marker(config.listStartAfter);
            } else {
                builder.startAfter(config.listStartAfter);
            }
        }
        return builder.build();
    }

    private int drain(GetObjectResponse response) throws IOException {
        byte[] buffer = drainBuffer;
        if (asyncDrainBuffers != null) {
            try {
                buffer = asyncDrainBuffers.take();
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
                throw new IOException("Interrupted while waiting for an S3 response buffer", ex);
            }
        }
        try {
            long bytes = 0;
            int count;
            while ((count = response.read(buffer)) > 0) {
                bytes += count;
            }
            return safeBytes(bytes);
        } finally {
            if (asyncDrainBuffers != null) {
                asyncDrainBuffers.offer(buffer);
            }
        }
    }

    private static void requireExistingBucket(String bucket, boolean exists) {
        if (!exists) {
            throw new S3CompletionException(new IOException(
                    "S3 bucket-stat target '" + bucket + "' does not exist"));
        }
    }

    private String nextBucketTarget() {
        if (bucketTargets.isEmpty()) {
            return config.bucketName;
        }
        long index = id + bucketSequence++ * readerCount;
        return bucketTargets.get((int) Math.floorMod(index, bucketTargets.size()));
    }

    private static List<String> parseList(String csv) {
        if (csv == null || csv.isBlank()) {
            return List.of();
        }
        return MinIO.parseList(csv);
    }

    private IOException operationFailure(S3Operation selected, Exception ex) {
        return new IOException("MinIO " + selected + " operation failed: " + ex.getMessage(), ex);
    }

    private void verifyBytes(PreparedOperation prepared, int actualBytes) {
        if (config.verifyReadSize
                && (prepared.operation == S3Operation.GET
                || prepared.operation == S3Operation.RANGE_GET)
                && actualBytes != prepared.bytes) {
            throw new S3CompletionException(new IOException("S3 " + prepared.operation
                    + " response length " + actualBytes + " does not match expected "
                    + prepared.bytes + " bytes for '" + prepared.key + "'"));
        }
    }

    private void recordSuccess(int bytes) {
        if (endpointMetrics != null) {
            endpointMetrics.success(bytes);
        }
    }

    private void recordFailure() {
        if (endpointMetrics != null) {
            endpointMetrics.failure();
        }
    }

    private static void markStopped(Status status, Time time) {
        status.records = 0;
        status.bytes = 0;
        status.endTime = time.getCurrentTime();
    }

    private static int safeBytes(long bytes) {
        return (int) Math.max(0, Math.min(Integer.MAX_VALUE, bytes));
    }

    @Override
    public byte[] read() {
        return null;
    }

    @Override
    public void close() throws IOException {
        if (asyncExecutor != null) {
            asyncExecutor.await();
        }
        if (responseExecutor != null) {
            responseExecutor.shutdown();
        }
    }

    private record PreparedOperation(S3Operation operation, String key, String versionId,
                                     long offset, int bytes, long createdTime) {
    }

    private static final class S3CompletionException extends RuntimeException {
        S3CompletionException(Throwable cause) {
            super(cause);
        }
    }
}
