/**
 * Copyright (c) KMG. All Rights Reserved..
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.gem.api;

import org.apache.sshd.client.SshClient;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;

/**
 * Owns the small set of Apache SSHD clients shared by compatible SBK-GEM connections.
 *
 * <p>Each remote node still receives an independent authenticated {@code ClientSession} and TCP connection.
 * Compatible nodes share only client-level MINA connector, scheduler, and lifecycle infrastructure. Connections
 * with different host-key or authentication policies never share a client.
 */
public final class SshClientManager implements AutoCloseable {
    private final Map<SshClientPolicy, SshClient> clients = new LinkedHashMap<>();
    private boolean closed;

    /**
     * Create an empty manager that starts shared SSH clients on demand.
     */
    public SshClientManager() {
    }

    /**
     * Return the started client for a connection's immutable security policy.
     *
     * @param connection remote connection configuration
     * @return shared, started SSH client
     * @throws IllegalStateException when the manager is already closed
     */
    synchronized SshClient clientFor(ConnectionConfig connection) {
        if (closed) {
            throw new IllegalStateException("SSH client manager is already closed");
        }
        final SshClientPolicy policy = SshUtils.clientPolicy(connection);
        return clients.computeIfAbsent(policy, key -> {
            final SshClient client = SshUtils.createClient(key);
            client.start();
            return client;
        });
    }

    /**
     * Create an independently managed node session backed by the appropriate shared client.
     *
     * @param connection remote connection configuration
     * @param controlExecutor bounded connection and control-operation executor
     * @param transferExecutor bounded deployment transfer executor
     * @param commandExecutor virtual-thread executor for long-running remote commands
     * @param diagnosticBytes maximum stdout/stderr bytes retained per command
     * @param copyBufferBytes read buffer bytes used by each bulk SCP upload
     * @return independent node session
     */
    public synchronized SshSession sessionFor(ConnectionConfig connection, ExecutorService controlExecutor,
                                              ExecutorService transferExecutor, ExecutorService commandExecutor,
                                              int diagnosticBytes, int copyBufferBytes) {
        return new SshSession(connection, clientFor(connection), controlExecutor, transferExecutor,
                commandExecutor, diagnosticBytes, copyBufferBytes);
    }

    /**
     * Return the number of distinct client policy groups, primarily for lifecycle diagnostics and tests.
     *
     * @return managed SSH client count
     */
    public synchronized int size() {
        return clients.size();
    }

    /** Stop every managed client after its node sessions have closed. */
    @Override
    public synchronized void close() {
        if (closed) {
            return;
        }
        closed = true;
        RuntimeException failure = null;
        for (SshClient client : clients.values()) {
            try {
                client.stop();
            } catch (RuntimeException exception) {
                if (failure == null) {
                    failure = exception;
                } else {
                    failure.addSuppressed(exception);
                }
            }
        }
        clients.clear();
        if (failure != null) {
            throw failure;
        }
    }
}
