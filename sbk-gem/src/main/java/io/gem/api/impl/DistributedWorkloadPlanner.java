/**
 * Copyright (c) KMG. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.gem.api.impl;

import io.perl.data.Bytes;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

/** Plans immutable per-node workload arguments from aggregate user limits. */
final class DistributedWorkloadPlanner {
    private static final int TOTAL_THROUGHPUT_SCALE = 12;

    private DistributedWorkloadPlanner() {
    }

    static List<List<String>> distributeRecords(List<String> commonArguments, long totalRecords,
                                                 int nodeCount, int workers, boolean rateMode) {
        if (totalRecords <= 0 || nodeCount <= 0 || workers <= 0) {
            throw new IllegalArgumentException("Total records, node count, and worker count must be positive");
        }
        final long allocationUnits = rateMode ? totalRecords / workers : totalRecords;
        if (rateMode && totalRecords % workers != 0) {
            throw new IllegalArgumentException("Total records/second must be divisible by the worker count");
        }
        if (allocationUnits < nodeCount) {
            throw new IllegalArgumentException("Total records must allocate at least one unit to every node");
        }
        final long unitsPerNode = allocationUnits / nodeCount;
        final long remainder = allocationUnits % nodeCount;
        final List<List<String>> argumentsByNode = new ArrayList<>(nodeCount);
        for (int i = 0; i < nodeCount; i++) {
            final long nodeUnits = unitsPerNode + (i < remainder ? 1 : 0);
            if (rateMode && nodeUnits > Integer.MAX_VALUE) {
                throw new IllegalArgumentException("The per-worker records/second value on node " + (i + 1)
                        + " exceeds " + Integer.MAX_VALUE);
            }
            final long nodeRecords = rateMode ? Math.multiplyExact(nodeUnits, workers) : nodeUnits;
            argumentsByNode.add(withOption(commonArguments, "-records", Long.toString(nodeRecords)));
        }
        return List.copyOf(argumentsByNode);
    }

    static List<List<String>> distributeThroughput(List<List<String>> argumentsByNode,
                                                    BigDecimal totalThroughput, int recordSize, int workers) {
        if (argumentsByNode.isEmpty() || totalThroughput.signum() <= 0 || recordSize <= 0 || workers <= 0) {
            throw new IllegalArgumentException(
                    "Remote arguments, total throughput, record size, and worker count must be positive");
        }
        final BigDecimal nodeCount = BigDecimal.valueOf(argumentsByNode.size());
        final BigDecimal baseThroughput = totalThroughput.divide(nodeCount, TOTAL_THROUGHPUT_SCALE,
                RoundingMode.DOWN);
        final BigDecimal remainder = totalThroughput.subtract(baseThroughput.multiply(nodeCount));
        final List<List<String>> distributedArguments = new ArrayList<>(argumentsByNode.size());
        for (int i = 0; i < argumentsByNode.size(); i++) {
            final BigDecimal nodeThroughput = i == 0 ? baseThroughput.add(remainder) : baseThroughput;
            final double nodeThroughputValue = nodeThroughput.doubleValue();
            final double recordsPerWorker = nodeThroughputValue * Bytes.BYTES_PER_MB / recordSize / workers;
            if (!Double.isFinite(nodeThroughputValue) || recordsPerWorker < 1) {
                throw new IllegalArgumentException("The '-totalthroughput' value must provide at least one "
                        + "record/second per active worker on every node");
            }
            if (!Double.isFinite(recordsPerWorker) || recordsPerWorker > Integer.MAX_VALUE) {
                throw new IllegalArgumentException("The '-totalthroughput' value exceeds SBK's maximum "
                        + "record/second rate per active worker");
            }
            distributedArguments.add(withOption(argumentsByNode.get(i), "-throughput",
                    nodeThroughput.stripTrailingZeros().toPlainString()));
        }
        return List.copyOf(distributedArguments);
    }

    static List<List<String>> identicalArguments(List<String> commonArguments, int nodeCount) {
        final List<String> immutableArguments = List.copyOf(commonArguments);
        final List<List<String>> argumentsByNode = new ArrayList<>(nodeCount);
        for (int i = 0; i < nodeCount; i++) {
            argumentsByNode.add(immutableArguments);
        }
        return List.copyOf(argumentsByNode);
    }

    private static List<String> withOption(List<String> arguments, String option, String value) {
        final List<String> result = new ArrayList<>(arguments.size() + 2);
        result.addAll(arguments);
        result.add(option);
        result.add(value);
        return List.copyOf(result);
    }
}
