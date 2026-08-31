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

import com.google.common.collect.HashMultimap;
import io.minio.CreateMultipartUploadResponse;
import io.minio.MinioAsyncClient;
import io.minio.ObjectWriteResponse;
import io.minio.UploadPartResponse;
import io.minio.messages.InitiateMultipartUploadResult;
import io.minio.messages.Part;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** Tests bounded multipart upload orchestration at the MinIO SDK boundary. */
public class S3MultipartUploaderTest {

    @Test
    public void uploadsPartsInBoundedWavesAndCompletesInPartOrder() throws Exception {
        MinioAsyncClient client = mock(MinioAsyncClient.class);
        stubCreate(client, "upload-1");
        List<CompletableFuture<UploadPartResponse>> pending = new ArrayList<>();
        when(client.uploadPartAsync(eq("bucket"), eq("us-east-1"), eq("object"),
                any(), anyLong(), eq("upload-1"), anyInt(), isNull(), isNull()))
                .thenAnswer(invocation -> {
                    CompletableFuture<UploadPartResponse> future = new CompletableFuture<>();
                    pending.add(future);
                    return future;
                });
        ObjectWriteResponse completed = mock(ObjectWriteResponse.class);
        when(client.completeMultipartUploadAsync(eq("bucket"), eq("us-east-1"),
                eq("object"), eq("upload-1"), any(Part[].class), isNull(), isNull()))
                .thenReturn(CompletableFuture.completedFuture(completed));
        S3MultipartUploader uploader = new S3MultipartUploader(client, "bucket", "us-east-1",
                5, 2, new S3RetryPolicy(1, 0));

        CompletableFuture<ObjectWriteResponse> upload = uploader.upload(
                "object", new byte[11], HashMultimap.create());
        verify(client, times(2)).uploadPartAsync(eq("bucket"), eq("us-east-1"),
                eq("object"), any(), anyLong(), eq("upload-1"), anyInt(), isNull(), isNull());
        pending.get(0).complete(part(1));
        pending.get(1).complete(part(2));
        verify(client, times(3)).uploadPartAsync(eq("bucket"), eq("us-east-1"),
                eq("object"), any(), anyLong(), eq("upload-1"), anyInt(), isNull(), isNull());
        pending.get(2).complete(part(3));

        assertSame(completed, upload.join());
        ArgumentCaptor<Part[]> parts = ArgumentCaptor.forClass(Part[].class);
        verify(client).completeMultipartUploadAsync(eq("bucket"), eq("us-east-1"),
                eq("object"), eq("upload-1"), parts.capture(), isNull(), isNull());
        assertEquals(List.of(1, 2, 3),
                java.util.Arrays.stream(parts.getValue()).map(Part::partNumber).toList());
        verify(client, never()).abortMultipartUploadAsync(any(), any(), any(), any(), any(), any());
    }

    @Test
    public void abortsMultipartUploadWhenAPartFails() throws Exception {
        MinioAsyncClient client = mock(MinioAsyncClient.class);
        stubCreate(client, "upload-2");
        IOException failure = new IOException("part failed");
        when(client.uploadPartAsync(any(), any(), any(), any(), anyLong(), any(), anyInt(),
                any(), any())).thenReturn(CompletableFuture.failedFuture(failure));
        when(client.abortMultipartUploadAsync(any(), any(), any(), eq("upload-2"), any(), any()))
                .thenReturn(CompletableFuture.completedFuture(null));
        S3MultipartUploader uploader = new S3MultipartUploader(client, "bucket", "us-east-1",
                5, 2, new S3RetryPolicy(1, 0));

        CompletionException thrown = assertThrows(CompletionException.class,
                () -> uploader.upload("object", new byte[10], HashMultimap.create()).join());

        assertSame(failure, thrown.getCause());
        verify(client).abortMultipartUploadAsync(
                eq("bucket"), eq("us-east-1"), eq("object"), eq("upload-2"), isNull(), isNull());
        verify(client, never()).completeMultipartUploadAsync(
                any(), any(), any(), any(), any(), any(), any());
    }

    @Test
    public void retriesOnlyTheFailedPartBeforeCompletingTheUpload() throws Exception {
        MinioAsyncClient client = mock(MinioAsyncClient.class);
        stubCreate(client, "upload-3");
        AtomicInteger firstPartAttempts = new AtomicInteger();
        when(client.uploadPartAsync(any(), any(), any(), any(), anyLong(), eq("upload-3"),
                anyInt(), any(), any())).thenAnswer(invocation -> {
                    int partNumber = invocation.getArgument(6);
                    if (partNumber == 1 && firstPartAttempts.getAndIncrement() == 0) {
                        return CompletableFuture.failedFuture(new IOException("retry part one"));
                    }
                    return CompletableFuture.completedFuture(part(partNumber));
                });
        when(client.completeMultipartUploadAsync(any(), any(), any(), eq("upload-3"),
                any(Part[].class), any(), any()))
                .thenReturn(CompletableFuture.completedFuture(mock(ObjectWriteResponse.class)));
        S3MultipartUploader uploader = new S3MultipartUploader(client, "bucket", "us-east-1",
                5, 2, new S3RetryPolicy(2, 0));

        uploader.upload("object", new byte[10], HashMultimap.create()).join();

        verify(client, times(3)).uploadPartAsync(any(), any(), any(), any(), anyLong(),
                eq("upload-3"), anyInt(), any(), any());
        verify(client, times(1)).completeMultipartUploadAsync(any(), any(), any(),
                eq("upload-3"), any(Part[].class), any(), any());
        verify(client, never()).abortMultipartUploadAsync(any(), any(), any(), any(), any(), any());
    }

    @Test
    public void payloadPoolReusesOnlyMatchingBuffers() {
        S3PayloadPool pool = new S3PayloadPool();
        S3DataGenerator generator = new S3DataGenerator(100, true, 1);
        byte[] first = pool.acquire(32, generator);
        pool.release(first);

        assertSame(first, pool.acquire(32, generator));
        pool.release(first);
        byte[] resized = pool.acquire(64, generator);
        assertEquals(64, resized.length);
        assertEquals(0, pool.retainedCount());
        pool.release(first);
        pool.release(resized);
        assertSame(resized, pool.acquire(64, generator));
        assertEquals(1, pool.retainedCount());
    }

    private static void stubCreate(MinioAsyncClient client, String uploadId) throws Exception {
        CreateMultipartUploadResponse response = mock(CreateMultipartUploadResponse.class);
        InitiateMultipartUploadResult result = mock(InitiateMultipartUploadResult.class);
        when(result.uploadId()).thenReturn(uploadId);
        when(response.result()).thenReturn(result);
        when(client.createMultipartUploadAsync(any(), any(), any(), any(), any()))
                .thenReturn(CompletableFuture.completedFuture(response));
    }

    private static UploadPartResponse part(int number) {
        UploadPartResponse response = mock(UploadPartResponse.class);
        when(response.etag()).thenReturn("etag-" + number);
        return response;
    }
}
