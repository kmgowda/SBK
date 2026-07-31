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
import org.apache.sshd.agent.SshAgent;
import org.apache.sshd.client.SshClient;
import org.apache.sshd.client.channel.ChannelExec;
import org.apache.sshd.client.channel.ClientChannelEvent;
import org.apache.sshd.client.config.hosts.KnownHostEntry;
import org.apache.sshd.client.future.ConnectFuture;
import org.apache.sshd.client.keyverifier.AcceptAllServerKeyVerifier;
import org.apache.sshd.client.keyverifier.KnownHostsServerKeyVerifier;
import org.apache.sshd.client.keyverifier.RejectAllServerKeyVerifier;
import org.apache.sshd.client.session.ClientSession;
import org.apache.sshd.common.NamedFactory;
import org.apache.sshd.common.config.keys.KeyUtils;
import org.apache.sshd.common.signature.Signature;
import org.apache.sshd.scp.client.ScpClient;
import org.apache.sshd.scp.client.ScpClientCreator;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.net.SocketTimeoutException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.List;
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
     * Creates an SSH utility facade for remote SBK orchestration.
     */
    public SshUtils() {
    }

    /**
     * Create an SSH client configured for host-key and optional SSH-agent authentication.
     *
     * @param connConfig connection and host-key policy
     * @return configured, unstarted SSH client
     */
    public static SshClient createClient(ConnectionConfig connConfig) {
        final SshClient client = SshClient.setUpDefaultClient();
        if (connConfig.isHostKeyCheck()) {
            final Path knownHosts = StringUtils.isEmpty(connConfig.getKnownHosts())
                    ? KnownHostEntry.getDefaultKnownHostsFile()
                    : Path.of(connConfig.getKnownHosts());
            client.setServerKeyVerifier(new KnownHostsServerKeyVerifier(RejectAllServerKeyVerifier.INSTANCE,
                    knownHosts));
            preferKnownHostAlgorithms(client, connConfig, knownHosts);
        } else {
            client.setServerKeyVerifier(AcceptAllServerKeyVerifier.INSTANCE);
        }

        final String agentSocket = System.getenv(SshAgent.SSH_AUTHSOCKET_ENV_NAME);
        if (StringUtils.isNotEmpty(agentSocket)) {
            client.setAgentFactory(new JdkUnixAgentFactory(agentSocket));
        }
        return client;
    }

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
        final ClientSession session;
        try {
            final ConnectFuture cf = client.connect(connConfig.getUserName(), connConfig.getHost(),
                    connConfig.getPort());
            session = cf.verify(timeoutSeconds, TimeUnit.SECONDS).getSession();
        } catch (IOException ex) {
            throw new IOException("SSH connection failed: " + ex.getMessage(), ex);
        }

        try {
            if (StringUtils.isNotEmpty(connConfig.getPassword())) {
                session.addPasswordIdentity(connConfig.getPassword());
            }
            session.auth().verify(TimeUnit.SECONDS.toMillis(timeoutSeconds));
        } catch (IOException ex) {
            session.close(true);
            if (hasCauseMessage(ex, "Server key did not validate")) {
                throw new IOException("SSH host key verification failed: " + ex.getMessage(), ex);
            }
            throw new IOException("SSH authentication failed: " + ex.getMessage(), ex);
        }
        return session;
    }

    private static void preferKnownHostAlgorithms(SshClient client, ConnectionConfig connConfig, Path knownHosts) {
        if (!java.nio.file.Files.isRegularFile(knownHosts)) {
            return;
        }
        try {
            final Set<String> knownKeyTypes = new HashSet<>();
            for (KnownHostEntry entry : KnownHostEntry.readKnownHostEntries(knownHosts)) {
                if (entry.isHostMatch(connConfig.getHost(), connConfig.getPort())) {
                    knownKeyTypes.add(KeyUtils.getCanonicalKeyType(entry.getKeyEntry().getKeyType()));
                }
            }
            if (knownKeyTypes.isEmpty()) {
                return;
            }
            final List<NamedFactory<Signature>> signatures = new ArrayList<>(client.getSignatureFactories());
            signatures.sort(Comparator.comparingInt(signature ->
                    knownKeyTypes.contains(KeyUtils.getCanonicalKeyType(signature.getName())) ? 0 : 1));
            client.setSignatureFactories(signatures);
        } catch (IOException ex) {
            // The verifier reports an actionable error when it reads the same file during connection establishment.
        }
    }

    private static boolean hasCauseMessage(Throwable failure, String message) {
        final Set<Throwable> visited = java.util.Collections.newSetFromMap(new IdentityHashMap<>());
        Throwable cause = failure;
        while (cause != null && visited.add(cause)) {
            if (cause.getMessage() != null && cause.getMessage().contains(message)) {
                return true;
            }
            cause = cause.getCause();
        }
        return false;
    }

    /**
     * Execute a command over SSH, wiring stdout/stderr to the provided response streams.
     *
     * @param session        non-null SSH session
     * @param cmd            command to execute
     * @param timeoutSeconds execution timeout in seconds
     * @param response       non-null holder to capture stdout/stderr and exit status
     * @throws IOException on timeout or channel errors
     * @throws SocketTimeoutException when the remote command exceeds its deadline
     */
    public static void runCommand(final @NotNull ClientSession session, String cmd, long timeoutSeconds,
                                  @NotNull SshResponse response) throws IOException {
        try (ChannelExec execChannel = session.createExecChannel(cmd)) {
            execChannel.setErr(response.errOutputStream);
            execChannel.setOut(response.stdOutputStream);
            final long timeoutMillis = TimeUnit.SECONDS.toMillis(timeoutSeconds);
            execChannel.open().verify(timeoutMillis);
            final Set<?> events = execChannel.waitFor(EnumSet.of(ClientChannelEvent.CLOSED), timeoutMillis);
            if (events.contains(ClientChannelEvent.TIMEOUT)) {
                throw new SocketTimeoutException("Remote command timed out after " + timeoutSeconds + " seconds");
            }
            final Integer exitStatus = execChannel.getExitStatus();
            if (exitStatus == null) {
                throw new IOException("Remote command closed without an SSH exit status");
            }
            response.returnCode = exitStatus;
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
