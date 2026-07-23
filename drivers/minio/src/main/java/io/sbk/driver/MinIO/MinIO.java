/**
 * Copyright (c) KMG. All Rights Reserved..
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 */
package io.sbk.driver.MinIO;

import io.minio.BucketExistsArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioAsyncClient;
import io.minio.MinioClient;
import io.minio.RemoveBucketArgs;
import io.minio.RemoveObjectArgs;
import io.minio.SetBucketVersioningArgs;
import io.minio.ListObjectsArgs;
import io.minio.Result;
import io.minio.errors.ErrorResponseException;
import io.minio.errors.InvalidResponseException;
import io.minio.messages.Item;
import io.minio.messages.VersioningConfiguration;
import io.sbk.api.DataReader;
import io.sbk.api.DataWriter;
import io.sbk.api.Storage;
import io.sbk.data.DataType;
import io.sbk.data.impl.ByteArray;
import io.sbk.params.InputOptions;
import io.sbk.params.ParameterOptions;
import io.sbk.system.Printer;
import okhttp3.ConnectionPool;
import okhttp3.Dispatcher;
import okhttp3.Interceptor;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.dataformat.javaprop.JavaPropsFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

/**
 * SBK driver for any S3-compatible object store (MinIO, AWS S3, Dell ECS,
 * Ceph RGW, SeaweedFS, etc).
 *
 * <p>The driver uses the MinIO Java SDK 8.5.17 and supports object, bucket,
 * synchronous, and bounded asynchronous workloads together with multipart
 * upload, S3 checksums, object tagging, bucket versioning, fsAccess-style
 * keys, data shaping, SSE-S3 encryption, and HTTP-client tuning.
 */
public class MinIO implements Storage<byte[]> {

    private static final String CONFIGFILE = "minio.properties";
    private static final long MIN_PART_SIZE = 5L * 1024 * 1024;            // 5 MiB
    private static final long MAX_PART_SIZE = 5L * 1024 * 1024 * 1024;     // 5 GiB
    private static final int MAX_ASYNC_DEPTH = 1024;

    private MinIOConfig config;
    private MinioClient mclient;
    private MinioAsyncClient asyncClient;
    private List<MinioClient> clients = Collections.emptyList();
    private List<MinioAsyncClient> asyncClients = Collections.emptyList();
    private DataType<byte[]> dType;
    private S3Operation writeOperation;
    private S3Operation readOperation;
    private S3OperationMix configuredWriteMix;
    private S3OperationMix configuredReadMix;
    private S3ObjectCatalog objectCatalog;
    private List<String> bucketTargets;
    private final Queue<String> createdBuckets = new ConcurrentLinkedQueue<>();
    private String runToken;
    private Semaphore globalAsyncPermits;

    public String getConfigFile() {
        return CONFIGFILE;
    }

