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

import io.gem.api.RemoteResponse;
import io.gem.api.SshSession;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/** Holds the controller-side lifecycle state for one configured remote node. */
final class RemoteNodeState {
    private final int index;
    private final SshSession session;
    private final List<String> sbkArguments;

    private String endpointIdentity;
    private String connectionDirectory;
    private String javaHome;
    private String agentPath;
    private String deploymentDirectory;
    private String deploymentName;
    private String leaseId;
    private boolean leaseLaunched;
    private boolean leaseActive;
    private CompletableFuture<?> leaseHeartbeat;
    private RemoteResponse result;

    RemoteNodeState(int index, SshSession session, List<String> sbkArguments) {
        this.index = index;
        this.session = session;
        this.sbkArguments = new ArrayList<>(sbkArguments);
    }

    int index() {
        return index;
    }

    SshSession session() {
        return session;
    }

    List<String> sbkArguments() {
        return sbkArguments;
    }

    String host() {
        return session.connection.getHost();
    }

    String hostAndPort() {
        return session.connection.getHost() + ":" + session.connection.getPort();
    }

    String endpointIdentity() {
        return endpointIdentity;
    }

    void endpointIdentity(String value) {
        endpointIdentity = value;
    }

    String connectionDirectory() {
        return connectionDirectory;
    }

    void connectionDirectory(String value) {
        connectionDirectory = value;
    }

    String javaHome() {
        return javaHome;
    }

    void javaHome(String value) {
        javaHome = value;
    }

    String agentPath() {
        return agentPath;
    }

    void agentPath(String value) {
        agentPath = value;
    }

    String deploymentDirectory() {
        return deploymentDirectory;
    }

    void deploymentDirectory(String value) {
        deploymentDirectory = value;
    }

    String deploymentName() {
        return deploymentName;
    }

    void deploymentName(String value) {
        deploymentName = value;
    }

    String leaseId() {
        return leaseId;
    }

    void leaseId(String value) {
        leaseId = value;
    }

    boolean leaseLaunched() {
        return leaseLaunched;
    }

    void leaseLaunched(boolean value) {
        leaseLaunched = value;
    }

    boolean leaseActive() {
        return leaseActive;
    }

    void leaseActive(boolean value) {
        leaseActive = value;
    }

    CompletableFuture<?> leaseHeartbeat() {
        return leaseHeartbeat;
    }

    void leaseHeartbeat(CompletableFuture<?> value) {
        leaseHeartbeat = value;
    }

    RemoteResponse result() {
        return result;
    }

    void result(RemoteResponse value) {
        result = value;
    }
}
