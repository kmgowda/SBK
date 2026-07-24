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

/**
 * Deterministic weighted operation selector.
 *
 * <p>A specification such as {@code put=80,copy=20} produces exactly that
 * ratio over each 100-operation cycle without random-number overhead.
 */
public final class S3OperationMix {
    private final List<S3Operation> operations;
    private final int[] cumulativeWeights;
    private final int totalWeight;
    private long sequence;

    private S3OperationMix(List<S3Operation> operations, int[] cumulativeWeights,
                           int totalWeight, long initialSequence) {
        this.operations = operations;
        this.cumulativeWeights = cumulativeWeights;
        this.totalWeight = totalWeight;
        sequence = initialSequence;
    }

    /**
     * Parse a weighted mix, or select the fallback operation when empty.
     *
     * @param specification comma-separated {@code operation=weight} entries
     * @param fallback operation used for an empty specification
     * @param writerMix true when only writer operations are permitted
     * @param initialSequence worker-specific initial position
     * @return parsed selector
     * @throws IllegalArgumentException when syntax, operation category, or weight is invalid
     */
    public static S3OperationMix parse(String specification, S3Operation fallback,
                                       boolean writerMix, long initialSequence) {
        if (specification == null || specification.isBlank()) {
            return new S3OperationMix(List.of(fallback), new int[]{1}, 1, initialSequence);
        }
        List<S3Operation> parsed = new ArrayList<>();
        List<Integer> weights = new ArrayList<>();
        int total = 0;
        for (String entry : specification.split(",")) {
            String[] fields = entry.trim().split("=", 2);
            if (fields.length != 2) {
                throw new IllegalArgumentException("Invalid S3 operation mix entry '" + entry
                        + "'; expected operation=weight");
            }
            S3Operation operation = S3Operation.fromString(fields[0]);
            if (operation.isWriterOperation() != writerMix) {
                throw new IllegalArgumentException("Operation " + operation + " is not valid in "
                        + (writerMix ? "write-mix" : "read-mix"));
            }
            int weight = Integer.parseInt(fields[1].trim());
            if (weight < 1) {
                throw new IllegalArgumentException("S3 operation mix weights must be positive");
            }
            total = Math.addExact(total, weight);
            parsed.add(operation);
            weights.add(total);
        }
        return new S3OperationMix(List.copyOf(parsed),
                weights.stream().mapToInt(Integer::intValue).toArray(), total, initialSequence);
    }

    /**
     * Select the next operation in the deterministic weighted cycle.
     *
     * @return selected operation
     * @throws IllegalStateException if internal cumulative weights are inconsistent
     */
    public S3Operation next() {
        int position = (int) Math.floorMod(sequence++, totalWeight);
        for (int i = 0; i < cumulativeWeights.length; i++) {
            if (position < cumulativeWeights[i]) {
                return operations.get(i);
            }
        }
        throw new IllegalStateException("Invalid S3 operation mix state");
    }

    /**
     * Check whether any selected operation needs existing objects.
     *
     * @return true when an operation requires the startup catalog
     */
    public boolean requiresObjectCatalog() {
        return operations.stream().anyMatch(S3Operation::requiresObjectCatalog);
    }

    /**
     * Check whether the mix contains an operation.
     *
     * @param operation operation to find
     * @return true when present
     */
    public boolean contains(S3Operation operation) {
        return operations.contains(operation);
    }
}
