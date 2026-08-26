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

import io.gem.api.ConnectionConfig;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Tests logical-node grouping by resolved physical deployment target. */
final class RemoteTargetPlanTest {
    @Test
    void deduplicatesHostCaseButPreservesRemotePathCase() {
        final ConnectionConfig[] connections = {
                connection("NODE-A", "user"), connection("node-a", "user"), connection("node-a", "user")
        };
        final RemoteTargetPlan plan = RemoteTargetPlan.create(connections,
                new String[]{"192.0.2.10", "192.0.2.10", "192.0.2.10"},
                new String[]{"/srv/SBK", "/srv/SBK", "/srv/sbk"});

        assertTrue(plan.isRepresentative(0));
        assertFalse(plan.isRepresentative(1));
        assertEquals(0, plan.representative(1));
        assertTrue(plan.isRepresentative(2));
    }

    @Test
    void keepsDifferentRemoteUsersAsIndependentTargets() {
        final ConnectionConfig[] connections = {connection("node-a", "first"), connection("node-a", "second")};
        final RemoteTargetPlan plan = RemoteTargetPlan.create(connections,
                new String[]{"192.0.2.10", "192.0.2.10"}, new String[]{"/srv/sbk", "/srv/sbk"});

        assertTrue(plan.isRepresentative(0));
        assertTrue(plan.isRepresentative(1));
    }

    @Test
    void representativeOwnsWorkSelectedByAnyLogicalNode() {
        final ConnectionConfig[] connections = {connection("node-a", "user"), connection("node-a", "user")};
        final RemoteTargetPlan plan = RemoteTargetPlan.create(connections,
                new String[]{"192.0.2.10", "192.0.2.10"}, new String[]{"/srv/sbk", "/srv/sbk"});

        assertTrue(plan.hasSelectedNode(0, new boolean[]{false, true}));
        final boolean[] physicalSelection = plan.representativeSelection(new boolean[]{false, true});
        assertTrue(physicalSelection[0]);
        assertFalse(physicalSelection[1]);
    }

    @Test
    void deduplicatesHostAliasesConnectedToTheSameEndpoint() {
        final ConnectionConfig[] connections = {
                connection("127.0.0.1", "user"), connection("localhost", "user")
        };
        final RemoteTargetPlan plan = RemoteTargetPlan.create(connections,
                new String[]{"127.0.0.1", "127.0.0.1"}, new String[]{"/srv/sbk", "/srv/sbk"});

        assertTrue(plan.isRepresentative(0));
        assertFalse(plan.isRepresentative(1));
        assertEquals(0, plan.representative(1));
    }

    @Test
    void deduplicatesEquivalentTargetsBeforeDirectoryResolution() {
        final ConnectionConfig[] connections = {
                connection("node-a", "user"), connection("node-alias", "user")
        };

        final RemoteTargetPlan plan = RemoteTargetPlan.createBeforeDirectoryResolution(connections,
                new String[]{"192.0.2.10", "192.0.2.10"});

        assertTrue(plan.isRepresentative(0));
        assertFalse(plan.isRepresentative(1));
        assertEquals(0, plan.representative(1));
    }

    private static ConnectionConfig connection(String host, String user) {
        return new ConnectionConfig(host, user, "", 22, "/configured", true, "");
    }
}
