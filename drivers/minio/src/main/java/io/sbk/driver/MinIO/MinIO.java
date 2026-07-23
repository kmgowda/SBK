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
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
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
    private DataType<byte[]> dType;
    private S3Operation writeOperation;
    private S3Operation readOperation;
    private S3ObjectCatalog objectCatalog;
    private List<String> bucketTargets;
    private final Queue<String> createdBuckets = new ConcurrentLinkedQueue<>();
    private String runToken;

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
        params.addOption("bucket",   true, "Bucket name, default: " + config.bucketName);
        params.addOption("key",      true, "Access key, default: " + config.accessKey);
        params.addOption("secret",   true, "Secret key, default: " + config.secretKey);
        params.addOption("region",   true, "AWS region (SigV4), default: '" + nullToEmpty(config.region) + "'");
        params.addOption("recreate", true, "Recreate bucket if present, default: " + config.reCreate);
        params.addOption("insecure", true, "Skip TLS cert validation, default: " + config.insecure);

        // Workload
        params.addOption("write-operation", true, "Writer S3 operation [put|update|copy|delete|tag-set"
                + "|tag-delete|bucket-create|bucket-delete], default: " + config.writeOperation);
        params.addOption("read-operation", true, "Reader S3 operation [get|range-get|stat|tag-get|list"
                + "|bucket-stat|bucket-list], default: " + config.readOperation);
        params.addOption("async", true, "Use bounded MinioAsyncClient operations, default: " + config.async);
        params.addOption("async-depth", true, "Maximum in-flight operations per worker, default: "
                + config.asyncDepth);

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
        config.bucketName = params.getOptionValue("bucket",   config.bucketName);
        config.accessKey  = params.getOptionValue("key",      config.accessKey);
        config.secretKey  = params.getOptionValue("secret",   config.secretKey);
        config.region     = params.getOptionValue("region",   nullToEmpty(config.region));
        if (params.getWritersCount() > 0 && params.getReadersCount() > 0) {
            config.reCreate = true;
        } else {
            config.reCreate = Boolean.parseBoolean(params.getOptionValue("recreate", String.valueOf(config.reCreate)));
        }
        config.insecure = Boolean.parseBoolean(params.getOptionValue("insecure", String.valueOf(config.insecure)));

        // Workload
        config.writeOperation = params.getOptionValue("write-operation", config.writeOperation);
        config.readOperation = params.getOptionValue("read-operation", config.readOperation);
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
        config.async = Boolean.parseBoolean(params.getOptionValue("async", String.valueOf(config.async)));
        config.asyncDepth = Integer.parseInt(params.getOptionValue("async-depth",
                String.valueOf(config.asyncDepth)));
        if (config.asyncDepth < 1 || config.asyncDepth > MAX_ASYNC_DEPTH) {
            throw new IllegalArgumentException("async-depth must be between 1 and "
                    + MAX_ASYNC_DEPTH);
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
        dType = new ByteArray();
    }

    @Override
    public void openStorage(final ParameterOptions params) throws IOException {
        try {
            mclient = buildClient(params);
            if (config.async) {
                asyncClient = buildAsyncClient(params);
            }
            logFeatureBanner();

            if (config.insecure) {
                Printer.log.info("Disabling TLS certificate validation");
                mclient.ignoreCertCheck();
                if (asyncClient != null) {
                    asyncClient.ignoreCertCheck();
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
            objectCatalog = loadObjectCatalog();
            validateCatalog(params);
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

    private void validateCatalog(ParameterOptions params) throws IOException {
        boolean writerNeedsObjects = params.getWritersCount() > 0 && writeOperation.requiresObjectCatalog();
        boolean readerNeedsObjects = params.getReadersCount() > 0 && readOperation.requiresObjectCatalog();
        boolean mixedPutRead = params.getWritersCount() > 0 && params.getReadersCount() > 0
                && writeOperation == S3Operation.PUT && readOperation.requiresObjectCatalog();
        if (objectCatalog.size() == 0 && (writerNeedsObjects || readerNeedsObjects) && !mixedPutRead) {
            throw new IOException("S3 " + (writerNeedsObjects ? writeOperation : readOperation)
                    + " requires existing objects, but bucket '" + config.bucketName + "' is empty");
        }
    }

    private S3ObjectCatalog loadObjectCatalog() throws Exception {
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
                objects.add(new S3ObjectRef(item.objectName(),
                        config.versioningEnabled ? item.versionId() : null, item.size(), 0));
            }
        }
        Printer.log.info("Prepared S3 object catalog: " + objects.size() + " objects");
        return new S3ObjectCatalog(objects);
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

    private MinioClient buildClient(ParameterOptions params) {
        // Always set a region so the SDK skips GetBucketLocation on openStorage.
        // Many S3-compatible backends (MinIO, Dell ECS, Ceph RGW) return HTML
        // or a non-AWS XML body for GET /?location, which trips the SDK's
        // strict XML parser. AWS S3 itself happily accepts "us-east-1" as
        // the default region for any bucket lookup, so this is a safe default.
        String effectiveRegion = (config.region == null || config.region.isEmpty())
                ? "us-east-1" : config.region;

        MinioClient.Builder mb = MinioClient.builder()
                .endpoint(config.url)
                .credentials(config.accessKey, config.secretKey)
                .region(effectiveRegion);
        mb.httpClient(buildHttpClient(params), true);
        return mb.build();
    }

    private MinioAsyncClient buildAsyncClient(ParameterOptions params) {
        String effectiveRegion = (config.region == null || config.region.isEmpty())
                ? "us-east-1" : config.region;
        return MinioAsyncClient.builder()
                .endpoint(config.url)
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
        Printer.log.info("  bucket          = " + config.bucketName);
        Printer.log.info("  write operation = " + writeOperation);
        Printer.log.info("  read operation  = " + readOperation);
        Printer.log.info("  client mode     = " + (config.async
                ? "async (depth " + config.asyncDepth + " per worker)" : "synchronous"));
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
        if (asyncClient != null) {
            try {
                asyncClient.close();
            } catch (Exception ex) {
                if (closeFailure == null) {
                    closeFailure = new IOException("Unable to close MinioAsyncClient", ex);
                }
            }
        }
        if (mclient != null) {
            try {
                mclient.close();
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
        return new MinIOWriter(id, params, config, writeOperation, mclient, asyncClient,
                objectCatalog, bucketTargets, createdBuckets, runToken);
    }

    @Override
    public DataReader<byte[]> createReader(final int id, final ParameterOptions params) {
        return new MinIOReader(id, params, config, readOperation, mclient, asyncClient,
                objectCatalog, bucketTargets);
    }

    @Override
    public DataType<byte[]> getDataType() throws IllegalArgumentException {
        return dType;
    }

    private static String nullToEmpty(String s) {
        return s == null ? "" : s;
    }
}
