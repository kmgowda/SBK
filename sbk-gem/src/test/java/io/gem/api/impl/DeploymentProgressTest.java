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

import org.junit.jupiter.api.Test;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Tests centralized deployment progress and transfer-size formatting. */
final class DeploymentProgressTest {
    @Test
    void reportsFinishedAndPendingRuntimeTransfers() {
        final CompletableFuture<?>[] uploads = {CompletableFuture.completedFuture(null),
                new CompletableFuture<>(), CompletableFuture.completedFuture(null)};
        final String[] hosts = {"node-a:22", "node-b:2202", null};

        assertEquals("1 of 2 transfer(s) finished; awaiting host(s): node-b:2202",
                DeploymentProgress.transferStatus(uploads, hosts, "transfer(s)"));
        assertEquals("waiting for node-b:2202", DeploymentProgress.pendingHosts(uploads, hosts));
    }

    @Test
    void reportsCopyBytesPercentageAndRateForPhysicalTargets() {
        final CompletableFuture<?>[] copies = {new CompletableFuture<>(), new CompletableFuture<>()};
        final String[] hosts = {"node-a:22", null};
        final AtomicLong[] copiedBytes = {new AtomicLong(50L * 1024 * 1024), new AtomicLong()};

        final String progress = DeploymentProgress.copyStatus(copies, hosts, copiedBytes,
                100L * 1024 * 1024, System.nanoTime() - TimeUnit.SECONDS.toNanos(2), "Java operation(s)");

        assertTrue(progress.contains("0 of 1 Java operation(s) finished"));
        assertTrue(progress.contains("transferred 50.00 MiB of 100.00 MiB"));
        assertTrue(progress.matches(".*\\[50\\.0%, [0-9.]+ MiB/s, ETA [0-9]+ second\\(s\\)\\]"));
    }

    @Test
    void formatsTransferSizesUsingAppropriateBinaryUnits() {
        assertEquals("0.00 KiB", DeploymentProgress.formatSize(0));
        assertEquals("1.50 KiB", DeploymentProgress.formatSize(1_536));
        assertEquals("1.00 MiB", DeploymentProgress.formatSize(1_048_576));
        assertEquals("1.16 GiB", DeploymentProgress.formatSize(1_249_950_208L));
    }
}
