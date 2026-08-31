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

import io.minio.CopyObjectArgs;
import io.minio.CopySource;
import io.minio.DeleteObjectTagsArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioAsyncClient;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.RemoveBucketArgs;
import io.minio.RemoveObjectArgs;
import io.minio.ServerSideEncryption;
import io.minio.ServerSideEncryptionS3;
import io.minio.SetObjectTagsArgs;
import io.perl.api.PerlChannel;
import io.sbk.api.Status;
import io.sbk.api.Writer;
import io.sbk.data.DataType;
import io.sbk.logger.WriteRequestsLogger;
import io.sbk.params.ParameterOptions;
import io.time.Time;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Semaphore;

/**
 * Per-worker MinIO SDK writer for mutating S3 operations.
 *
 * <p>Synchronous mode reuses one payload buffer per worker. Asynchronous mode
 * retains one payload per in-flight request and applies bounded backpressure
 * through {@link S3AsyncExecutor}.
 */
public class MinIOWriter implements Writer<byte[]> {
    private final int id;
    private final int writerCount;
    private final MinIOConfig config;
    private final S3Operation operation;
    private final S3OperationMix operationMix;
    private final MinioClient client;
    private final MinioAsyncClient asyncClient;
    private final S3ObjectCatalog catalog;
    private final List<String> bucketTargets;
    private final Queue<String> createdBuckets;
    private final S3DataGenerator dataGenerator;
    private final S3ChecksumUtil.Algorithm checksumAlgorithm;
    private final S3ObjectKey keyGenerator;
    private final S3ObjectSizeSelector sizeSelector;
    private final S3BucketName bucketNameGenerator;
    private final Map<String, String> objectTags;
    private final ServerSideEncryption sse;
    private final S3AsyncExecutor asyncExecutor;
    private final S3RetryPolicy retryPolicy;
    private final S3MultipartUploader multipartUploader;
    private final S3PayloadPool payloadPool;
    private final S3EndpointMetrics endpointMetrics;
    private long copySequence;
    private long bucketTargetSequence;
    private byte[] reusablePayload;

    /**
     * Create an S3 writer worker.
     *
     * @param id writer id
     * @param params parsed SBK parameters
     * @param config MinIO configuration
     * @param operation writer operation
     * @param client synchronous MinIO client
     * @param asyncClient asynchronous MinIO client, or {@code null}
     * @param catalog shared object catalog
     * @param bucketTargets explicit bucket targets
     * @param createdBuckets generated buckets to clean up
     * @param runToken run discriminator
     * @param globalAsyncPermits shared process-wide async permits
     * @param endpointMetrics optional endpoint attribution
     * @throws IllegalArgumentException when tagging is enabled without tags
     */
    public MinIOWriter(int id, ParameterOptions params, MinIOConfig config, S3Operation operation,
                       MinioClient client, MinioAsyncClient asyncClient, S3ObjectCatalog catalog,
                       List<String> bucketTargets, Queue<String> createdBuckets, String runToken,
                       Semaphore globalAsyncPermits, S3EndpointMetrics endpointMetrics) {
        this.id = id;
        writerCount = Math.max(1, params.getWritersCount());
        this.config = config;
        this.operation = operation;
        this.client = client;
        this.asyncClient = asyncClient;
        this.catalog = catalog;
        this.bucketTargets = bucketTargets;
        this.createdBuckets = createdBuckets;
        this.endpointMetrics = endpointMetrics;
        long seed = config.dataSeed == 0 ? System.nanoTime() : config.dataSeed + id;
        dataGenerator = new S3DataGenerator(config.dataCompressibility, config.dataDedupable, seed);
        operationMix = S3OperationMix.parse(config.writeMix, operation, true, id);
        checksumAlgorithm = S3ChecksumUtil.Algorithm.fromString(config.checksumAlgorithm);
        keyGenerator = new S3ObjectKey(config, id, runToken);
        sizeSelector = S3ObjectSizeSelector.parse(config.objectSizeDistribution, id);
        bucketNameGenerator = new S3BucketName(config.bucketPrefix, runToken, id);
        objectTags = parseTags(config.taggingTags);
        sse = config.sseEnabled ? new ServerSideEncryptionS3() : null;
        retryPolicy = new S3RetryPolicy(config.retryMaxAttempts, config.retryBackoffMs,
                () -> {
                    if (endpointMetrics != null) {
                        endpointMetrics.retry();
                    }
                });
        asyncExecutor = config.async
                ? new S3AsyncExecutor(config.asyncDepth, globalAsyncPermits) : null;
        multipartUploader = config.mpuConcurrentParts > 1
                ? new S3MultipartUploader(asyncClient, config.bucketName, effectiveRegion(config),
                config.partSize, config.mpuConcurrentParts, retryPolicy) : null;
        payloadPool = config.async ? new S3PayloadPool() : null;
        copySequence = 0;
        bucketTargetSequence = 0;
        reusablePayload = null;
        if ((operation == S3Operation.TAG_SET || config.taggingEnabled) && objectTags.isEmpty()) {
            throw new IllegalArgumentException("S3 tagging requires non-empty -tagging-tags");
        }
    }

