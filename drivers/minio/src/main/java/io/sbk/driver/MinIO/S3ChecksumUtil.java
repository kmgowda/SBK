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

import java.nio.ByteBuffer;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import java.util.zip.CRC32;
import java.util.zip.CRC32C;

/**
 * S3 checksum calculation utility.
 *
 * <p>Implements the five algorithms accepted by AWS S3 PutObject and
 * Dell SPT: CRC32, CRC32C, SHA1, SHA256, CRC64-NVMe. Each algorithm
 * produces a Base64-encoded digest that is sent as an
 * {@code x-amz-checksum-*} header.
 */
public final class S3ChecksumUtil {

    // CRC64-NVMe (polynomial 0xad93d23594c93659, reflected form used by AWS S3).
    private static final long CRC64_NVME_POLY = 0x9a6c9329ac4bc9b5L;
    private static final long[] CRC64_NVME_TABLE = buildCrc64Table(CRC64_NVME_POLY);

    private S3ChecksumUtil() {
    }

    /** Supported S3 checksum algorithms. */
    public enum Algorithm {
        CRC32("crc32", "x-amz-checksum-crc32"),
        CRC32C("crc32c", "x-amz-checksum-crc32c"),
        SHA1("sha1", "x-amz-checksum-sha1"),
        SHA256("sha256", "x-amz-checksum-sha256"),
        CRC64NVME("crc64nvme", "x-amz-checksum-crc64nvme");

        public final String configToken;
        public final String headerName;

        Algorithm(String configToken, String headerName) {
            this.configToken = configToken;
            this.headerName = headerName;
        }

        /**
         * Resolve a textual config value to an {@link Algorithm}.
         *
         * @param s user-supplied value (case-insensitive; dashes/underscores ignored)
         * @return the matching algorithm, or {@code null} when {@code s} is empty
         * @throws IllegalArgumentException when {@code s} does not name a supported algorithm
         */
        public static Algorithm fromString(String s) {
            if (s == null || s.isEmpty()) {
                return null;
            }
            String norm = s.trim().toLowerCase().replace("-", "").replace("_", "");
            for (Algorithm a : values()) {
                if (a.configToken.equals(norm)) {
                    return a;
                }
            }
            throw new IllegalArgumentException("Unsupported checksum algorithm: " + s);
        }
    }

    /**
     * Compute the checksum and return the raw bytes.
     *
     * @param data      payload to checksum
     * @param algorithm algorithm to use; {@code null} returns {@code null}
     * @return checksum bytes, or {@code null} when {@code algorithm} is {@code null}
     * @throws IllegalStateException when the algorithm enum has an unexpected value
     */
    public static byte[] compute(byte[] data, Algorithm algorithm) {
        if (algorithm == null) {
            return null;
        }
        switch (algorithm) {
            case CRC32:
                return crc32(data);
            case CRC32C:
                return crc32c(data);
            case SHA1:
                return digest(data, "SHA-1");
            case SHA256:
                return digest(data, "SHA-256");
            case CRC64NVME:
                return crc64nvme(data);
            default:
                throw new IllegalStateException("Unhandled algorithm: " + algorithm);
        }
    }

    /**
     * Compute and Base64-encode in one shot — the format S3 expects in
     * the {@code x-amz-checksum-*} header.
     *
     * @param data      payload to checksum
     * @param algorithm algorithm to use; {@code null} returns {@code null}
     * @return Base64-encoded digest, or {@code null} when {@code algorithm} is {@code null}
     */
    public static String computeBase64(byte[] data, Algorithm algorithm) {
        byte[] raw = compute(data, algorithm);
        return raw == null ? null : Base64.getEncoder().encodeToString(raw);
    }

    private static byte[] crc32(byte[] data) {
        CRC32 c = new CRC32();
        c.update(data);
        return ByteBuffer.allocate(4).putInt((int) (c.getValue() & 0xFFFFFFFFL)).array();
    }

    private static byte[] crc32c(byte[] data) {
        CRC32C c = new CRC32C();
        c.update(data);
        return ByteBuffer.allocate(4).putInt((int) (c.getValue() & 0xFFFFFFFFL)).array();
    }

    private static byte[] digest(byte[] data, String alg) {
        try {
            return MessageDigest.getInstance(alg).digest(data);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(alg + " not available", e);
        }
    }

    private static long[] buildCrc64Table(long poly) {
        long[] table = new long[256];
        for (int b = 0; b < 256; b++) {
            long crc = b;
            for (int i = 0; i < 8; i++) {
                if ((crc & 1L) != 0) {
                    crc = (crc >>> 1) ^ poly;
                } else {
                    crc = crc >>> 1;
                }
            }
            table[b] = crc;
        }
        return table;
    }

    private static byte[] crc64nvme(byte[] data) {
        long crc = 0L;
        for (byte b : data) {
            crc = CRC64_NVME_TABLE[(int) ((crc ^ b) & 0xFF)] ^ (crc >>> 8);
        }
        return ByteBuffer.allocate(8).putLong(crc).array();
    }
}
