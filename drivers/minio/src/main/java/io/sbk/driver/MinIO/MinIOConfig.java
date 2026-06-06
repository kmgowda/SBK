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

/**
 * Configuration for the MinIO / S3 driver.
 *
 * Mirrors the feature set of the Dell Storage Performance Tool (SPT)
 * S3 storage driver, so that SBK can perform comprehensive S3 benchmarks.
 */
public class MinIOConfig {

    // ----- Connection / credentials -----
    public String url;
    public String accessKey;
    public String secretKey;
    public String bucketName;
    public String region;
    public boolean reCreate;
    public boolean insecure;

    // ----- Object naming / layout -----
    /** When true, use filesystem-style hierarchical object keys (e.g. "a/b/c-uuid"). */
    public boolean fsAccess;
    /** Optional key prefix for all generated objects. */
    public String prefix;

    // ----- Multipart upload (SPT: --part-size, --mpu-concurrent-parts) -----
    /** Part size in bytes; 0 disables multipart. SDK requires 5 MiB .. 5 GiB. */
    public long partSize;
    /** Max concurrent parts uploaded in parallel per object (0 = SDK default). */
    public int mpuConcurrentParts;

    // ----- S3 Checksum validation (SPT: --checksum) -----
    /** crc32, crc32c, sha1, sha256, crc64nvme. Empty/null disables. */
    public String checksumAlgorithm;

    // ----- Auth (SPT: --auth-version) -----
    /** 2 or 4. Default 4 (SigV4). The MinIO SDK uses SigV4 by default. */
    public int authVersion;

    // ----- Object tagging (SPT: --tagging-*) -----
    public boolean taggingEnabled;
    /** Comma-separated `key=value` pairs (e.g. "env=prod,team=storage"). */
    public String taggingTags;

    // ----- Versioning (SPT: bucket versioning) -----
    public boolean versioningEnabled;

    // ----- Data shaping (SPT: --object-data-compressibility, --object-data-dedupable) -----
    /** 0..100, target compressibility percentage. */
    public int dataCompressibility;
    /** When false, anti-dedup stamping is added every 4 KiB. */
    public boolean dataDedupable;

    // ----- Server-side encryption (SPT: SSE-S3) -----
    /** Enable server-side encryption with S3-managed keys (SSE-S3). */
    public boolean sseEnabled;

    // ----- HTTP client tuning -----
    /** Connect timeout in milliseconds (0 = SDK default). */
    public long connectTimeoutMs;
    /** Read/write timeout in milliseconds (0 = SDK default). */
    public long readTimeoutMs;
    /** Write timeout in milliseconds (0 = SDK default). */
    public long writeTimeoutMs;

    /**
     * Comma-separated {@code key=value} headers added to every S3 HTTP
     * request. Use this for backend-specific auth like Dell ECS /
     * ObjectScale: {@code -extra-headers "x-emc-namespace=myns"}.
     */
    public String extraHeaders;
}
