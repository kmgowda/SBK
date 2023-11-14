/**
 * Copyright (c) KMG. All Rights Reserved..
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.gem.api.impl;

import io.gem.api.ConnectionConfig;
import io.gem.api.SshUtils;
import io.sbk.system.Printer;
import lombok.Synchronized;
import org.apache.sshd.client.SshClient;
import org.apache.sshd.client.session.ClientSession;

import javax.annotation.concurrent.GuardedBy;
import java.io.IOException;
import java.net.ConnectException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;

/**
 * Class SbkSsh.
 */
final public class SshSession {

    /**
     * <code>public SshConnection connection</code>.
     */
    final public ConnectionConfig connection;

    /**
     * <code>private SshClient client</code>.
     */
    final private SshClient client;

    /**
     * <code>ExecutorService executor</code>.
     */
    final private ExecutorService executor;


    /**
     * <code>ClientSession session</code>.
     */
    @GuardedBy("this")
    private ClientSession session;

    /**
     * This Constructor initializes all values.
     *
     * @param conn      SshConnection
     * @param executor  ExecutorService
     */
    public SshSession(ConnectionConfig conn, ExecutorService executor) {
        this.connection = conn;
        this.executor = executor;
        this.client = SshClient.setUpDefaultClient();
    }

    @Synchronized
    private void creationSession(long timeoutSeconds) {
        Printer.log.info("SBK-GEM: Ssh Connection to host '" + connection.getHost() + "' starting...");
        try {
            client.start();
            session = SshUtils.createSession(client, connection, timeoutSeconds);
            Printer.log.info("SBK-GEM: Ssh Connection to host '" + connection.getHost() + "' Success.");
        } catch (IOException e) {
            Printer.log.error("SBK-GEM: Ssh Connection to host '" + connection.getHost() + "' time out!");
            session = null;
        }
    }


    /**
     * This method Creates Sessions.
     *
     * @param timeoutSeconds long
     * @return CompletableFuture
     */
    public CompletableFuture<Void> createSessionAsync(long timeoutSeconds) {
        return CompletableFuture.runAsync(() -> creationSession(timeoutSeconds), executor);
    }

    @Synchronized
    private ClientSession getSession() throws ConnectException {
        if (session == null) {
            String errMgs = "ssh session to host: " + connection.getHost() + " not found!";
            throw new ConnectException(errMgs);
        }
        return session;
    }


    /**
     * This method is responsible for running commands but throws ConnectException if it occurs.
     *
     * @param cmd               String
     * @param timeoutSeconds    long
     * @param response          SshResponseStream
     * @return CompletableFuture
     * @throws ConnectException If connection exception occurs.
     */
    public CompletableFuture<Void> runCommandAsync(String cmd, long timeoutSeconds, SshResponseStream response)
            throws ConnectException {
        final ClientSession sshSession = getSession();
        return CompletableFuture.runAsync(() -> {
            try {
                SshUtils.runCommand(sshSession, cmd, timeoutSeconds, response);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }, executor);
    }

    /**
     * It copies directory of sessions but throws ConnectException if it occurs.
     *
     * @param srcPath   String
     * @param dstPath   String
     * @return CompletableFuture
     * @throws ConnectException If connection exception occurs.
     */
    public CompletableFuture<Void> copyDirectoryAsync(String srcPath, String dstPath) throws ConnectException {
        final ClientSession sshSession = getSession();
        return CompletableFuture.runAsync(() -> {
            try {
                SshUtils.copyDirectory(sshSession, srcPath, dstPath);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }, executor);
    }


    @Synchronized
    private void closeSession() {
        if (session != null) {
            session.close(true);
            session = null;
        }
    }


    /**
     * This method is responsible for closing session and stopping the client.
     */
    public void stop() {
        closeSession();
        client.stop();
    }


}
