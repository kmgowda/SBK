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

import io.minio.GetObjectArgs;
import io.minio.GetObjectResponse;
import io.minio.ListObjectsArgs;
import io.minio.MinioClient;
import io.minio.Result;
import io.minio.StatObjectArgs;
import io.minio.StatObjectResponse;
import io.minio.messages.Item;
import io.perl.api.PerlChannel;
import io.sbk.api.Reader;
import io.sbk.api.Status;
import io.sbk.data.DataType;
import io.sbk.params.ParameterOptions;
import io.time.Time;

import java.io.IOException;
import java.io.InterruptedIOException;
import java.util.concurrent.RejectedExecutionException;

/**
 * Per-thread reader that performs S3 {@code GetObject} (with optional
 * {@code versionId}) for every object reachable under the configured
 * bucket/prefix. Listing is recursive so the fsAccess key layout works
 * naturally.
 */
public class MinIOReader implements Reader<byte[]> {

    @SuppressWarnings("unused")
    private final ParameterOptions params;
    private final MinIOConfig config;
    private final MinioClient client;

    public MinIOReader(int id, ParameterOptions params, MinIOConfig config, MinioClient client) {
        this.params = params;
        this.config = config;
        this.client = client;
    }

    private ListObjectsArgs.Builder listBuilder() {
        ListObjectsArgs.Builder b = ListObjectsArgs.builder()
                .bucket(config.bucketName)
                .recursive(true)
                .includeVersions(config.versioningEnabled);
        if (config.prefix != null && !config.prefix.isEmpty()) {
            b.prefix(config.prefix);
        }
        return b;
    }

    @Override
    public void recordRead(DataType<byte[]> dType, int size, Time time,
                           Status status, PerlChannel perlChannel) throws IOException {
        Iterable<Result<Item>> results = client.listObjects(listBuilder().build());
        try {
            for (Result<Item> r : results) {
                if (Thread.currentThread().isInterrupted()) {
                    return;
                }
                Item it = r.get();
                String key = it.objectName();
                String versionId = config.versioningEnabled ? it.versionId() : null;

                status.startTime = time.getCurrentTime();
                StatObjectArgs.Builder stb = StatObjectArgs.builder()
                        .bucket(config.bucketName).object(key);
                if (versionId != null && !versionId.isEmpty()) {
                    stb.versionId(versionId);
                }
                StatObjectResponse stat = client.statObject(stb.build());
                status.bytes = (int) stat.size();

                GetObjectArgs.Builder gb = GetObjectArgs.builder()
                        .bucket(config.bucketName).object(key);
                if (versionId != null && !versionId.isEmpty()) {
                    gb.versionId(versionId);
                }
                try (GetObjectResponse in = client.getObject(gb.build())) {
                    drain(in);
                }
                status.endTime = time.getCurrentTime();
                perlChannel.send(status.startTime, status.endTime, 1, status.bytes);
            }
        } catch (InterruptedIOException | RejectedExecutionException shutdown) {
            // Benchmark window ended; the SDK's HTTP dispatcher has been
            // terminated underneath us. Treat as a clean stop.
            return;
        } catch (Exception ex) {
            throw new IOException(ex);
        }
    }

    @Override
    public void recordReadTime(DataType<byte[]> dType, int size, Time time,
                               Status status, PerlChannel perlChannel) throws IOException {
        Iterable<Result<Item>> results = client.listObjects(listBuilder().build());
        try {
            for (Result<Item> r : results) {
                if (Thread.currentThread().isInterrupted()) {
                    return;
                }
                Item it = r.get();
                String key = it.objectName();
                String versionId = config.versioningEnabled ? it.versionId() : null;

                status.startTime = time.getCurrentTime();
                StatObjectArgs.Builder stb = StatObjectArgs.builder()
                        .bucket(config.bucketName).object(key);
                if (versionId != null && !versionId.isEmpty()) {
                    stb.versionId(versionId);
                }
                StatObjectResponse stat = client.statObject(stb.build());
                status.bytes = (int) stat.size();

                byte[] inData = new byte[status.bytes];
                GetObjectArgs.Builder gb = GetObjectArgs.builder()
                        .bucket(config.bucketName).object(key);
                if (versionId != null && !versionId.isEmpty()) {
                    gb.versionId(versionId);
                }

                int read = 0;
                try (GetObjectResponse in = client.getObject(gb.build())) {
                    int n;
                    while (read < inData.length
                            && (n = in.read(inData, read, inData.length - read)) > 0) {
                        read += n;
                    }
                }
                if (read > 0) {
                    status.endTime = dType.getTime(inData);
                    perlChannel.send(status.startTime, status.endTime, 1, status.bytes);
                }
            }
        } catch (InterruptedIOException | RejectedExecutionException shutdown) {
            // Benchmark window ended; SDK dispatcher terminated. Clean stop.
            return;
        } catch (Exception ex) {
            throw new IOException(ex);
        }
    }

    private static void drain(GetObjectResponse in) throws IOException {
        byte[] buf = new byte[64 * 1024];
        while (in.read(buf) > 0) {
            // discard payload; we only care about the round-trip latency
            continue;
        }
    }

    @Override
    public byte[] read() throws IOException {
        return null;
    }

    @Override
    public void close() throws IOException {
        // No per-reader resources.
    }
}
