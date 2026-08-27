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

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

/** Formats progress for concurrent remote deployment operations. */
final class DeploymentProgress {
    private static final long BYTES_PER_GIB = (long) Bytes.BYTES_PER_MB * Bytes.BYTES_PER_KB;
    private static final double PERCENTAGE_SCALE = 100.0;
    private static final double NANOSECONDS_PER_SECOND = TimeUnit.SECONDS.toNanos(1);

    private DeploymentProgress() {
    }

    static String pendingHosts(CompletableFuture<?>[] futures, String[] targetHosts) {
        final List<String> pendingHosts = new ArrayList<>();
        for (int i = 0; i < targetHosts.length; i++) {
            if (targetHosts[i] != null && !futures[i].isDone()) {
                pendingHosts.add(targetHosts[i]);
            }
        }
        return pendingHosts.isEmpty() ? "finalizing" : "waiting for " + String.join(", ", pendingHosts);
    }

    static String transferStatus(CompletableFuture<?>[] futures, String[] targetHosts,
                                 String operationLabel) {
        int finished = 0;
        int total = 0;
        final List<String> pendingHosts = new ArrayList<>();
        for (int i = 0; i < targetHosts.length; i++) {
            if (targetHosts[i] != null) {
                total++;
                if (futures[i].isDone()) {
                    finished++;
                } else {
                    pendingHosts.add(targetHosts[i]);
                }
            }
        }
        return finished + " of " + total + " " + operationLabel + " finished; awaiting host(s): "
                + String.join(", ", pendingHosts);
    }

    static String copyStatus(CompletableFuture<?>[] copies, String[] copyHosts,
                             AtomicLong[] copiedBytes, long contentBytesPerTarget,
                             long startedNanos, String operationDescription) {
        final long copied = copiedByteCount(copiedBytes);
        int targets = 0;
        for (String host : copyHosts) {
            if (host != null) {
                targets++;
            }
        }
        final long total = saturatedMultiply(contentBytesPerTarget, targets);
        final double percentage = total == 0 ? PERCENTAGE_SCALE
                : Math.min(PERCENTAGE_SCALE, copied * PERCENTAGE_SCALE / total);
        final double elapsedSeconds = Math.max(1L, System.nanoTime() - startedNanos) / NANOSECONDS_PER_SECOND;
        final double mebibytesPerSecond = copied / (double) Bytes.BYTES_PER_MB / elapsedSeconds;
        final String estimate;
        if (copied == 0) {
            estimate = "ETA pending while remote metadata is prepared";
        } else if (copied < total) {
            final long remainingSeconds = Math.max(1L,
                    (long) Math.ceil((total - copied) / (copied / elapsedSeconds)));
            estimate = "ETA " + remainingSeconds + " second(s)";
        } else {
            estimate = "data transfer complete; finalizing remote metadata";
        }
        return String.format(Locale.ROOT, "%s; transferred %s of %s [%.1f%%, %.2f MiB/s, %s]",
                transferStatus(copies, copyHosts, operationDescription), formatSize(copied),
                formatSize(total), percentage, mebibytesPerSecond, estimate);
    }

    static String formatSize(long bytes) {
        if (bytes >= BYTES_PER_GIB) {
            return String.format(Locale.ROOT, "%,.2f GiB", bytes / (double) BYTES_PER_GIB);
        }
        if (bytes >= Bytes.BYTES_PER_MB) {
            return String.format(Locale.ROOT, "%,.2f MiB", bytes / (double) Bytes.BYTES_PER_MB);
        }
        return String.format(Locale.ROOT, "%,.2f KiB", bytes / (double) Bytes.BYTES_PER_KB);
    }

    static long copiedByteCount(AtomicLong[] copiedBytes) {
        long copied = 0;
        for (AtomicLong counter : copiedBytes) {
            copied += counter.get();
        }
        return copied;
    }

    private static long saturatedMultiply(long value, int multiplier) {
        if (value == 0 || multiplier == 0) {
            return 0;
        }
        return value > Long.MAX_VALUE / multiplier ? Long.MAX_VALUE : value * multiplier;
    }
}
