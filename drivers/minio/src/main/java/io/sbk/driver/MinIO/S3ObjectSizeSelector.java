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

import java.util.ArrayList;
import java.util.List;

/** Selects deterministic object sizes without random allocation in the operation path. */
final class S3ObjectSizeSelector {
    private final int uniformMinimum;
    private final int uniformSpan;
    private final int[] weightedSizes;
    private final int[] cumulativeWeights;
    private final int totalWeight;
    private long sequence;

    private S3ObjectSizeSelector(int uniformMinimum, int uniformSpan, int[] weightedSizes,
                                 int[] cumulativeWeights, int totalWeight, long sequence) {
        this.uniformMinimum = uniformMinimum;
        this.uniformSpan = uniformSpan;
        this.weightedSizes = weightedSizes;
        this.cumulativeWeights = cumulativeWeights;
        this.totalWeight = totalWeight;
        this.sequence = sequence;
    }

    static S3ObjectSizeSelector parse(String specification, long initialSequence) {
        if (specification == null || specification.isBlank()
                || specification.equalsIgnoreCase("fixed")) {
            return new S3ObjectSizeSelector(0, 0, null, null, 0, initialSequence);
        }
        String normalized = specification.trim().toLowerCase(java.util.Locale.ROOT);
        if (normalized.startsWith("uniform:")) {
            String[] bounds = normalized.substring("uniform:".length()).split(":", 2);
            if (bounds.length != 2) {
                throw new IllegalArgumentException(
                        "object-size-distribution uniform syntax is uniform:min:max");
            }
            int minimum = positiveSize(bounds[0]);
            int maximum = positiveSize(bounds[1]);
            if (maximum < minimum) {
                throw new IllegalArgumentException(
                        "object-size-distribution maximum must be at least its minimum");
            }
            int span = Math.addExact(Math.subtractExact(maximum, minimum), 1);
            return new S3ObjectSizeSelector(minimum, span,
                    null, null, 0, initialSequence);
        }
        if (normalized.startsWith("weighted:")) {
            List<Integer> sizes = new ArrayList<>();
            List<Integer> weights = new ArrayList<>();
            int total = 0;
            for (String entry : normalized.substring("weighted:".length()).split(",")) {
                String[] fields = entry.trim().split("=", 2);
                if (fields.length != 2) {
                    throw new IllegalArgumentException(
                            "object-size-distribution weighted entries require size=weight");
                }
                int size = positiveSize(fields[0]);
                int weight = Integer.parseInt(fields[1].trim());
                if (weight < 1) {
                    throw new IllegalArgumentException("object-size weights must be positive");
                }
                total = Math.addExact(total, weight);
                sizes.add(size);
                weights.add(total);
            }
            return new S3ObjectSizeSelector(0, 0,
                    sizes.stream().mapToInt(Integer::intValue).toArray(),
                    weights.stream().mapToInt(Integer::intValue).toArray(), total,
                    initialSequence);
        }
        throw new IllegalArgumentException("object-size-distribution must be fixed, "
                + "uniform:min:max, or weighted:size=weight,...");
    }

    int next(int fixedSize) {
        if (weightedSizes != null) {
            int position = (int) Math.floorMod(sequence++, totalWeight);
            for (int index = 0; index < cumulativeWeights.length; index++) {
                if (position < cumulativeWeights[index]) {
                    return weightedSizes[index];
                }
            }
        }
        if (uniformSpan > 0) {
            return uniformMinimum + (int) Math.floorMod(sequence++, uniformSpan);
        }
        return fixedSize;
    }

    int maximum(int fixedSize) {
        if (weightedSizes != null) {
            return java.util.Arrays.stream(weightedSizes).max().orElse(fixedSize);
        }
        return uniformSpan > 0 ? uniformMinimum + uniformSpan - 1 : fixedSize;
    }

    private static int positiveSize(String value) {
        int size = Integer.parseInt(value.trim());
        if (size < 1) {
            throw new IllegalArgumentException("object sizes must be positive");
        }
        return size;
    }
}
