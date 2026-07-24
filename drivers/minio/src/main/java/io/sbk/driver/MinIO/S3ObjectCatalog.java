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

import java.util.List;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Shared object catalog used to avoid an S3 list request before every read.
 *
 * <p>Read-only workers cycle over a fixed startup snapshot and partition it by
 * reader id. Mixed write/read workloads publish newly completed PUTs through a
 * blocking queue so readers do not spin while waiting for objects.
 */
public final class S3ObjectCatalog {
    private final List<S3ObjectRef> snapshot;
    private final LinkedBlockingQueue<S3ObjectRef> published;
    private final AtomicLong sharedCursor;
    private final AtomicLong deleteCursor;

    /**
     * Create a catalog from a startup object listing.
     *
     * @param objects immutable startup snapshot
     */
    public S3ObjectCatalog(List<S3ObjectRef> objects) {
        snapshot = List.copyOf(objects);
        published = new LinkedBlockingQueue<>();
        sharedCursor = new AtomicLong();
        deleteCursor = new AtomicLong();
    }

    /**
     * Number of objects in the startup snapshot.
     *
     * @return snapshot size
     */
    public int size() {
        return snapshot.size();
    }

    /**
     * Check whether the startup snapshot contains an object larger than the
     * supplied byte offset.
     *
     * @param offset exclusive lower bound for object size
     * @return true when at least one eligible object exists
     */
    public boolean hasObjectLargerThan(long offset) {
        for (S3ObjectRef object : snapshot) {
            if (object.size() > offset) {
                return true;
            }
        }
        return false;
    }

    /**
     * Select an existing object cyclically for update, copy, stat, or tagging.
     *
     * @return selected object, or {@code null} when the snapshot is empty
     */
    public S3ObjectRef nextShared() {
        if (snapshot.isEmpty()) {
            return null;
        }
        return snapshot.get(index(sharedCursor.getAndIncrement(), snapshot.size()));
    }

    /**
     * Claim an object once for destructive delete workloads.
     *
     * @return the next unclaimed object, or {@code null} when exhausted
     */
    public S3ObjectRef claimDelete() {
        long position = deleteCursor.getAndIncrement();
        return position < snapshot.size() ? snapshot.get((int) position) : null;
    }

    /**
     * Select a reader's next partitioned object.
     *
     * @param readerId zero-based reader id
     * @param readerCount total reader count
     * @param sequence reader-local sequence number
     * @return selected object, or {@code null} when the snapshot is empty
     */
    public S3ObjectRef nextForReader(int readerId, int readerCount, long sequence) {
        if (snapshot.isEmpty()) {
            return null;
        }
        long logicalIndex = readerId + sequence * Math.max(1, readerCount);
        return snapshot.get(index(logicalIndex, snapshot.size()));
    }

    /**
     * Select the next reader object whose size is larger than the supplied
     * offset. The bounded scan prevents an invalid range configuration from
     * turning into an endless stream of zero-record iterations.
     *
     * @param readerId zero-based reader id
     * @param readerCount total reader count
     * @param sequence reader-local sequence number
     * @param offset exclusive lower bound for object size
     * @return an eligible object, or {@code null} when none exists
     */
    public S3ObjectRef nextForReader(int readerId, int readerCount, long sequence, long offset) {
        if (snapshot.isEmpty()) {
            return null;
        }
        long logicalIndex = readerId + sequence * Math.max(1, readerCount);
        for (int i = 0; i < snapshot.size(); i++) {
            S3ObjectRef object = snapshot.get(index(logicalIndex + i, snapshot.size()));
            if (object.size() > offset) {
                return object;
            }
        }
        return null;
    }

    /**
     * Publish an object after a successful PUT for mixed write/read workloads.
     *
     * @param object completed object
     */
    public void publish(S3ObjectRef object) {
        published.offer(object);
    }

    /**
     * Wait briefly for a newly written object.
     *
     * @param timeout timeout value
     * @param unit timeout unit
     * @return published object, or {@code null} on timeout
     * @throws InterruptedException when the worker is interrupted
     */
    public S3ObjectRef pollPublished(long timeout, TimeUnit unit) throws InterruptedException {
        return published.poll(timeout, unit);
    }

    private static int index(long value, int size) {
        return (int) Math.floorMod(value, size);
    }
}