    @Override
    public void recordWrite(DataType<byte[]> dataType, byte[] data, int size, Time time,
                            Status status, PerlChannel channel) throws IOException {
        record(dataType, size, time, status, channel, null);
    }

    @Override
    public void recordWrite(DataType<byte[]> dataType, byte[] data, int size, Time time,
                            Status status, PerlChannel channel, int writerId,
                            WriteRequestsLogger logger) throws IOException {
        record(dataType, size, time, status, channel, logger);
    }

    private void record(DataType<byte[]> dataType, int size, Time time, Status status,
                        PerlChannel channel, WriteRequestsLogger logger) throws IOException {
        boolean asyncSlotAcquired = acquireAsyncSlot(status, time);
        if (config.async && !asyncSlotAcquired) {
            return;
        }
        final PreparedOperation prepared;
        try {
            prepared = prepare(size);
        } catch (RuntimeException ex) {
            releaseFailedAsyncStart();
            throw ex;
        }
        if (prepared == null) {
            releaseFailedAsyncStart();
            status.records = 0;
            status.bytes = 0;
            status.startTime = time.getCurrentTime();
            status.endTime = status.startTime;
            idleBriefly();
            return;
        }

        status.startTime = time.getCurrentTime();
        status.bytes = prepared.bytes;
        status.records = 1;
        if (logger != null) {
            logger.recordWriteRequests(id, status.startTime, prepared.bytes, 1);
        }

        if (!config.async) {
            try {
                OperationResult result = executeSync(prepared);
                status.endTime = time.getCurrentTime();
                channel.send(status.startTime, status.endTime, 1, result.bytes);
                publish(result, status.startTime);
                recordSuccess(result.bytes);
            } catch (Exception ex) {
                if (S3AsyncExecutor.isCleanShutdown(ex)) {
                    markStopped(status, time);
                    return;
                }
                recordFailure();
                throw operationFailure(ex);
            }
            return;
        }

        final long startTime = status.startTime;
        try {
            asyncExecutor.track(executeAsync(prepared), (result, thrown) -> {
                try {
                    if (thrown != null && !S3AsyncExecutor.isCleanShutdown(thrown)) {
                        recordFailure();
                        channel.throwException(thrown);
                    } else if (thrown == null) {
                        long endTime = time.getCurrentTime();
                        channel.send(startTime, endTime, 1, result.bytes);
                        publish(result, startTime);
                        recordSuccess(result.bytes);
                    }
                } finally {
                    releasePayload(prepared);
                }
            });
        } catch (Exception ex) {
            asyncExecutor.releaseFailedStart();
            releasePayload(prepared);
            if (S3AsyncExecutor.isCleanShutdown(ex)) {
                markStopped(status, time);
                return;
            }
            recordFailure();
            throw operationFailure(ex);
        }
        status.endTime = time.getCurrentTime();
    }

    private PreparedOperation prepare(int size) {
        S3Operation selected = operationMix.next();
        int selectedSize = selected == S3Operation.PUT || selected == S3Operation.UPDATE
                ? sizeSelector.next(size) : size;
        return switch (selected) {
            case PUT -> new PreparedOperation(selected, nextPayload(selectedSize),
                    keyGenerator.next(), null, null, selectedSize);
            case UPDATE -> objectOperation(selected, nextPayload(selectedSize), catalog.nextShared(),
                    selectedSize);
            case COPY -> copyOperation(catalog.nextShared());
            case DELETE, TAG_SET, TAG_DELETE -> objectOperation(selected, null,
                    selected == S3Operation.DELETE ? catalog.claimDelete() : catalog.nextShared(), 0);
            case BUCKET_CREATE -> new PreparedOperation(selected, null,
                    bucketNameGenerator.next(), null, null, 0);
            case BUCKET_DELETE -> {
                String bucket = nextBucketTarget();
                yield bucket == null ? null
                        : new PreparedOperation(selected, null, bucket, null, null, 0);
            }
            default -> throw new IllegalStateException("Unsupported writer operation " + selected);
        };
    }

