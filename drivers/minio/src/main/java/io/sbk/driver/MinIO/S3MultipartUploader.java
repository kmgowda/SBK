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

import com.google.common.collect.Multimap;
import io.minio.MinioAsyncClient;
import io.minio.ObjectWriteResponse;
import io.minio.UploadPartResponse;
import io.minio.messages.Part;

import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;

/**
 * Executes one multipart upload with a bounded number of concurrent parts.
 *
 * <p>Parts are submitted in bounded waves so a large object cannot create an
 * unbounded future graph or SDK buffer set. Failed parts are retried without
 * restarting successful parts, and a terminal failure aborts the upload.
 */
final class S3MultipartUploader {
    private static final int MAX_PART_COUNT = 10_000;

    private final MinioAsyncClient client;
    private final String bucket;
    private final String region;
    private final int partSize;
    private final int concurrentParts;
    private final S3RetryPolicy retryPolicy;

    S3MultipartUploader(MinioAsyncClient client, String bucket, String region, long partSize,
                        int concurrentParts, S3RetryPolicy retryPolicy) {
        this.client = client;
        this.bucket = bucket;
        this.region = region;
        this.partSize = Math.toIntExact(partSize);
        this.concurrentParts = concurrentParts;
        this.retryPolicy = retryPolicy;
    }

    CompletableFuture<ObjectWriteResponse> upload(String object, byte[] payload,
                                                   Multimap<String, String> headers) {
        int partCount = Math.toIntExact((payload.length + (long) partSize - 1) / partSize);
        if (partCount < 2 || partCount > MAX_PART_COUNT) {
            return CompletableFuture.failedFuture(new IllegalArgumentException(
                    "Concurrent multipart upload requires 2.." + MAX_PART_COUNT
                            + " parts; computed " + partCount));
        }
        Part[] completedParts = new Part[partCount];
        return future(() -> client.createMultipartUploadAsync(bucket, region, object, headers, null))
                .thenCompose(created -> {
                    String uploadId = created.result().uploadId();
                    return uploadWave(object, payload, uploadId, completedParts, 0)
                            .thenCompose(ignored -> future(() ->
                                    client.completeMultipartUploadAsync(bucket, region, object,
                                            uploadId, completedParts, null, null)))
                            .handle((response, thrown) -> thrown == null
                                    ? CompletableFuture.completedFuture(response)
                                    : abort(object, uploadId, thrown))
                            .thenCompose(future -> future);
                });
    }

    private CompletableFuture<Void> uploadWave(String object, byte[] payload, String uploadId,
                                               Part[] completedParts, int firstPartIndex) {
        if (firstPartIndex >= completedParts.length) {
            return CompletableFuture.completedFuture(null);
        }
        int waveEnd = Math.min(completedParts.length, firstPartIndex + concurrentParts);
        List<CompletableFuture<?>> futures = new ArrayList<>(waveEnd - firstPartIndex);
        for (int partIndex = firstPartIndex; partIndex < waveEnd; partIndex++) {
            int offset = partIndex * partSize;
            int length = Math.min(partSize, payload.length - offset);
            int partNumber = partIndex + 1;
            int completedPartIndex = partIndex;
            CompletableFuture<UploadPartResponse> future = retryPolicy.executeAsync(() ->
                    client.uploadPartAsync(bucket, region, object,
                            new ByteArrayInputStream(payload, offset, length), length, uploadId,
                            partNumber, null, null));
            futures.add(future.thenAccept(response -> completedParts[completedPartIndex] =
                    new Part(partNumber, response.etag())));
        }
        return CompletableFuture.allOf(futures.toArray(CompletableFuture[]::new))
                .thenCompose(ignored -> uploadWave(object, payload, uploadId, completedParts,
                        waveEnd));
    }

    private CompletableFuture<ObjectWriteResponse> abort(String object, String uploadId,
                                                          Throwable failure) {
        Throwable original = unwrap(failure);
        return future(() -> client.abortMultipartUploadAsync(
                        bucket, region, object, uploadId, null, null))
                .handle((ignored, abortFailure) -> {
                    if (abortFailure != null) {
                        original.addSuppressed(unwrap(abortFailure));
                    }
                    throw new CompletionException(original);
                });
    }

    private static <T> CompletableFuture<T> future(
            S3RetryPolicy.ThrowingSupplier<CompletableFuture<T>> supplier) {
        try {
            return supplier.get();
        } catch (Exception ex) {
            return CompletableFuture.failedFuture(ex);
        }
    }

    private static Throwable unwrap(Throwable thrown) {
        Throwable cause = thrown;
        while (cause instanceof CompletionException && cause.getCause() != null) {
            cause = cause.getCause();
        }
        return cause;
    }
}