    @Override
    public void addArgs(final InputOptions params) throws IllegalArgumentException {
        final ObjectMapper mapper = new ObjectMapper(new JavaPropsFactory());
        try {
            config = mapper.readValue(
                    Objects.requireNonNull(MinIO.class.getClassLoader().getResourceAsStream(getConfigFile())),
                    MinIOConfig.class);
        } catch (Exception ex) {
            ex.printStackTrace();
            throw new IllegalArgumentException(ex);
        }

        // Connection
        params.addOption("url",      true, "S3 endpoint URL, default: " + config.url);
        params.addOption("endpoints", true, "Comma-separated S3 endpoints distributed across workers,"
                + " default: '" + nullToEmpty(config.endpoints) + "'");
        params.addOption("bucket",   true, "Bucket name, default: " + config.bucketName);
        params.addOption("key",      true, "Access key, default: " + config.accessKey);
        params.addOption("secret",   true, "Secret key (value is never printed), default: "
                + (nullToEmpty(config.secretKey).isEmpty() ? "not configured" : "configured"));
        params.addOption("region",   true, "AWS region (SigV4), default: '" + nullToEmpty(config.region) + "'");
        params.addOption("recreate", true, "Recreate bucket if present, default: " + config.reCreate);
        params.addOption("insecure", true, "Skip TLS cert validation, default: " + config.insecure);

        // Workload
        params.addOption("write-operation", true, "Writer S3 operation [put|update|copy|delete|tag-set"
                + "|tag-delete|bucket-create|bucket-delete], default: " + config.writeOperation);
        params.addOption("read-operation", true, "Reader S3 operation [get|range-get|stat|tag-get|list"
                + "|bucket-stat|bucket-list], default: " + config.readOperation);
        params.addOption("write-mix", true,
                "Weighted writer operations, e.g. put=80,copy=20, default: '"
                        + nullToEmpty(config.writeMix) + "'");
        params.addOption("read-mix", true,
                "Weighted reader operations, e.g. get=90,stat=10, default: '"
                        + nullToEmpty(config.readMix) + "'");
        params.addOption("async", true, "Use bounded MinioAsyncClient operations, default: " + config.async);
        params.addOption("async-depth", true, "Maximum in-flight operations per worker, default: "
                + config.asyncDepth);
        params.addOption("async-max-inflight", true,
                "Process-wide maximum in-flight operations (0=auto), default: "
                        + config.asyncMaxInflight);
        params.addOption("async-max-memory-mb", true,
                "Maximum estimated async buffer memory MiB (0=off), default: "
                        + config.asyncMaxMemoryMb);

        // Object layout
        params.addOption("fs-access", true, "fs-style key layout, default: " + config.fsAccess);
        params.addOption("prefix",    true, "Object key prefix, default: '" + nullToEmpty(config.prefix) + "'");
        params.addOption("copy-prefix", true, "Destination prefix for COPY, default: '"
                + nullToEmpty(config.copyPrefix) + "'");
        params.addOption("range-offset", true, "Ranged GET byte offset, default: " + config.rangeOffset);
        params.addOption("range-length", true, "Ranged GET length (0 uses -size), default: "
                + config.rangeLength);
        params.addOption("list-max-keys", true, "Maximum objects consumed by each LIST operation, default: "
                + config.listMaxKeys);
        params.addOption("list-prefixes", true,
                "Comma-separated LIST prefixes assigned across readers, default: '"
                        + nullToEmpty(config.listPrefixes) + "'");
        params.addOption("object-file", true,
                "Local CSV object manifest key,size[,versionId], default: '"
                        + nullToEmpty(config.objectFile) + "'");
        params.addOption("catalog-max-objects", true,
                "Maximum startup object references retained, default: "
                        + config.catalogMaxObjects);

        // Multipart
        params.addOption("part-size",            true, "Multipart part size in bytes (0=disabled, min 5MiB), default: " + config.partSize);
        params.addOption("mpu-concurrent-parts", true, "Concurrent parts per object (0=SDK default), default: " + config.mpuConcurrentParts);

        // Checksum
        params.addOption("checksum", true,
                "S3 checksum algo (crc32|crc32c|sha1|sha256|crc64nvme; empty=off), default: '"
                        + nullToEmpty(config.checksumAlgorithm) + "'");

        // Auth
        params.addOption("auth-version", true, "S3 signature version (2 or 4), default: " + config.authVersion);

        // Tagging
        params.addOption("tagging-enabled", true, "Enable object tagging, default: " + config.taggingEnabled);
        params.addOption("tagging-tags",    true, "CSV key=value tags, default: '" + nullToEmpty(config.taggingTags) + "'");

        // Versioning
        params.addOption("versioning-enabled", true, "Enable bucket versioning, default: " + config.versioningEnabled);

        // Bucket workloads
        params.addOption("bucket-targets", true, "Comma-separated explicit bucket targets, default: '"
                + nullToEmpty(config.bucketTargets) + "'");
        params.addOption("bucket-prefix", true, "Generated bucket prefix for bucket-create, default: '"
                + nullToEmpty(config.bucketPrefix) + "'");
        params.addOption("cleanup-created-buckets", true,
                "Remove buckets generated by bucket-create on shutdown, default: "
                        + config.cleanupCreatedBuckets);

        // Data shaping
        params.addOption("data-compressibility", true, "Compressibility % 0..100, default: " + config.dataCompressibility);
        params.addOption("data-dedupable",       true, "Dedup-friendly (false=anti-dedup stamp), default: " + config.dataDedupable);
        params.addOption("data-seed", true,
                "Reproducible payload seed (0=random), default: " + config.dataSeed);
        params.addOption("verify-read-size", true,
                "Validate GET response length, default: " + config.verifyReadSize);
        params.addOption("retry-max-attempts", true,
                "Total attempts for transient failures, default: " + config.retryMaxAttempts);
        params.addOption("retry-backoff-ms", true,
                "Delay between retry attempts in ms, default: " + config.retryBackoffMs);
        params.addOption("warmup-requests", true,
                "Untimed bucket-existence requests before measurement, default: "
                        + config.warmupRequests);
        params.addOption("partition-count", true,
                "Distributed object partitions, default: " + config.partitionCount);
        params.addOption("partition-index", true,
                "This process partition index, default: " + config.partitionIndex);
        params.addOption("run-manifest", true,
                "Credential-free JSON run manifest path, default: '"
                        + nullToEmpty(config.runManifest) + "'");

        // SSE
        params.addOption("sse-enabled", true, "Enable SSE-S3 server-side encryption, default: " + config.sseEnabled);

        // HTTP timeouts (ms)
        params.addOption("connect-timeout-ms", true, "HTTP connect timeout ms (0=default), default: " + config.connectTimeoutMs);
        params.addOption("read-timeout-ms",    true, "HTTP read timeout ms (0=default), default: " + config.readTimeoutMs);
        params.addOption("write-timeout-ms",   true, "HTTP write timeout ms (0=default), default: " + config.writeTimeoutMs);
        params.addOption("http-max-requests", true, "OkHttp maximum async requests (0=auto), default: "
                + config.httpMaxRequests);
        params.addOption("http-max-requests-per-host", true,
                "OkHttp maximum async requests per host (0=auto), default: "
                        + config.httpMaxRequestsPerHost);
        params.addOption("http-max-idle-connections", true,
                "OkHttp maximum idle connections, default: " + config.httpMaxIdleConnections);
        params.addOption("http-keepalive-seconds", true,
                "OkHttp connection keep-alive seconds, default: " + config.httpKeepAliveSeconds);

        // Extra HTTP headers (Dell ECS / ObjectScale: x-emc-namespace=<ns>)
        params.addOption("extra-headers", true,
                "CSV key=value headers added to every S3 request (e.g. 'x-emc-namespace=ns1'), default: '"
                        + nullToEmpty(config.extraHeaders) + "'");
    }