    private PreparedOperation objectOperation(S3Operation selected, byte[] payload,
                                              S3ObjectRef object, long bytes) {
        if (object == null) {
            return null;
        }
        int effectiveBytes = safeBytes(bytes < 0 ? object.size() : bytes);
        return new PreparedOperation(selected, payload, object.key(),
                object.versionId(), null, effectiveBytes);
    }

    private PreparedOperation copyOperation(S3ObjectRef object) {
        if (object == null) {
            return null;
        }
        return new PreparedOperation(S3Operation.COPY, null, object.key(), object.versionId(),
                copyDestination(), safeBytes(object.size()));
    }

    private byte[] nextPayload(int size) {
        if (config.async) {
            return payloadPool.acquire(size, dataGenerator);
        }
        dataGenerator.newObject();
        if (reusablePayload == null || reusablePayload.length != size) {
            reusablePayload = new byte[size];
        }
        dataGenerator.fill(reusablePayload, 0, reusablePayload.length);
        return reusablePayload;
    }

    private OperationResult executeSync(PreparedOperation prepared) throws Exception {
        if (usesConcurrentMultipart(prepared)) {
            return executeSyncOnce(prepared);
        }
        return retryPolicy.execute(() -> executeSyncOnce(prepared));
    }

    private OperationResult executeSyncOnce(PreparedOperation prepared) throws Exception {
        return switch (prepared.operation) {
            case PUT, UPDATE -> {
                if (usesConcurrentMultipart(prepared)) {
                    multipartUploader.upload(prepared.key, prepared.payload,
                            putArgs(prepared).build().genHeaders()).get();
                } else {
                    client.putObject(putArgs(prepared).build());
                }
                yield new OperationResult(prepared.operation, prepared.key,
                        prepared.versionId, prepared.bytes);
            }
            case COPY -> {
                client.copyObject(copyArgs(prepared, prepared.destination).build());
                yield new OperationResult(prepared.operation, prepared.destination, null,
                        prepared.bytes);
            }
            case DELETE -> {
                client.removeObject(removeArgs(prepared).build());
                yield new OperationResult(prepared.operation, null, null, 0);
            }
            case TAG_SET -> {
                client.setObjectTags(tagArgs(prepared).build());
                yield new OperationResult(prepared.operation, null, null, 0);
            }
            case TAG_DELETE -> {
                client.deleteObjectTags(deleteTagArgs(prepared).build());
                yield new OperationResult(prepared.operation, null, null, 0);
            }
            case BUCKET_CREATE -> {
                client.makeBucket(makeBucketArgs(prepared.key).build());
                createdBuckets.offer(prepared.key);
                yield new OperationResult(prepared.operation, null, null, 0);
            }
            case BUCKET_DELETE -> {
                client.removeBucket(RemoveBucketArgs.builder().bucket(prepared.key).build());
                yield new OperationResult(prepared.operation, null, null, 0);
            }
            default -> throw new IllegalStateException("Unsupported writer operation "
                    + prepared.operation);
        };
    }

    private CompletableFuture<OperationResult> executeAsync(PreparedOperation prepared) throws Exception {
        if (usesConcurrentMultipart(prepared)) {
            return executeAsyncOnce(prepared);
        }
        return retryPolicy.executeAsync(() -> executeAsyncOnce(prepared));
    }

