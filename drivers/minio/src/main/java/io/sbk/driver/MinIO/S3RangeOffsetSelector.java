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
import java.util.SplittableRandom;

/** Selects aligned offsets for ranged GET workloads without allocation. */
final class S3RangeOffsetSelector {
    private final Mode mode;
    private final long firstOffset;
    private final long windowLength;
    private final long alignment;
    private final SplittableRandom random;
    private long sequence;

    S3RangeOffsetSelector(String mode, long firstOffset, long windowLength,
                          long alignment, long seed) {
        this.mode = Mode.parse(mode);
        this.firstOffset = firstOffset;
        this.windowLength = windowLength;
        this.alignment = alignment;
        if (windowLength > 0) {
            Math.addExact(firstOffset, windowLength - 1);
        }
        random = this.mode == Mode.RANDOM ? new SplittableRandom(seed) : null;
    }

    long next(long objectSize, long requestLength) {
        long lastObjectOffset = Math.max(firstOffset, objectSize - requestLength);
        long configuredLast = windowLength == 0
                ? lastObjectOffset
                : Math.min(lastObjectOffset, Math.addExact(firstOffset, windowLength - 1));
        long slots = Math.max(1, (configuredLast - firstOffset) / alignment + 1);
        long selectedSlot = switch (mode) {
            case FIXED -> 0;
            case SEQUENTIAL -> Math.floorMod(sequence++, slots);
            case RANDOM -> random.nextLong(slots);
        };
        return firstOffset + selectedSlot * alignment;
    }

    private enum Mode {
        FIXED,
        SEQUENTIAL,
        RANDOM;

        static Mode parse(String value) {
            if (value == null || value.isBlank()) {
                return FIXED;
            }
            try {
                return valueOf(value.trim().toUpperCase(Locale.ROOT));
            } catch (RuntimeException ex) {
                throw new IllegalArgumentException(
                        "range-offset-distribution must be fixed, sequential, or random", ex);
            }
        }
    }
}
