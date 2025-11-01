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

import org.apache.commons.lang3.StringUtils;
import org.apache.sshd.client.SshClient;
import org.apache.sshd.client.channel.ChannelExec;
import org.apache.sshd.client.channel.ClientChannelEvent;
import org.apache.sshd.client.future.ConnectFuture;
import org.apache.sshd.client.session.ClientSession;
import org.apache.sshd.scp.client.ScpClient;
import org.apache.sshd.scp.client.ScpClientCreator;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.util.EnumSet;
import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * SSH utility methods for session creation, command execution, and SCP copy.
 *
 * <p>Wraps Apache Mina SSHD primitives to provide simpler operations used by SBK-GEM
 * for orchestrating remote SBK runs.
 */
public final class SshUtils {

    /**
     * Create and authenticate an SSH {@link ClientSession}.
     *
     * @param client         SSH client (must be started by caller)
     * @param connConfig     connection details (host, user, password, port)
     * @param timeoutSeconds authentication timeout in seconds
     * @return authenticated session (caller is responsible for closing it)
     * @throws IOException on connection or authentication failure
     */
    public static ClientSession createSession(SshClient client, ConnectionConfig connConfig, long timeoutSeconds)
            throws IOException {
        // Connect to the server
        final ConnectFuture cf = client.connect(connConfig.getUserName(), connConfig.getHost(), connConfig.getPort());
        final ClientSession session = cf.verify().getSession();

        if (StringUtils.isNotEmpty(connConfig.getPassword())) {
            session.addPasswordIdentity(connConfig.getPassword());
            session.auth().verify(TimeUnit.SECONDS.toMillis(timeoutSeconds));
        }
        return session;
    }

    /**
     * Execute a command over SSH, wiring stdout/stderr to the provided response streams.
     *
     * @param session        non-null SSH session
     * @param cmd            command to execute
     * @param timeoutSeconds execution timeout in seconds
     * @param response       non-null holder to capture stdout/stderr and exit status
     * @throws IOException on timeout or channel errors
     */
    public static void runCommand(final @NotNull ClientSession session, String cmd, long timeoutSeconds,
                                  @NotNull SshResponse response) throws IOException {
        // Create the exec and channel its output/error streams
        final ChannelExec execChannel = session.createExecChannel(cmd);
        execChannel.setErr(response.errOutputStream);
        execChannel.setOut(response.stdOutputStream);

        // Execute and wait
        execChannel.open();
        final Set<?> events = execChannel.waitFor(EnumSet.of(ClientChannelEvent.CLOSED),
                TimeUnit.SECONDS.toMillis(timeoutSeconds));

        // Check if timed out
        if (events.contains(ClientChannelEvent.TIMEOUT)) {
            throw new IOException("The cmd: " + cmd + " timeout !");
        }

        if (session.isOpen()) {
            response.returnCode = execChannel.getExitStatus();
            execChannel.close(true);
        }
    }

    /**
     * Recursively copy a directory to a remote path using SCP.
     *
     * @param session SSH session
     * @param srcPath local source path
     * @param dstPath remote destination path
     * @throws IOException on copy failure
     */
    public static void copyDirectory(final ClientSession session, String srcPath,
                                     String dstPath) throws IOException {

        final ScpClientCreator creator = ScpClientCreator.instance();
        final ScpClient scpClient = creator.createScpClient(session);

        scpClient.upload(srcPath, dstPath, ScpClient.Option.Recursive, ScpClient.Option.PreserveAttributes,
                ScpClient.Option.TargetIsDirectory);
    }

}