    @Override
    public void parseArgs(final ParameterOptions params) throws IllegalArgumentException {
        // Connection
        config.url        = params.getOptionValue("url",      config.url);
        config.endpoints = params.getOptionValue("endpoints", nullToEmpty(config.endpoints));
        config.bucketName = params.getOptionValue("bucket",   config.bucketName);
        config.accessKey  = params.getOptionValue("key",      config.accessKey);
        config.secretKey  = params.getOptionValue("secret",   config.secretKey);
        config.region     = params.getOptionValue("region",   nullToEmpty(config.region));
        config.reCreate = Boolean.parseBoolean(
                params.getOptionValue("recreate", String.valueOf(config.reCreate)));
        config.insecure = Boolean.parseBoolean(params.getOptionValue("insecure", String.valueOf(config.insecure)));

        // Workload
        config.writeOperation = params.getOptionValue("write-operation", config.writeOperation);
        config.readOperation = params.getOptionValue("read-operation", config.readOperation);
        config.writeMix = params.getOptionValue("write-mix", nullToEmpty(config.writeMix));
        config.readMix = params.getOptionValue("read-mix", nullToEmpty(config.readMix));
        writeOperation = S3Operation.fromString(config.writeOperation);
        readOperation = S3Operation.fromString(config.readOperation);
        if (!writeOperation.isWriterOperation()) {
            throw new IllegalArgumentException("-write-operation must be a mutating S3 operation; got "
                    + config.writeOperation);
        }
        if (readOperation.isWriterOperation()) {
            throw new IllegalArgumentException("-read-operation must be a read-only S3 operation; got "
                    + config.readOperation);
        }
        configuredWriteMix = S3OperationMix.parse(config.writeMix, writeOperation, true, 0);
        configuredReadMix = S3OperationMix.parse(config.readMix, readOperation, false, 0);
        config.async = Boolean.parseBoolean(params.getOptionValue("async", String.valueOf(config.async)));
        config.asyncDepth = Integer.parseInt(params.getOptionValue("async-depth",
                String.valueOf(config.asyncDepth)));
        if (config.asyncDepth < 1 || config.asyncDepth > MAX_ASYNC_DEPTH) {
            throw new IllegalArgumentException("async-depth must be between 1 and "
                    + MAX_ASYNC_DEPTH);
        }
        config.asyncMaxInflight = Integer.parseInt(params.getOptionValue("async-max-inflight",
                String.valueOf(config.asyncMaxInflight)));
        config.asyncMaxMemoryMb = Long.parseLong(params.getOptionValue("async-max-memory-mb",
                String.valueOf(config.asyncMaxMemoryMb)));
        if (config.asyncMaxInflight < 0 || config.asyncMaxMemoryMb < 0) {
            throw new IllegalArgumentException(
                    "async-max-inflight and async-max-memory-mb must not be negative");
        }

        // Object layout
        config.fsAccess = Boolean.parseBoolean(params.getOptionValue("fs-access", String.valueOf(config.fsAccess)));
        config.prefix   = params.getOptionValue("prefix", nullToEmpty(config.prefix));
        config.copyPrefix = params.getOptionValue("copy-prefix", nullToEmpty(config.copyPrefix));
        config.rangeOffset = Long.parseLong(params.getOptionValue("range-offset",
                String.valueOf(config.rangeOffset)));
        config.rangeLength = Long.parseLong(params.getOptionValue("range-length",
                String.valueOf(config.rangeLength)));
        config.listMaxKeys = Integer.parseInt(params.getOptionValue("list-max-keys",
                String.valueOf(config.listMaxKeys)));
        if (config.rangeOffset < 0 || config.rangeLength < 0) {
            throw new IllegalArgumentException("range-offset and range-length must not be negative");
        }
        if (config.listMaxKeys < 1 || config.listMaxKeys > 1000) {
            throw new IllegalArgumentException("list-max-keys must be between 1 and 1000");
        }
        config.listPrefixes = params.getOptionValue("list-prefixes",
                nullToEmpty(config.listPrefixes));
        config.objectFile = params.getOptionValue("object-file", nullToEmpty(config.objectFile));
        config.catalogMaxObjects = Integer.parseInt(params.getOptionValue("catalog-max-objects",
                String.valueOf(config.catalogMaxObjects)));
        if (config.catalogMaxObjects < 1) {
            throw new IllegalArgumentException("catalog-max-objects must be at least 1");
        }

        // Multipart
        config.partSize          = Long.parseLong(params.getOptionValue("part-size",            String.valueOf(config.partSize)));
        config.mpuConcurrentParts = Integer.parseInt(params.getOptionValue("mpu-concurrent-parts", String.valueOf(config.mpuConcurrentParts)));
        if (config.partSize > 0 && (config.partSize < MIN_PART_SIZE || config.partSize > MAX_PART_SIZE)) {
            throw new IllegalArgumentException(
                    "part-size must be between " + MIN_PART_SIZE + " and " + MAX_PART_SIZE
                            + " bytes; got " + config.partSize);
        }
        if (config.mpuConcurrentParts < 0) {
            throw new IllegalArgumentException("mpu-concurrent-parts must not be negative");
        }

        // Checksum
        config.checksumAlgorithm = params.getOptionValue("checksum", nullToEmpty(config.checksumAlgorithm));
        // Validate (throws IllegalArgumentException for bad input)
        S3ChecksumUtil.Algorithm.fromString(config.checksumAlgorithm);

        // Auth
        config.authVersion = Integer.parseInt(params.getOptionValue("auth-version", String.valueOf(config.authVersion)));
        if (config.authVersion != 2 && config.authVersion != 4) {
            throw new IllegalArgumentException("auth-version must be 2 or 4, got " + config.authVersion);
        }
        if (config.authVersion == 2) {
            Printer.log.warn("SigV2 is not supported by the MinIO Java SDK; falling back to SigV4");
        }

        // Tagging
        config.taggingEnabled = Boolean.parseBoolean(params.getOptionValue("tagging-enabled", String.valueOf(config.taggingEnabled)));
        config.taggingTags    = params.getOptionValue("tagging-tags", nullToEmpty(config.taggingTags));

        // Versioning
        config.versioningEnabled = Boolean.parseBoolean(params.getOptionValue("versioning-enabled", String.valueOf(config.versioningEnabled)));

        // Bucket workloads
        config.bucketTargets = params.getOptionValue("bucket-targets", nullToEmpty(config.bucketTargets));
        config.bucketPrefix = params.getOptionValue("bucket-prefix", nullToEmpty(config.bucketPrefix));
        config.cleanupCreatedBuckets = Boolean.parseBoolean(params.getOptionValue("cleanup-created-buckets",
                String.valueOf(config.cleanupCreatedBuckets)));
        bucketTargets = parseList(config.bucketTargets);
        if (params.getWritersCount() > 0 && writeOperation == S3Operation.BUCKET_DELETE
                && bucketTargets.isEmpty()) {
            throw new IllegalArgumentException("bucket-delete requires explicit -bucket-targets");
        }

        // Data shaping
        config.dataCompressibility = Integer.parseInt(params.getOptionValue("data-compressibility", String.valueOf(config.dataCompressibility)));
        config.dataDedupable       = Boolean.parseBoolean(params.getOptionValue("data-dedupable",       String.valueOf(config.dataDedupable)));
        config.dataSeed = Long.parseLong(params.getOptionValue("data-seed",
                String.valueOf(config.dataSeed)));
        config.verifyReadSize = Boolean.parseBoolean(params.getOptionValue("verify-read-size",
                String.valueOf(config.verifyReadSize)));
        config.retryMaxAttempts = Integer.parseInt(params.getOptionValue("retry-max-attempts",
                String.valueOf(config.retryMaxAttempts)));
        config.retryBackoffMs = Long.parseLong(params.getOptionValue("retry-backoff-ms",
                String.valueOf(config.retryBackoffMs)));
        if (config.retryMaxAttempts < 1 || config.retryBackoffMs < 0) {
            throw new IllegalArgumentException(
                    "retry-max-attempts must be positive and retry-backoff-ms non-negative");
        }
        config.warmupRequests = Integer.parseInt(params.getOptionValue("warmup-requests",
                String.valueOf(config.warmupRequests)));
        if (config.warmupRequests < 0) {
            throw new IllegalArgumentException("warmup-requests must not be negative");
        }
        config.partitionCount = Integer.parseInt(params.getOptionValue("partition-count",
                String.valueOf(config.partitionCount)));
        config.partitionIndex = Integer.parseInt(params.getOptionValue("partition-index",
                String.valueOf(config.partitionIndex)));
        config.runManifest = params.getOptionValue("run-manifest", nullToEmpty(config.runManifest));
        if (config.partitionCount < 1 || config.partitionIndex < 0
                || config.partitionIndex >= config.partitionCount) {
            throw new IllegalArgumentException(
                    "partition-count must be positive and partition-index in [0,count)");
        }
        if (config.dataCompressibility < 0 || config.dataCompressibility > 100) {
            throw new IllegalArgumentException("data-compressibility must be between 0 and 100");
        }

        // SSE
        config.sseEnabled = Boolean.parseBoolean(params.getOptionValue("sse-enabled", String.valueOf(config.sseEnabled)));

        // Timeouts
        config.connectTimeoutMs = Long.parseLong(params.getOptionValue("connect-timeout-ms", String.valueOf(config.connectTimeoutMs)));
        config.readTimeoutMs    = Long.parseLong(params.getOptionValue("read-timeout-ms",    String.valueOf(config.readTimeoutMs)));
        config.writeTimeoutMs   = Long.parseLong(params.getOptionValue("write-timeout-ms",   String.valueOf(config.writeTimeoutMs)));
        config.httpMaxRequests = Integer.parseInt(params.getOptionValue("http-max-requests",
                String.valueOf(config.httpMaxRequests)));
        config.httpMaxRequestsPerHost = Integer.parseInt(params.getOptionValue("http-max-requests-per-host",
                String.valueOf(config.httpMaxRequestsPerHost)));
        config.httpMaxIdleConnections = Integer.parseInt(params.getOptionValue("http-max-idle-connections",
                String.valueOf(config.httpMaxIdleConnections)));
        config.httpKeepAliveSeconds = Long.parseLong(params.getOptionValue("http-keepalive-seconds",
                String.valueOf(config.httpKeepAliveSeconds)));
        if (config.connectTimeoutMs < 0 || config.readTimeoutMs < 0 || config.writeTimeoutMs < 0
                || config.httpMaxRequests < 0 || config.httpMaxRequestsPerHost < 0
                || config.httpMaxIdleConnections < 0 || config.httpKeepAliveSeconds < 1) {
            throw new IllegalArgumentException("HTTP timeouts and request limits must not be negative"
                    + " and keep-alive must be positive");
        }

        // Extra headers
        config.extraHeaders = params.getOptionValue("extra-headers", nullToEmpty(config.extraHeaders));

        runToken = Long.toUnsignedString(System.currentTimeMillis(), 36);
        validateAsyncCapacity(params);
        dType = new ByteArray();
    }

