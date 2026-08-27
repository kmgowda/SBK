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

import io.gem.api.SshResponse;
import org.junit.jupiter.api.Test;

import java.net.ConnectException;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Tests manager-owned runtime lease launch and release state transitions. */
final class RuntimeLeaseManagerTest {
    @Test
    void claimsOnlyActiveUnlaunchedLease() {
        final RemoteNodeState node = new RemoteNodeState(0, null, List.of());
        final RuntimeLeaseManager manager = new RuntimeLeaseManager(null, null, List.of(node), null);
        node.leaseActive(true);

        assertTrue(manager.claimUnlaunchedLease(node));
        assertFalse(node.leaseActive());
        assertFalse(manager.claimUnlaunchedLease(node));
    }

    @Test
    void launchedLeaseCannotBeClaimedByShutdownCleanup() throws Exception {
        final RemoteNodeState node = new RemoteNodeState(0, null, List.of());
        final RuntimeLeaseManager manager = new RuntimeLeaseManager(null, null, List.of(node), null);
        node.leaseActive(true);

        final CompletableFuture<SshResponse> command = CompletableFuture.completedFuture(null);
        assertSame(command, manager.launch(node, () -> command));

        assertTrue(manager.isLaunched(node));
        assertFalse(manager.claimUnlaunchedLease(node));
        assertTrue(node.leaseActive());
    }

    @Test
    void failedCommandStartLeavesLeaseAvailableForShutdownCleanup() {
        final RemoteNodeState node = new RemoteNodeState(0, null, List.of());
        final RuntimeLeaseManager manager = new RuntimeLeaseManager(null, null, List.of(node), null);
        node.leaseActive(true);

        assertThrows(ConnectException.class, () -> manager.launch(node, () -> {
            throw new ConnectException("channel unavailable");
        }));

        assertFalse(manager.isLaunched(node));
        assertTrue(manager.claimUnlaunchedLease(node));
    }
}
