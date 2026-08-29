/**
 * Copyright (c) KMG. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 */
package io.perl.api.impl;

import io.perl.api.LatencyPercentiles;
import io.perl.api.LatencyRecord;
import io.perl.api.LatencyRecordWindow;
import io.perl.api.ReportLatencies;
import io.perl.data.Bytes;
import io.time.Time;
import org.eclipse.collections.impl.map.mutable.primitive.LongObjectHashMap;

import java.util.Arrays;

/**
 * Exact latency-frequency recorder optimized for clustered, high-resolution values.
 *
 * <p>Latency values are split into fixed-size pages. A page begins as two compact,
 * sorted primitive arrays and promotes to a dense counter array only after its
 * configured sparse-entry limit is exceeded. Percentile extraction sorts active
 * page identifiers rather than every distinct latency value. This preserves exact
 * nanosecond values while avoiding the hash entry and global-sort cost paid by a
 * flat sparse map for dense regions.</p>
 */
final public class HybridPagedLatencyRecorder extends LatencyRecordWindow {
    private static final int INITIAL_SPARSE_CAPACITY = 4;
    private static final int LONG_BYTES = Long.BYTES;
    private static final int CHARACTER_BYTES = Character.BYTES;
    private static final int PAGE_OBJECT_ESTIMATED_BYTES = 32;
    private static final int PAGE_MAP_ENTRY_ESTIMATED_BYTES = 32;
    private static final int ARRAY_OBJECT_ESTIMATED_BYTES = 16;
    private static final long[] EMPTY_PAGE_IDS = new long[0];

    private final LongObjectHashMap<LatencyPage> pages;
    private final long maxMemoryBytes;
    private final int pageBits;
    private final int pageSize;
    private final int pageMask;
    private final int sparseEntryLimit;
    private long[] activePageIds;
    private int activePageCount;
    private long retainedMemoryBytes;

    /**
     * Creates an exact hybrid paged latency recorder.
     *
     * @param lowLatency minimum accepted latency
     * @param highLatency maximum accepted latency
     * @param totalLatencyMax maximum accumulated latency
     * @param totalRecordsMax maximum accumulated records
     * @param bytesMax maximum accumulated bytes
     * @param percentiles configured percentile fractions
     * @param time latency time source
     * @param maxMemorySizeMB retained-memory target in MiB
     * @param configuredPageBits log2 of the number of exact values per page
     * @param configuredSparseEntryLimit entries retained sparsely before dense promotion
     * @throws IllegalArgumentException if the memory target or page geometry is invalid
     */
    public HybridPagedLatencyRecorder(long lowLatency, long highLatency, long totalLatencyMax,
                                      long totalRecordsMax, long bytesMax, double[] percentiles,
                                      Time time, int maxMemorySizeMB, int configuredPageBits,
                                      int configuredSparseEntryLimit) {
        super(lowLatency, highLatency, totalLatencyMax, totalRecordsMax, bytesMax, percentiles, time);
        if (maxMemorySizeMB < 1 || configuredPageBits < 1
                || configuredPageBits > Character.SIZE
                || configuredSparseEntryLimit < 1
                || configuredSparseEntryLimit >= (1 << configuredPageBits)) {
            throw new IllegalArgumentException("Invalid hybrid latency page configuration");
        }
        this.pageBits = configuredPageBits;
        this.pageSize = 1 << configuredPageBits;
        this.pageMask = pageSize - 1;
        this.sparseEntryLimit = configuredSparseEntryLimit;
        this.maxMemoryBytes = (long) maxMemorySizeMB * Bytes.BYTES_PER_MB;
        this.pages = new LongObjectHashMap<>();
        this.activePageIds = EMPTY_PAGE_IDS;
        this.activePageCount = 0;
        this.retainedMemoryBytes = 0;
    }

    @Override
    public void reset(long startTime) {
        super.reset(startTime);
        clearActivePages();
        releaseOversizedRetainedPages();
    }

