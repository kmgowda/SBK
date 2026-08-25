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

import java.util.HashMap;
import java.util.Map;

/** Maps logical SBK clients to unique physical remote deployment targets. */
final class RemoteTargetPlan {
    private final int[] representatives;

    private RemoteTargetPlan(int[] representatives) {
        this.representatives = representatives;
    }

    /**
     * Build a target plan from resolved absolute remote directories.
     *
     * @param connections SSH connections
     * @param endpointIdentities authenticated network endpoint identities corresponding to the connections
     * @param directories resolved absolute directories corresponding to the connections
     * @return immutable target plan
     * @throws IllegalArgumentException when the array lengths differ
     */
    static RemoteTargetPlan create(ConnectionConfig[] connections, String[] endpointIdentities,
                                   String[] directories) {
        if (connections.length != endpointIdentities.length || connections.length != directories.length) {
            throw new IllegalArgumentException("Connection, endpoint-identity, and remote-directory counts "
                    + "must match");
        }
        final int[] representatives = new int[connections.length];
        final Map<RemoteTarget, Integer> firstByTarget = new HashMap<>();
        for (int index = 0; index < connections.length; index++) {
            final int currentIndex = index;
            final RemoteTarget target = target(connections[index], endpointIdentities[index], directories[index]);
            representatives[index] = firstByTarget.computeIfAbsent(target, ignored -> currentIndex);
        }
        return new RemoteTargetPlan(representatives);
    }

    /**
     * Return whether an index owns physical deployment work for its target.
     *
     * @param index logical node index
     * @return true for the first logical node using the physical target
     */
    boolean isRepresentative(int index) {
        return representatives[index] == index;
    }

    /**
     * Return the physical-target representative for a logical node.
     *
     * @param index logical node index
     * @return representative node index
     */
    int representative(int index) {
        return representatives[index];
    }

    /**
     * Return whether any logical node for this representative is selected.
     *
     * @param representative representative index
     * @param selected logical-node selection flags
     * @return true when the physical target requires the selected operation
     */
    boolean hasSelectedNode(int representative, boolean[] selected) {
        for (int index = 0; index < representatives.length; index++) {
            if (representatives[index] == representative && selected[index]) {
                return true;
            }
        }
        return false;
    }

    /**
     * Collapse logical selections onto the representative that performs physical work.
     *
     * @param selected logical-node selection flags
     * @return selection flags containing only physical representatives
     */
    boolean[] representativeSelection(boolean[] selected) {
        final boolean[] representativesSelected = new boolean[representatives.length];
        for (int index = 0; index < representatives.length; index++) {
            if (selected[index]) {
                representativesSelected[representatives[index]] = true;
            }
        }
        return representativesSelected;
    }

    static RemoteTarget target(ConnectionConfig connection, String endpointIdentity, String directory) {
        return new RemoteTarget(connection.getUserName(), endpointIdentity, connection.getPort(), directory);
    }

    record RemoteTarget(String user, String host, int port, String directory) {
    }
}
