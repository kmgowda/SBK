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

import java.util.Locale;

/**
 * Generates valid, unique bucket names for bucket-create workloads.
 */
public final class S3BucketName {
    private static final int MAX_BUCKET_LENGTH = 63;
    private final String prefix;
    private final String runToken;
    private final int workerId;
    private long sequence;

    /**
     * Create a bucket-name generator.
     *
     * @param configuredPrefix user-supplied prefix
     * @param runToken process/run discriminator
     * @param workerId writer id
     */
    public S3BucketName(String configuredPrefix, String runToken, int workerId) {
        prefix = sanitize(configuredPrefix);
        this.runToken = sanitize(runToken);
        this.workerId = workerId;
        sequence = 0;
    }

    /**
     * Generate the next bucket name.
     *
     * @return valid S3 bucket name no longer than 63 characters
     */
    public String next() {
        String suffix = "-" + runToken + "-" + workerId + "-"
                + Long.toUnsignedString(++sequence, 36);
        int prefixLimit = Math.max(3, MAX_BUCKET_LENGTH - suffix.length());
        String base = prefix.length() > prefixLimit ? prefix.substring(0, prefixLimit) : prefix;
        base = trimHyphens(base);
        if (base.length() < 3) {
            base = "sbk";
        }
        return base + suffix;
    }

    private static String sanitize(String value) {
        String source = value == null ? "" : value.toLowerCase(Locale.ROOT);
        String result = source.replaceAll("[^a-z0-9.-]", "-")
                .replaceAll("\\.{2,}", ".")
                .replaceAll("-{2,}", "-");
        result = trimHyphens(result);
        return result.isEmpty() ? "sbk-bucket" : result;
    }

    private static String trimHyphens(String value) {
        return value.replaceAll("^[.-]+|[.-]+$", "");
    }
}