    @Override
    public void openStorage(final ParameterOptions params) throws IOException {
        try {
            List<String> endpoints = configuredEndpoints();
            clients = endpoints.stream().map(endpoint -> buildClient(params, endpoint)).toList();
            mclient = clients.getFirst();
            asyncClients = config.async
                    ? endpoints.stream().map(endpoint -> buildAsyncClient(params, endpoint)).toList()
                    : Collections.emptyList();
            asyncClient = asyncClients.isEmpty() ? null : asyncClients.getFirst();
            logFeatureBanner();

            if (config.insecure) {
                Printer.log.info("Disabling TLS certificate validation");
                for (MinioClient configuredClient : clients) {
                    configuredClient.ignoreCertCheck();
                }
                for (MinioAsyncClient configuredClient : asyncClients) {
                    configuredClient.ignoreCertCheck();
                }
            }

            if (!usesMainBucket(params)) {
                objectCatalog = new S3ObjectCatalog(Collections.emptyList());
                return;
            }

            boolean exists = mclient.bucketExists(
                    BucketExistsArgs.builder().bucket(config.bucketName).build());

            if (exists && config.reCreate && params.getWritersCount() > 0) {
                Printer.log.info("Recreating bucket '" + config.bucketName + "'");
                emptyBucket(config.bucketName);
                mclient.removeBucket(RemoveBucketArgs.builder().bucket(config.bucketName).build());
                exists = false;
            }

            if (!exists) {
                if (params.getWritersCount() < 1) {
                    throw new IOException("Bucket '" + config.bucketName + "' does not exist and no writers configured");
                }
                Printer.log.info("Creating bucket '" + config.bucketName + "'");
                MakeBucketArgs.Builder mkb = MakeBucketArgs.builder().bucket(config.bucketName);
                // Only forward an explicitly-set region; otherwise let the
                // client-level default (us-east-1) be used.
                if (config.region != null && !config.region.isEmpty()) {
                    mkb.region(config.region);
                }
                mclient.makeBucket(mkb.build());
            } else {
                Printer.log.info("Bucket '" + config.bucketName + "' already exists");
            }

            if (config.versioningEnabled) {
                Printer.log.info("Enabling versioning on bucket '" + config.bucketName + "'");
                mclient.setBucketVersioning(
                        SetBucketVersioningArgs.builder()
                                .bucket(config.bucketName)
                                .config(new VersioningConfiguration(
                                        VersioningConfiguration.Status.ENABLED,
                                        /* mfaDelete */ null))
                                .build());
            }
            warmUpConnections();
            objectCatalog = requiresObjectCatalog(params)
                    ? loadObjectCatalog() : new S3ObjectCatalog(Collections.emptyList());
            validateCatalog(params);
            writeRunManifest(params);
        } catch (IOException ioe) {
            throw ioe;
        } catch (Exception ex) {
            throw new IOException("Failed to open S3 storage at " + config.url
                    + " (bucket=" + config.bucketName + "): " + explain(ex), ex);
        }
    }