    @Override
    public boolean isFull() {
        return retainedMemoryBytes > maxMemoryBytes || super.isOverflow();
    }

    @Override
    public long getMaxMemoryBytes() {
        return maxMemoryBytes;
    }

    /**
     * Returns the estimated retained bytes used by pages and active-page indexes.
     *
     * @return estimated retained bytes
     */
    long getRetainedMemoryBytes() {
        return retainedMemoryBytes;
    }

    @Override
    public void copyPercentiles(LatencyPercentiles percentiles, ReportLatencies copyLatencies) {
        if (copyLatencies != null) {
            copyLatencies.reportLatencyRecord(this);
        }
        percentiles.reset(validLatencyRecords);
        Arrays.sort(activePageIds, 0, activePageCount);
        long currentIndex = 0;
        for (int pageIndex = 0; pageIndex < activePageCount; pageIndex++) {
            final long pageId = activePageIds[pageIndex];
            final LatencyPage page = pages.get(pageId);
            final long pageBase = pageId << pageBits;
            if (page.denseCounts == null) {
                for (int entryIndex = 0; entryIndex < page.sparseSize; entryIndex++) {
                    final long latency = pageBase + page.sparseOffsets[entryIndex];
                    final long count = page.sparseCounts[entryIndex];
                    final long nextIndex = currentIndex + count;
                    if (copyLatencies != null) {
                        copyLatencies.reportLatency(latency, count);
                    }
                    percentiles.copyLatency(latency, count, currentIndex, nextIndex);
                    currentIndex = nextIndex;
                }
            } else {
                for (int offset = 0; offset < pageSize; offset++) {
                    final long count = page.denseCounts[offset];
                    if (count != 0) {
                        final long latency = pageBase + offset;
                        final long nextIndex = currentIndex + count;
                        if (copyLatencies != null) {
                            copyLatencies.reportLatency(latency, count);
                        }
                        percentiles.copyLatency(latency, count, currentIndex, nextIndex);
                        currentIndex = nextIndex;
                    }
                }
            }
            page.clear();
        }
        activePageCount = 0;
        releaseOversizedRetainedPages();
    }

    @Override
    public void reportLatencyRecord(LatencyRecord record) {
        super.update(record);
    }

    @Override
    public void reportLatency(long latency, long count) {
        final long pageId = latency >> pageBits;
        LatencyPage page = pages.get(pageId);
        if (page == null) {
            page = new LatencyPage(pageSize, sparseEntryLimit);
            pages.put(pageId, page);
            retainedMemoryBytes += PAGE_OBJECT_ESTIMATED_BYTES
                    + PAGE_MAP_ENTRY_ESTIMATED_BYTES + page.retainedArrayBytes();
        }
        if (page.isEmpty()) {
            addActivePage(pageId);
        }
        retainedMemoryBytes += page.add(latency & pageMask, count);
    }

    @Override
    public void recordLatency(long startTime, int events, int bytes, long latency) {
        if (record(events, bytes, latency)) {
            reportLatency(latency, events);
        }
    }

    private void addActivePage(long pageId) {
        if (activePageCount == activePageIds.length) {
            final int oldLength = activePageIds.length;
            final int newLength = Math.max(INITIAL_SPARSE_CAPACITY, oldLength << 1);
            activePageIds = Arrays.copyOf(activePageIds, newLength);
            retainedMemoryBytes += (long) (newLength - oldLength) * LONG_BYTES;
            if (oldLength == 0) {
                retainedMemoryBytes += ARRAY_OBJECT_ESTIMATED_BYTES;
            }
        }
        activePageIds[activePageCount++] = pageId;
    }

    private void clearActivePages() {
        for (int index = 0; index < activePageCount; index++) {
            pages.get(activePageIds[index]).clear();
        }
        activePageCount = 0;
    }

