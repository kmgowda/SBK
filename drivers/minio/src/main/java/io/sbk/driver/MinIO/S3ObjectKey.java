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

import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Object-key generator.
 *
 * <p>Supports an optional prefix and a "filesystem access" mode that
 * spreads keys across a 2-level directory tree. Sample outputs:
 * <pre>
 *   plain:    sbk-{uuid}
 *   prefix:   {prefix}/sbk-{uuid}
 *   fsAccess: {prefix}/aa/bb/sbk-{uuid}
 * </pre>
 */
public final class S3ObjectKey {

    private static final char[] HEX = "0123456789abcdef".toCharArray();

    private final boolean fsAccess;
    private final String prefix;
    private final String bucketTag;
    private final AtomicLong counter = new AtomicLong();

    public S3ObjectKey(MinIOConfig cfg) {
        this.fsAccess = cfg.fsAccess;
        this.prefix = (cfg.prefix == null) ? "" : cfg.prefix.trim();
        this.bucketTag = (cfg.bucketName == null) ? "obj" : cfg.bucketName;
    }

    /**
     * Generate a fresh object key. Thread-safe.
     *
     * @return a unique key string
     */
    public String next() {
        long n = counter.incrementAndGet();
        StringBuilder sb = new StringBuilder(96);
        if (!prefix.isEmpty()) {
            sb.append(prefix);
            if (!prefix.endsWith("/")) {
                sb.append('/');
            }
        }
        if (fsAccess) {
            sb.append(HEX[(int) (n >> 4) & 0xF]).append(HEX[(int) n & 0xF]).append('/');
            sb.append(HEX[(int) (n >> 12) & 0xF]).append(HEX[(int) (n >> 8) & 0xF]).append('/');
        }
        sb.append(bucketTag).append('-').append(UUID.randomUUID());
        return sb.toString();
    }
}
