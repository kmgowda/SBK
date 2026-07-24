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

import java.util.SplittableRandom;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Generates per-write payload bytes with configurable
 * <em>compressibility</em> and <em>deduplication</em> characteristics.
 *
 * <p>Mirrors Dell SPT's {@code --object-data-compressibility} and
 * {@code --object-data-dedupable} controls:
 * <ul>
 *   <li>4 KiB chunks: each chunk is split into a random portion
 *       ({@code 100 - compressibility}%) and a zero portion
 *       ({@code compressibility}%). Zero portions are highly
 *       compressible; the random portion is not.</li>
 *   <li>When {@code dedupable == false} a 16-byte anti-dedup stamp
 *       (object-id + chunk-offset) is written at the start of every chunk,
 *       which defeats inline deduplication.</li>
 * </ul>
 *
 * <p>Instances are inexpensive; create one per writer thread.
 */
public final class S3DataGenerator {

    private static final int CHUNK_SIZE = 4 * 1024;
    private static final int STAMP_BYTES = 16;

    private final int compressibility;
    private final boolean dedupable;
    private final SplittableRandom random;
    private long objectId;

    public S3DataGenerator(int compressibility, boolean dedupable) {
        this(compressibility, dedupable, ThreadLocalRandom.current().nextLong());
    }

    /**
     * Create a generator with a reproducible random seed.
     *
     * @param compressibility zero to one hundred
     * @param dedupable whether identical chunks may be generated
     * @param seed payload seed
     * @throws IllegalArgumentException when compressibility is outside zero to one hundred
     */
    public S3DataGenerator(int compressibility, boolean dedupable, long seed) {
        if (compressibility < 0 || compressibility > 100) {
            throw new IllegalArgumentException(
                    "compressibility must be 0..100, got " + compressibility);
        }
        this.compressibility = compressibility;
        this.dedupable = dedupable;
        random = new SplittableRandom(seed);
        this.objectId = seed;
    }

    /** Bump the object-id so the anti-dedup stamp varies between objects. */
    public void newObject() {
        objectId++;
    }

    /**
     * Generate {@code size} bytes following the configured pattern.
     *
     * @param size number of bytes to generate
     * @return freshly-allocated byte array of length {@code size}
     */
    public byte[] generate(int size) {
        byte[] out = new byte[size];
        fill(out, 0, size);
        return out;
    }

    /**
     * Fill {@code dst[offset..offset+len)} with the configured pattern.
     *
     * @param dst    destination buffer (must already be allocated)
     * @param offset start position within {@code dst}
     * @param len    number of bytes to write
     */
    public void fill(byte[] dst, int offset, int len) {
        int written = 0;
        while (written < len) {
            int chunkLen = Math.min(CHUNK_SIZE, len - written);
            fillChunk(dst, offset + written, chunkLen);
            written += chunkLen;
        }
    }

    private void fillChunk(byte[] dst, int off, int chunkLen) {
        int cursor = 0;
        if (!dedupable && chunkLen >= STAMP_BYTES) {
            putLong(dst, off, objectId);
            putLong(dst, off + Long.BYTES, off);
            cursor = STAMP_BYTES;
        }
        int payload = chunkLen - cursor;
        int randomBytes = (payload * (100 - compressibility)) / 100;
        // Zero portion: dst is already zero-initialised, nothing to do.
        if (randomBytes > 0) {
            fillRandom(dst, off + cursor, randomBytes);
        }
        int zeroOffset = off + cursor + randomBytes;
        for (int i = zeroOffset; i < off + chunkLen; i++) {
            dst[i] = 0;
        }
    }

    private void fillRandom(byte[] dst, int offset, int length) {
        int position = offset;
        int end = offset + length;
        while (position + Long.BYTES <= end) {
            putLong(dst, position, random.nextLong());
            position += Long.BYTES;
        }
        long tail = random.nextLong();
        while (position < end) {
            dst[position++] = (byte) tail;
            tail >>>= Byte.SIZE;
        }
    }

    private static void putLong(byte[] dst, int offset, long value) {
        for (int i = Long.BYTES - 1; i >= 0; i--) {
            dst[offset + i] = (byte) value;
            value >>>= Byte.SIZE;
        }
    }
}