    private void releaseOversizedRetainedPages() {
        if (retainedMemoryBytes > maxMemoryBytes) {
            pages.clear();
            pages.trimToSize();
            activePageIds = EMPTY_PAGE_IDS;
            retainedMemoryBytes = 0;
        }
    }

    /** One page of exact latency counters with sparse-to-dense promotion. */
    private static final class LatencyPage {
        private final int pageSize;
        private final int sparseEntryLimit;
        private char[] sparseOffsets;
        private long[] sparseCounts;
        private int sparseSize;
        private int entryCount;
        private long[] denseCounts;

        private LatencyPage(int configuredPageSize, int configuredSparseEntryLimit) {
            this.pageSize = configuredPageSize;
            this.sparseEntryLimit = configuredSparseEntryLimit;
            final int initialCapacity = Math.min(INITIAL_SPARSE_CAPACITY, configuredSparseEntryLimit);
            this.sparseOffsets = new char[initialCapacity];
            this.sparseCounts = new long[initialCapacity];
            this.sparseSize = 0;
            this.entryCount = 0;
            this.denseCounts = null;
        }

        private boolean isEmpty() {
            return entryCount == 0;
        }

        private long add(long offsetValue, long count) {
            final int offset = (int) offsetValue;
            if (denseCounts != null) {
                if (denseCounts[offset] == 0) {
                    entryCount++;
                }
                denseCounts[offset] += count;
                return 0;
            }
            final char characterOffset = (char) offset;
            final int searchResult = Arrays.binarySearch(sparseOffsets, 0, sparseSize, characterOffset);
            if (searchResult >= 0) {
                sparseCounts[searchResult] += count;
                return 0;
            }
            if (sparseSize == sparseEntryLimit) {
                return promoteAndAdd(offset, count);
            }
            final int insertionIndex = -searchResult - 1;
            final long retainedDelta = ensureSparseCapacity();
            if (insertionIndex < sparseSize) {
                System.arraycopy(sparseOffsets, insertionIndex, sparseOffsets, insertionIndex + 1,
                        sparseSize - insertionIndex);
                System.arraycopy(sparseCounts, insertionIndex, sparseCounts, insertionIndex + 1,
                        sparseSize - insertionIndex);
            }
            sparseOffsets[insertionIndex] = characterOffset;
            sparseCounts[insertionIndex] = count;
            sparseSize++;
            entryCount++;
            return retainedDelta;
        }

        private long ensureSparseCapacity() {
            if (sparseSize < sparseOffsets.length) {
                return 0;
            }
            final int oldCapacity = sparseOffsets.length;
            final int newCapacity = Math.min(sparseEntryLimit, oldCapacity << 1);
            sparseOffsets = Arrays.copyOf(sparseOffsets, newCapacity);
            sparseCounts = Arrays.copyOf(sparseCounts, newCapacity);
            return (long) (newCapacity - oldCapacity) * (CHARACTER_BYTES + LONG_BYTES);
        }

        private long promoteAndAdd(int offset, long count) {
            final long oldBytes = retainedArrayBytes();
            denseCounts = new long[pageSize];
            for (int index = 0; index < sparseSize; index++) {
                denseCounts[sparseOffsets[index]] = sparseCounts[index];
            }
            denseCounts[offset] += count;
            entryCount = sparseSize + 1;
            sparseOffsets = null;
            sparseCounts = null;
            sparseSize = 0;
            return retainedArrayBytes() - oldBytes;
        }

        private long retainedArrayBytes() {
            if (denseCounts != null) {
                return ARRAY_OBJECT_ESTIMATED_BYTES + (long) denseCounts.length * LONG_BYTES;
            }
            return 2L * ARRAY_OBJECT_ESTIMATED_BYTES
                    + (long) sparseOffsets.length * (CHARACTER_BYTES + LONG_BYTES);
        }

        private void clear() {
            if (denseCounts == null) {
                sparseSize = 0;
            } else {
                Arrays.fill(denseCounts, 0);
            }
            entryCount = 0;
        }
    }
}
