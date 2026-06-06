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
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicLong;

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

    /** Process-global unique object-id source. */
    private static final AtomicLong OBJECT_ID_GEN = new AtomicLong(
            System.nanoTime() ^ Thread.currentThread().threadId());

    private final int compressibility;
    private final boolean dedupable;
    private long objectId;

    public S3DataGenerator(int compressibility, boolean dedupable) {
        if (compressibility < 0 || compressibility > 100) {
            throw new IllegalArgumentException(
                    "compressibility must be 0..100, got " + compressibility);
        }
        this.compressibility = compressibility;
        this.dedupable = dedupable;
        this.objectId = OBJECT_ID_GEN.incrementAndGet();
    }

    /** Bump the object-id so the anti-dedup stamp varies between objects. */
    public void newObject() {
        this.objectId = OBJECT_ID_GEN.incrementAndGet();
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
            ByteBuffer stamp = ByteBuffer.allocate(STAMP_BYTES);
            stamp.putLong(objectId);
            stamp.putLong((long) off);
            System.arraycopy(stamp.array(), 0, dst, off, STAMP_BYTES);
            cursor = STAMP_BYTES;
        }
        int payload = chunkLen - cursor;
        int randomBytes = (payload * (100 - compressibility)) / 100;
        // Zero portion: dst is already zero-initialised, nothing to do.
        if (randomBytes > 0) {
            byte[] buf = new byte[randomBytes];
            ThreadLocalRandom.current().nextBytes(buf);
            System.arraycopy(buf, 0, dst, off + cursor, randomBytes);
        }
    }
}
