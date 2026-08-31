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
    private final int partitionIndex;
    private final int partitionCount;
    private final boolean partitionByPrefix;
    private final KeyDistribution distribution;
    private final java.util.SplittableRandom random;
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
        partitionIndex = cfg.partitionIndex;
        partitionCount = cfg.partitionCount;
        partitionByPrefix = cfg.partitionByPrefix;
        distribution = KeyDistribution.parse(cfg.keyDistribution);
        random = new java.util.SplittableRandom((cfg.dataSeed == 0 ? System.nanoTime()
                : cfg.dataSeed) + writerId);
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
        if (partitionCount > 1 && partitionByPrefix) {
            sb.append("partition-").append(partitionIndex).append('/');
        }
        long keyValue = distribution == KeyDistribution.RANDOM ? random.nextLong() : n;
        if (fsAccess || distribution == KeyDistribution.HASHED) {
            long hash = mix64(keyValue);
            sb.append(HEX[(int) (hash >> 4) & 0xF]).append(HEX[(int) hash & 0xF]).append('/');
            sb.append(HEX[(int) (hash >> 12) & 0xF]).append(HEX[(int) (hash >> 8) & 0xF])
                    .append('/');
        }
        sb.append(bucketTag).append('-').append(runToken).append('-').append(writerId);
        if (partitionCount > 1) {
            sb.append("-p").append(partitionIndex);
        }
        sb.append('-').append(Long.toUnsignedString(keyValue, 36));
        return sb.toString();
    }

    static String partitionPrefix(MinIOConfig config) {
        String prefix = config.prefix == null ? "" : config.prefix.trim();
        StringBuilder value = new StringBuilder(prefix);
        if (!value.isEmpty() && value.charAt(value.length() - 1) != '/') {
            value.append('/');
        }
        value.append("partition-").append(config.partitionIndex).append('/');
        return value.toString();
    }

    static void validateDistribution(String specification) {
        KeyDistribution.parse(specification);
    }

    private static long mix64(long value) {
        value = (value ^ (value >>> 30)) * 0xbf58476d1ce4e5b9L;
        value = (value ^ (value >>> 27)) * 0x94d049bb133111ebL;
        return value ^ (value >>> 31);
    }

    private enum KeyDistribution {
        SEQUENTIAL,
        HASHED,
        RANDOM;

        static KeyDistribution parse(String specification) {
            if (specification == null || specification.isBlank()) {
                return SEQUENTIAL;
            }
            try {
                return valueOf(specification.trim().toUpperCase(java.util.Locale.ROOT));
            } catch (IllegalArgumentException ex) {
                throw new IllegalArgumentException(
                        "key-distribution must be sequential, hashed, or random", ex);
            }
        }
    }
}
