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

import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.ServerSideEncryption;
import io.minio.ServerSideEncryptionS3;
import io.minio.SetObjectTagsArgs;
import io.minio.errors.ErrorResponseException;
import io.minio.errors.InvalidResponseException;
import io.perl.api.PerlChannel;
import io.sbk.api.Status;
import io.sbk.api.Writer;
import io.sbk.data.DataType;
import io.sbk.params.ParameterOptions;
import io.time.Time;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InterruptedIOException;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.RejectedExecutionException;

/**
 * Per-thread writer that performs S3 {@code PutObject} (or multipart
 * upload) calls. Implements all SPT-equivalent features:
 * <ul>
 *   <li>SDK-driven multipart upload via
 *       {@link PutObjectArgs.Builder#stream} when {@code part-size}
 *       is configured (the SDK chunks and parallelises the parts).</li>
 *   <li>S3 checksum header (CRC32 / CRC32C / SHA1 / SHA256 / CRC64-NVMe).</li>
 *   <li>Object tagging (issued after the PUT so backends that reject
 *       {@code x-amz-tagging} on PUT still work).</li>
 *   <li>Data-shaping: compressibility and anti-dedup stamping.</li>
 *   <li>SSE-S3 encryption.</li>
 * </ul>
 */
public class MinIOWriter implements Writer<byte[]> {

    private final MinIOConfig config;
    private final MinioClient client;
    private final S3DataGenerator dataGen;
    private final S3ChecksumUtil.Algorithm checksumAlgo;
    private final S3ObjectKey keyGen;
    private final Map<String, String> objectTags;
    private final ServerSideEncryption sse;

    public MinIOWriter(int id, ParameterOptions params, MinIOConfig config,
                       MinioClient client, DataType<byte[]> dType) {
        this.config = config;
        this.client = client;
        this.dataGen = new S3DataGenerator(config.dataCompressibility, config.dataDedupable);
        this.checksumAlgo = S3ChecksumUtil.Algorithm.fromString(config.checksumAlgorithm);
        this.keyGen = new S3ObjectKey(config);
        this.objectTags = parseTags(config.taggingTags);
        this.sse = config.sseEnabled ? new ServerSideEncryptionS3() : null;
    }

    private static Map<String, String> parseTags(String csv) {
        Map<String, String> out = new LinkedHashMap<>();
        if (csv == null || csv.isEmpty()) {
            return out;
        }
        for (String pair : csv.split(",")) {
            String[] kv = pair.split("=", 2);
            if (kv.length == 2) {
                String k = kv[0].trim();
                String v = kv[1].trim();
                if (!k.isEmpty()) {
                    out.put(k, v);
                }
            }
        }
        return out;
    }

    @Override
    public void recordWrite(DataType<byte[]> dType, byte[] data, int size, Time time,
                            Status status, PerlChannel record) throws IOException {
        // Generate the payload that will actually be sent over the wire.
        // We don't use `data` from the caller because we apply
        // compressibility / anti-dedup shaping here, matching SPT semantics.
        dataGen.newObject();
        byte[] payload = dataGen.generate(size);

        status.startTime = time.getCurrentTime();
        String objectName = doPut(payload);
        status.endTime = time.getCurrentTime();

        if (config.taggingEnabled && !objectTags.isEmpty()) {
            setTags(objectName);
        }

        status.bytes = size;
        status.records = 1;
        record.send(status.startTime, status.endTime, 1, size);
    }

    @Override
    public CompletableFuture<?> writeAsync(byte[] data) throws IOException {
        dataGen.newObject();
        byte[] payload = dataGen.generate(data.length);
        String objectName = doPut(payload);
        if (config.taggingEnabled && !objectTags.isEmpty()) {
            setTags(objectName);
        }
        return null;
    }

    /**
     * Build the put-object args and execute.
     *
     * @param payload bytes to upload
     * @return generated object key
     * @throws IOException when the SDK call fails
     */
    private String doPut(byte[] payload) throws IOException {
        String objectName = keyGen.next();
        PutObjectArgs.Builder b = PutObjectArgs.builder()
                .bucket(config.bucketName)
                .object(objectName)
                .stream(new ByteArrayInputStream(payload), (long) payload.length,
                        config.partSize > 0 ? config.partSize : -1L);

        // MinIO 8.5.x manages multipart parallelism internally based on
        // partSize. There is no public knob for explicit concurrent-parts;
        // the config value is accepted for forward compatibility but ignored
        // by this SDK version.
        if (sse != null) {
            b.sse(sse);
        }

        Map<String, String> headers = new HashMap<>();
        if (checksumAlgo != null) {
            String base64 = S3ChecksumUtil.computeBase64(payload, checksumAlgo);
            if (base64 != null) {
                headers.put(checksumAlgo.headerName, base64);
            }
        }
        if (!headers.isEmpty()) {
            b.headers(headers);
        }

        try {
            client.putObject(b.build());
        } catch (InterruptedIOException | RejectedExecutionException shutdown) {
            // Benchmark window ended mid-PUT; SDK HTTP dispatcher terminated.
            // Surface as IOException but with a clean, recognizable message.
            throw new IOException("PutObject interrupted (benchmark shutdown)", shutdown);
        } catch (Exception e) {
            throw new IOException("PutObject failed for " + objectName + ": " + explain(e), e);
        }
        return objectName;
    }

    private void setTags(String objectName) throws IOException {
        try {
            client.setObjectTags(SetObjectTagsArgs.builder()
                    .bucket(config.bucketName)
                    .object(objectName)
                    .tags(objectTags)
                    .build());
        } catch (Exception e) {
            throw new IOException("setObjectTags failed for " + objectName + ": " + explain(e), e);
        }
    }

    /**
     * Produce a one-line diagnostic from an SDK exception. Surfaces the HTTP
     * status code and response body when present so that classic mis-configs
     * (wrong endpoint, wrong port, non-S3 service on the target IP) are
     * obvious without trawling the stack trace.
     *
     * @param e exception thrown by the MinIO SDK
     * @return short description suitable for logging
     */
    private static String explain(Exception e) {
        if (e instanceof InvalidResponseException) {
            // SDK message already contains: response code, Content-Type, body.
            // Add a hint about the most common cause.
            return e.getMessage()
                    + " -- HINT: the endpoint is likely not an S3 service, or"
                    + " you are pointed at the wrong host/port.";
        }
        if (e instanceof ErrorResponseException ere) {
            return "S3 error " + ere.errorResponse().code()
                    + " (HTTP " + ere.response().code() + "): "
                    + ere.errorResponse().message();
        }
        return e.getClass().getSimpleName() + ": " + e.getMessage();
    }

    @Override
    public void sync() throws IOException {
        // S3 PutObject is durable by the time it returns; no-op.
    }

    @Override
    public void close() throws IOException {
        // No per-writer resources to release.
    }
}