    private CompletableFuture<OperationResult> executeAsyncOnce(
            PreparedOperation prepared) throws Exception {
        return switch (prepared.operation) {
            case PUT, UPDATE -> {
                CompletableFuture<?> upload = usesConcurrentMultipart(prepared)
                        ? multipartUploader.upload(prepared.key, prepared.payload,
                        putArgs(prepared).build().genHeaders())
                        : asyncClient.putObject(putArgs(prepared).build());
                yield upload.thenApply(ignored ->
                        new OperationResult(prepared.operation, prepared.key,
                                prepared.versionId, prepared.bytes));
            }
            case COPY -> {
                yield asyncClient.copyObject(copyArgs(prepared, prepared.destination).build())
                        .thenApply(ignored -> new OperationResult(prepared.operation,
                                prepared.destination, null, prepared.bytes));
            }
            case DELETE -> asyncClient.removeObject(removeArgs(prepared).build())
                    .thenApply(ignored -> new OperationResult(prepared.operation, null, null, 0));
            case TAG_SET -> asyncClient.setObjectTags(tagArgs(prepared).build())
                    .thenApply(ignored -> new OperationResult(prepared.operation, null, null, 0));
            case TAG_DELETE -> asyncClient.deleteObjectTags(deleteTagArgs(prepared).build())
                    .thenApply(ignored -> new OperationResult(prepared.operation, null, null, 0));
            case BUCKET_CREATE -> asyncClient.makeBucket(makeBucketArgs(prepared.key).build())
                    .thenApply(ignored -> {
                        createdBuckets.offer(prepared.key);
                        return new OperationResult(prepared.operation, null, null, 0);
                    });
            case BUCKET_DELETE -> asyncClient.removeBucket(
                            RemoveBucketArgs.builder().bucket(prepared.key).build())
                    .thenApply(ignored -> new OperationResult(prepared.operation, null, null, 0));
            default -> throw new IllegalStateException("Unsupported writer operation "
                    + prepared.operation);
        };
    }

    private PutObjectArgs.Builder putArgs(PreparedOperation prepared) {
        PutObjectArgs.Builder builder = PutObjectArgs.builder()
                .bucket(config.bucketName)
                .object(prepared.key)
                .stream(new ByteArrayInputStream(prepared.payload), (long) prepared.payload.length,
                        config.partSize > 0 ? config.partSize : -1L);
        if (sse != null) {
            builder.sse(sse);
        }
        if (config.taggingEnabled) {
            builder.tags(objectTags);
        }
        if (checksumAlgorithm != null) {
            builder.headers(Map.of(checksumAlgorithm.headerName,
                    S3ChecksumUtil.computeBase64(prepared.payload, checksumAlgorithm)));
        }
        return builder;
    }

    private boolean usesConcurrentMultipart(PreparedOperation prepared) {
        return multipartUploader != null && prepared.payload != null
                && prepared.payload.length > config.partSize;
    }

    private CopyObjectArgs.Builder copyArgs(PreparedOperation prepared, String destination) {
        CopySource.Builder source = CopySource.builder()
                .bucket(config.bucketName)
                .object(prepared.key);
        if (prepared.versionId != null && !prepared.versionId.isEmpty()) {
            source.versionId(prepared.versionId);
        }
        CopyObjectArgs.Builder builder = CopyObjectArgs.builder()
                .bucket(config.bucketName)
                .object(destination)
                .source(source.build());
        if (sse != null) {
            builder.sse(sse);
        }
        return builder;
    }

    private RemoveObjectArgs.Builder removeArgs(PreparedOperation prepared) {
        RemoveObjectArgs.Builder builder = RemoveObjectArgs.builder()
                .bucket(config.bucketName)
                .object(prepared.key);
        if (prepared.versionId != null && !prepared.versionId.isEmpty()) {
            builder.versionId(prepared.versionId);
        }
        return builder;
    }

    private SetObjectTagsArgs.Builder tagArgs(PreparedOperation prepared) {
        SetObjectTagsArgs.Builder builder = SetObjectTagsArgs.builder()
                .bucket(config.bucketName).object(prepared.key).tags(objectTags);
        if (prepared.versionId != null && !prepared.versionId.isEmpty()) {
            builder.versionId(prepared.versionId);
        }
        return builder;
    }

    private DeleteObjectTagsArgs.Builder deleteTagArgs(PreparedOperation prepared) {
        DeleteObjectTagsArgs.Builder builder = DeleteObjectTagsArgs.builder()
                .bucket(config.bucketName).object(prepared.key);
        if (prepared.versionId != null && !prepared.versionId.isEmpty()) {
            builder.versionId(prepared.versionId);
        }
        return builder;
    }

    private MakeBucketArgs.Builder makeBucketArgs(String bucket) {
        MakeBucketArgs.Builder builder = MakeBucketArgs.builder().bucket(bucket);
        if (config.region != null && !config.region.isEmpty()) {
            builder.region(config.region);
        }
        return builder;
    }