    private boolean usesMainBucket(ParameterOptions params) {
        boolean writerUsesMainBucket = params.getWritersCount() > 0
                && (writeOperation != S3Operation.BUCKET_CREATE
                && writeOperation != S3Operation.BUCKET_DELETE);
        boolean readerUsesMainBucket = params.getReadersCount() > 0
                && (readOperation != S3Operation.BUCKET_STAT
                && readOperation != S3Operation.BUCKET_LIST);
        return writerUsesMainBucket || readerUsesMainBucket;
    }

    private boolean requiresObjectCatalog(ParameterOptions params) {
        boolean writerNeedsObjects = params.getWritersCount() > 0
                && configuredWriteMix.requiresObjectCatalog();
        boolean mixedPutRead = params.getWritersCount() > 0 && params.getReadersCount() > 0
                && configuredWriteMix.contains(S3Operation.PUT)
                && configuredReadMix.requiresObjectCatalog();
        boolean readerNeedsStartupObjects = params.getReadersCount() > 0
                && configuredReadMix.requiresObjectCatalog() && !mixedPutRead;
        return writerNeedsObjects || readerNeedsStartupObjects;
    }

    private void validateCatalog(ParameterOptions params) throws IOException {
        boolean writerNeedsObjects = params.getWritersCount() > 0
                && configuredWriteMix.requiresObjectCatalog();
        boolean readerNeedsObjects = params.getReadersCount() > 0
                && configuredReadMix.requiresObjectCatalog();
        boolean mixedPutRead = params.getWritersCount() > 0 && params.getReadersCount() > 0
                && configuredWriteMix.contains(S3Operation.PUT)
                && configuredReadMix.requiresObjectCatalog();
        if (objectCatalog.size() == 0 && (writerNeedsObjects || readerNeedsObjects) && !mixedPutRead) {
            throw new IOException("S3 " + (writerNeedsObjects ? writeOperation : readOperation)
                    + " requires existing objects, but bucket '" + config.bucketName + "' is empty");
        }
        if (params.getReadersCount() > 0 && configuredReadMix.contains(S3Operation.RANGE_GET)
                && !mixedPutRead && !objectCatalog.hasObjectLargerThan(config.rangeOffset)) {
            throw new IOException("S3 RANGE_GET requires at least one object larger than range-offset "
                    + config.rangeOffset + ", but no eligible object exists in bucket '"
                    + config.bucketName + "'");
        }
    }

