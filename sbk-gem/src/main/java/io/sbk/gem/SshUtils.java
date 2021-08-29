/**
 * Copyright (c) KMG. All Rights Reserved..
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.sbk.gem;

import io.sbk.gem.impl.SshResponseStream;
import org.apache.commons.lang.StringUtils;
import org.apache.sshd.client.SshClient;
import org.apache.sshd.client.channel.ChannelExec;
import org.apache.sshd.client.channel.ClientChannelEvent;
import org.apache.sshd.client.future.ConnectFuture;
import org.apache.sshd.client.session.ClientSession;
import org.apache.sshd.scp.client.ScpClient;
import org.apache.sshd.scp.client.ScpClientCreator;

import java.io.IOException;
import java.util.EnumSet;
import java.util.Set;
import java.util.concurrent.TimeUnit;

public final class SshUtils {

    public static ClientSession createSession(SshClient client, SshConnection conn, long timeoutSeconds)
            throws IOException {
        // Connect to the server
        final ConnectFuture cf = client.connect(conn.getUserName(), conn.getHost(), conn.getPort());
        final ClientSession session = cf.verify().getSession();

        if (StringUtils.isNotEmpty(conn.getPassword())) {
            session.addPasswordIdentity(conn.getPassword());
            session.auth().verify(TimeUnit.SECONDS.toMillis(timeoutSeconds));
        }
        return session;
    }

    public static void runCommand(final ClientSession session, String cmd, long timeoutSeconds,
                                  SshResponseStream response) throws IOException {
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

    public static void copyDirectory(final ClientSession session, String srcPath,
                                     String dstPath) throws IOException {

        final ScpClientCreator creator = ScpClientCreator.instance();
        final ScpClient scpClient = creator.createScpClient(session);

        scpClient.upload(srcPath, dstPath, ScpClient.Option.Recursive, ScpClient.Option.PreserveAttributes,
                ScpClient.Option.TargetIsDirectory);
    }

}