    private String copyDestination() {
        String prefix = config.copyPrefix == null ? "" : config.copyPrefix.trim();
        String separator = prefix.isEmpty() || prefix.endsWith("/") ? "" : "/";
        return prefix + separator + "copy-" + id + "-"
                + Long.toUnsignedString(++copySequence, 36);
    }

    private String nextBucketTarget() {
        long position = id + bucketTargetSequence++ * writerCount;
        return position < bucketTargets.size() ? bucketTargets.get((int) position) : null;
    }

    private void publish(OperationResult result, long createdTime) {
        if (result.key != null
                && (result.operation == S3Operation.PUT || result.operation == S3Operation.COPY)) {
            catalog.publish(new S3ObjectRef(result.key, result.versionId, result.bytes, createdTime));
        }
    }

    private static void idleBriefly() throws IOException {
        try {
            Thread.sleep(1);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new IOException("Interrupted while waiting for another S3 operation target", ex);
        }
    }

    private static void markStopped(Status status, Time time) {
        status.records = 0;
        status.bytes = 0;
        status.endTime = time.getCurrentTime();
    }

    private boolean acquireAsyncSlot(Status status, Time time) throws IOException {
        if (!config.async) {
            return false;
        }
        try {
            asyncExecutor.acquire();
            return true;
        } catch (IOException ex) {
            if (S3AsyncExecutor.isCleanShutdown(ex)) {
                markStopped(status, time);
                return false;
            }
            throw ex;
        }
    }

    private void releaseFailedAsyncStart() {
        if (asyncExecutor != null) {
            asyncExecutor.releaseFailedStart();
        }
    }

    private void releasePayload(PreparedOperation prepared) {
        if (payloadPool != null) {
            payloadPool.release(prepared.payload);
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

    private static String effectiveRegion(MinIOConfig config) {
        return config.region == null || config.region.isEmpty() ? "us-east-1" : config.region;
    }

    private static int safeBytes(long bytes) {
        return (int) Math.max(0, Math.min(Integer.MAX_VALUE, bytes));
    }

    private IOException operationFailure(Exception ex) {
        return new IOException("MinIO " + operation + " operation failed: " + ex.getMessage(), ex);
    }

    private static Map<String, String> parseTags(String csv) {
        Map<String, String> tags = new LinkedHashMap<>();
        if (csv == null || csv.isBlank()) {
            return tags;
        }
        for (String pair : csv.split(",")) {
            String[] keyValue = pair.split("=", 2);
            if (keyValue.length == 2 && !keyValue[0].trim().isEmpty()) {
                tags.put(keyValue[0].trim(), keyValue[1].trim());
            }
        }
        return tags;
    }

    @Override
    public CompletableFuture<?> writeAsync(byte[] data) throws IOException {
        try {
            if (config.async) {
                asyncExecutor.acquire();
                PreparedOperation prepared = prepare(data.length);
                if (prepared == null) {
                    asyncExecutor.releaseFailedStart();
                    return null;
                }
                try {
                    return asyncExecutor.track(executeAsync(prepared)
                            .whenComplete((result, thrown) -> {
                                releasePayload(prepared);
                                if (thrown == null) {
                                    recordSuccess(result.bytes);
                                } else if (!S3AsyncExecutor.isCleanShutdown(thrown)) {
                                    recordFailure();
                                }
                            }));
                } catch (Exception ex) {
                    asyncExecutor.releaseFailedStart();
                    releasePayload(prepared);
                    throw ex;
                }
            }
            PreparedOperation prepared = prepare(data.length);
            if (prepared == null) {
                return null;
            }
            OperationResult result = executeSync(prepared);
            recordSuccess(result.bytes);
            return null;
        } catch (Exception ex) {
            if (S3AsyncExecutor.isCleanShutdown(ex)) {
                return null;
            }
            recordFailure();
            throw operationFailure(ex);
        }
    }

    @Override
    public void sync() throws IOException {
        if (asyncExecutor != null) {
            asyncExecutor.await();
        }
    }

    @Override
    public void close() throws IOException {
        sync();
    }

    private record PreparedOperation(S3Operation operation, byte[] payload, String key,
                                     String versionId, String destination, int bytes) {
    }

    private record OperationResult(S3Operation operation, String key, String versionId, int bytes) {
    }
}