    private S3ObjectCatalog loadObjectCatalog() throws Exception {
        if (config.objectFile != null && !config.objectFile.isBlank()) {
            return loadObjectManifest(Path.of(config.objectFile));
        }
        List<S3ObjectRef> objects = new ArrayList<>();
        ListObjectsArgs.Builder builder = ListObjectsArgs.builder()
                .bucket(config.bucketName)
                .recursive(true)
                .includeVersions(config.versioningEnabled);
        if (config.prefix != null && !config.prefix.isEmpty()) {
            builder.prefix(config.prefix);
        }
        for (Result<Item> result : mclient.listObjects(builder.build())) {
            Item item = result.get();
            if (!item.isDir() && !item.isDeleteMarker()) {
                if (belongsToPartition(item.objectName())) {
                    objects.add(new S3ObjectRef(item.objectName(),
                            config.versioningEnabled ? item.versionId() : null, item.size(), 0));
                }
                if (objects.size() >= config.catalogMaxObjects) {
                    Printer.log.warn("S3 object catalog reached -catalog-max-objects "
                            + config.catalogMaxObjects + "; remaining objects are not retained");
                    break;
                }
            }
        }
        Printer.log.info("Prepared S3 object catalog: " + objects.size() + " objects");
        return new S3ObjectCatalog(objects);
    }

    private void warmUpConnections() throws Exception {
        if (config.warmupRequests == 0) {
            return;
        }
        Printer.log.info("Warming S3 HTTP/TLS connections with " + config.warmupRequests
                + " untimed bucket-existence requests");
        for (int request = 0; request < config.warmupRequests; request++) {
            MinioClient configuredClient = clients.get(request % clients.size());
            configuredClient.bucketExists(
                    BucketExistsArgs.builder().bucket(config.bucketName).build());
        }
    }

    private S3ObjectCatalog loadObjectManifest(Path path) throws IOException {
        List<S3ObjectRef> objects = new ArrayList<>();
        try (var lines = Files.lines(path)) {
            var iterator = lines.iterator();
            int lineNumber = 0;
            while (iterator.hasNext() && objects.size() < config.catalogMaxObjects) {
                String line = iterator.next().trim();
                lineNumber++;
                if (line.isEmpty() || line.startsWith("#")) {
                    continue;
                }
                String[] fields = line.split(",", 3);
                if (fields[0].isBlank()) {
                    throw new IOException("Empty object key in manifest " + path + ":" + lineNumber);
                }
                try {
                    long size = fields.length > 1 ? Long.parseLong(fields[1].trim()) : 0;
                    String version = fields.length > 2 && !fields[2].isBlank()
                            ? fields[2].trim() : null;
                    String key = fields[0].trim();
                    if (belongsToPartition(key)) {
                        objects.add(new S3ObjectRef(key, version, size, 0));
                    }
                } catch (NumberFormatException ex) {
                    throw new IOException("Invalid object size in manifest " + path + ":"
                            + lineNumber, ex);
                }
            }
        }
        Printer.log.info("Prepared S3 object catalog from '" + path + "': "
                + objects.size() + " objects");
        return new S3ObjectCatalog(objects);
    }

    private boolean belongsToPartition(String key) {
        return config.partitionCount == 1
                || Math.floorMod(key.hashCode(), config.partitionCount) == config.partitionIndex;
    }

