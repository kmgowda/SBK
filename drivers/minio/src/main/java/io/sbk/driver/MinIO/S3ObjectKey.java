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
    private final String runToken;
    private final int writerId;
    private long counter;

    /**
     * Create an object-key generator for one writer.
     *
     * @param cfg driver configuration
     * @param writerId writer identifier
     * @param runToken process/run discriminator
     */
    public S3ObjectKey(MinIOConfig cfg, int writerId, String runToken) {
        this.fsAccess = cfg.fsAccess;
        this.prefix = (cfg.prefix == null) ? "" : cfg.prefix.trim();
        this.bucketTag = (cfg.bucketName == null) ? "obj" : cfg.bucketName;
        this.writerId = writerId;
        this.runToken = runToken;
        counter = 0;
    }

    /**
     * Generate a fresh object key for the owning writer.
     *
     * @return a unique key string
     */
    public String next() {
        long n = ++counter;
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
        sb.append(bucketTag).append('-').append(runToken).append('-').append(writerId)
                .append('-').append(Long.toUnsignedString(n, 36));
        return sb.toString();
    }
}