    private void writeRunManifest(ParameterOptions params) throws IOException {
        if (config.runManifest == null || config.runManifest.isBlank()) {
            return;
        }
        String json = "{\n"
                + "  \"driver\": \"MinIO\",\n"
                + "  \"endpointCount\": " + configuredEndpoints().size() + ",\n"
                + "  \"bucket\": \"" + jsonEscape(config.bucketName) + "\",\n"
                + "  \"writeOperation\": \"" + writeOperation + "\",\n"
                + "  \"readOperation\": \"" + readOperation + "\",\n"
                + "  \"writers\": " + params.getWritersCount() + ",\n"
                + "  \"readers\": " + params.getReadersCount() + ",\n"
                + "  \"recordSize\": " + params.getRecordSize() + ",\n"
                + "  \"async\": " + config.async + ",\n"
                + "  \"partitionCount\": " + config.partitionCount + ",\n"
                + "  \"partitionIndex\": " + config.partitionIndex + "\n"
                + "}\n";
        Path manifest = Path.of(config.runManifest);
        Path parent = manifest.toAbsolutePath().getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }
        Files.writeString(manifest, json);
        Printer.log.info("Wrote credential-free S3 run manifest to " + manifest.toAbsolutePath());
    }

    private static String jsonEscape(String value) {
        return nullToEmpty(value).replace("\\", "\\\\").replace("\"", "\\\"");
    }

    /**
     * Convert an SDK exception into a single, human-readable hint that
     * surfaces HTTP status / body and points the user at common mis-configs.
     *
     * @param e exception from the MinIO SDK
     * @return diagnostic string suitable for logging
     */
    private static String explain(Exception e) {
        if (e instanceof InvalidResponseException) {
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

    private MinioClient buildClient(ParameterOptions params, String endpoint) {
        // Always set a region so the SDK skips GetBucketLocation on openStorage.
        // Many S3-compatible backends (MinIO, Dell ECS, Ceph RGW) return HTML
        // or a non-AWS XML body for GET /?location, which trips the SDK's
        // strict XML parser. AWS S3 itself happily accepts "us-east-1" as
        // the default region for any bucket lookup, so this is a safe default.
        String effectiveRegion = (config.region == null || config.region.isEmpty())
                ? "us-east-1" : config.region;

        MinioClient.Builder mb = MinioClient.builder()
                .endpoint(endpoint)
                .credentials(config.accessKey, config.secretKey)
                .region(effectiveRegion);
        mb.httpClient(buildHttpClient(params), true);
        return mb.build();
    }

    private MinioAsyncClient buildAsyncClient(ParameterOptions params, String endpoint) {
        String effectiveRegion = (config.region == null || config.region.isEmpty())
                ? "us-east-1" : config.region;
        return MinioAsyncClient.builder()
                .endpoint(endpoint)
                .credentials(config.accessKey, config.secretKey)
                .region(effectiveRegion)
                .httpClient(buildHttpClient(params), true)
                .build();
    }

    private OkHttpClient buildHttpClient(ParameterOptions params) {
        int workers = Math.max(1, params.getWritersCount() + params.getReadersCount());
        int derivedLimit = (int) Math.min(Integer.MAX_VALUE,
                Math.max(64L, (long) workers * config.asyncDepth));
        int maxRequests = config.httpMaxRequests > 0 ? config.httpMaxRequests : derivedLimit;
        int maxRequestsPerHost = config.httpMaxRequestsPerHost > 0
                ? config.httpMaxRequestsPerHost : derivedLimit;
        Dispatcher dispatcher = new Dispatcher();
        dispatcher.setMaxRequests(maxRequests);
        dispatcher.setMaxRequestsPerHost(maxRequestsPerHost);

        OkHttpClient.Builder httpB = new OkHttpClient.Builder()
                .dispatcher(dispatcher)
                .connectionPool(new ConnectionPool(config.httpMaxIdleConnections,
                        config.httpKeepAliveSeconds, TimeUnit.SECONDS));
        if (config.connectTimeoutMs > 0) {
            httpB.connectTimeout(config.connectTimeoutMs, TimeUnit.MILLISECONDS);
        }
        if (config.readTimeoutMs > 0) {
            httpB.readTimeout(config.readTimeoutMs, TimeUnit.MILLISECONDS);
        }
        if (config.writeTimeoutMs > 0) {
            httpB.writeTimeout(config.writeTimeoutMs, TimeUnit.MILLISECONDS);
        }
        Map<String, String> extra = parseHeaders(config.extraHeaders);
        if (!extra.isEmpty()) {
            httpB.addInterceptor(new HeaderInjector(extra));
        }
        return httpB.build();
    }

    /**
     * Parse {@code k1=v1,k2=v2} into an ordered map. Whitespace around tokens
     * is trimmed; malformed pairs are silently skipped.
     *
     * @param csv comma-separated {@code key=value} list (may be empty/null)
     * @return ordered map (preserves declaration order)
     */
    private static Map<String, String> parseHeaders(String csv) {
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

    private static List<String> parseList(String csv) {
        if (csv == null || csv.isBlank()) {
            return Collections.emptyList();
        }
        List<String> values = new ArrayList<>();
        for (String value : csv.split(",")) {
            String trimmed = value.trim();
            if (!trimmed.isEmpty()) {
                values.add(trimmed);
            }
        }
        return List.copyOf(values);
    }

    /** OkHttp interceptor that injects a fixed set of headers on every request. */
    private static final class HeaderInjector implements Interceptor {
        private final Map<String, String> headers;

        HeaderInjector(Map<String, String> headers) {
            this.headers = headers;
        }

        @Override
        public Response intercept(Chain chain) throws IOException {
            Request original = chain.request();
            Request.Builder rb = original.newBuilder();
            for (Map.Entry<String, String> e : headers.entrySet()) {
                rb.header(e.getKey(), e.getValue());
            }
            return chain.proceed(rb.build());
        }
    }

    private void logFeatureBanner() {
        Printer.log.info("MinIO/S3 driver features:");
        Printer.log.info("  endpoint        = " + config.url);
        if (clients.size() > 1) {
            Printer.log.info("  endpoints       = " + configuredEndpoints());
        }
        Printer.log.info("  bucket          = " + config.bucketName);
        Printer.log.info("  write operation = " + writeOperation);
        Printer.log.info("  read operation  = " + readOperation);
        Printer.log.info("  client mode     = " + (config.async
                ? "async (depth " + config.asyncDepth + " per worker, "
                + globalAsyncLimit() + " process-wide)" : "synchronous"));
        String shownRegion = nullToEmpty(config.region).isEmpty()
                ? "us-east-1 (default)" : config.region;
        Printer.log.info("  region          = " + shownRegion);
        if (config.fsAccess) {
            Printer.log.info("  fsAccess        = true");
        }
        if (!nullToEmpty(config.prefix).isEmpty()) {
            Printer.log.info("  prefix          = " + config.prefix);
        }
        if (config.partSize > 0) {
            Printer.log.info("  partSize        = " + config.partSize + " B (multipart enabled)");
        }
        if (config.mpuConcurrentParts > 0) {
            Printer.log.info("  mpuConcurrent   = " + config.mpuConcurrentParts
                    + " (info only; not exposed by MinIO SDK 8.5.x)");
        }
        if (!nullToEmpty(config.checksumAlgorithm).isEmpty()) {
            Printer.log.info("  checksum        = " + config.checksumAlgorithm);
        }
        if (config.taggingEnabled) {
            Printer.log.info("  tagging         = " + config.taggingTags);
        }
        if (config.versioningEnabled) {
            Printer.log.info("  versioning      = enabled");
        }
        if (config.dataCompressibility > 0) {
            Printer.log.info("  compressibility = " + config.dataCompressibility + "%");
        }
        if (!config.dataDedupable) {
            Printer.log.info("  anti-dedup      = enabled");
        }
        if (config.sseEnabled) {
            Printer.log.info("  sse             = SSE-S3");
        }
        Map<String, String> hdrs = parseHeaders(config.extraHeaders);
        if (!hdrs.isEmpty()) {
            Printer.log.info("  extra-headers   = " + hdrs.keySet());
        }
    }

    private void emptyBucket(String bucket) throws Exception {
        Iterable<Result<Item>> results = mclient.listObjects(
                ListObjectsArgs.builder().bucket(bucket).recursive(true).includeVersions(true).build());
        for (Result<Item> r : results) {
            Item it = r.get();
            RemoveObjectArgs.Builder builder = RemoveObjectArgs.builder()
                    .bucket(bucket).object(it.objectName());
            if (it.versionId() != null && !it.versionId().isEmpty()) {
                builder.versionId(it.versionId());
            }
            mclient.removeObject(builder.build());
        }
    }

    @Override
    public void closeStorage(final ParameterOptions params) throws IOException {
        IOException closeFailure = null;
        if (config.cleanupCreatedBuckets && mclient != null) {
            String bucket;
            while ((bucket = createdBuckets.poll()) != null) {
                try {
                    mclient.removeBucket(RemoveBucketArgs.builder().bucket(bucket).build());
                } catch (Exception ex) {
                    if (closeFailure == null) {
                        closeFailure = new IOException("Unable to remove generated benchmark bucket '"
                                + bucket + "'", ex);
                    }
                }
            }
        }
        for (MinioAsyncClient configuredClient : asyncClients) {
            try {
                configuredClient.close();
            } catch (Exception ex) {
                if (closeFailure == null) {
                    closeFailure = new IOException("Unable to close MinioAsyncClient", ex);
                }
            }
        }
        for (MinioClient configuredClient : clients) {
            try {
                configuredClient.close();
            } catch (Exception ex) {
                if (closeFailure == null) {
                    closeFailure = new IOException("Unable to close MinioClient", ex);
                }
            }
        }
        if (closeFailure != null) {
            throw closeFailure;
        }
    }

    @Override
    public DataWriter<byte[]> createWriter(final int id, final ParameterOptions params) {
        return new MinIOWriter(id, params, config, writeOperation,
                clients.get(Math.floorMod(id, clients.size())),
                asyncClients.isEmpty() ? null : asyncClients.get(Math.floorMod(id, asyncClients.size())),
                objectCatalog, bucketTargets, createdBuckets, runToken, globalAsyncPermits);
    }

    @Override
    public DataReader<byte[]> createReader(final int id, final ParameterOptions params) {
        return new MinIOReader(id, params, config, readOperation,
                clients.get(Math.floorMod(id, clients.size())),
                asyncClients.isEmpty() ? null : asyncClients.get(Math.floorMod(id, asyncClients.size())),
                objectCatalog, bucketTargets, globalAsyncPermits);
    }

    @Override
    public DataType<byte[]> getDataType() throws IllegalArgumentException {
        return dType;
    }

    private static String nullToEmpty(String s) {
        return s == null ? "" : s;
    }

    private List<String> configuredEndpoints() {
        List<String> values = parseList(config.endpoints);
        return values.isEmpty() ? List.of(config.url) : values;
    }

    private int globalAsyncLimit() {
        return globalAsyncPermits == null ? 0 : config.asyncMaxInflight;
    }

    private void validateAsyncCapacity(ParameterOptions params) {
        if (!config.async) {
            globalAsyncPermits = null;
            return;
        }
        int workers = Math.max(1, params.getWritersCount() + params.getReadersCount());
        long derived = (long) workers * config.asyncDepth;
        int limit = config.asyncMaxInflight > 0 ? config.asyncMaxInflight
                : (int) Math.min(Integer.MAX_VALUE, derived);
        config.asyncMaxInflight = limit;
        long largestBuffer = Math.max(params.getRecordSize(), 64L * 1024);
        long estimatedBytes = Math.multiplyExact((long) limit, largestBuffer);
        if (config.asyncMaxMemoryMb > 0
                && estimatedBytes > config.asyncMaxMemoryMb * 1024L * 1024L) {
            throw new IllegalArgumentException("Estimated async S3 buffers require "
                    + ((estimatedBytes + 1024 * 1024 - 1) / (1024 * 1024))
                    + " MiB, exceeding -async-max-memory-mb " + config.asyncMaxMemoryMb
                    + "; reduce -async-depth/-async-max-inflight or object size");
        }
        globalAsyncPermits = new Semaphore(limit);
    }
}
