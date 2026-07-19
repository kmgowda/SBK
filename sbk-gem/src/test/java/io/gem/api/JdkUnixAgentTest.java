/**
 * Copyright (c) KMG. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.gem.api;

import org.apache.sshd.agent.SshAgentConstants;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.OS;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.net.StandardProtocolFamily;
import java.net.UnixDomainSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.nio.file.Path;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertFalse;

/**
 * Verifies the JDK Unix-domain transport used for OpenSSH agent authentication.
 */
@EnabledOnOs({OS.LINUX, OS.MAC})
final class JdkUnixAgentTest {

    @TempDir
    private Path temporaryDirectory;

    @Test
    void readsIdentitiesFromAgentSocket() throws Exception {
        final Path socketPath = temporaryDirectory.resolve("agent.sock");
        try (ServerSocketChannel server = ServerSocketChannel.open(StandardProtocolFamily.UNIX);
             ExecutorService executor = Executors.newSingleThreadExecutor()) {
            server.bind(UnixDomainSocketAddress.of(socketPath));
            final Future<?> response = executor.submit(() -> serveEmptyIdentities(server));
            try (JdkUnixAgent agent = new JdkUnixAgent(socketPath.toString())) {
                assertFalse(agent.getIdentities().iterator().hasNext());
            }
            response.get(5, TimeUnit.SECONDS);
        }
    }

    private static void serveEmptyIdentities(ServerSocketChannel server) {
        try (SocketChannel socket = server.accept()) {
            final ByteBuffer request = ByteBuffer.allocate(Integer.BYTES + 1);
            readFully(socket, request);
            request.flip();
            if (request.getInt() != 1 || request.get() != SshAgentConstants.SSH2_AGENTC_REQUEST_IDENTITIES) {
                throw new IOException("Unexpected SSH-agent request");
            }
            final ByteBuffer reply = ByteBuffer.allocate(Integer.BYTES + 1 + Integer.BYTES);
            reply.putInt(1 + Integer.BYTES);
            reply.put((byte) SshAgentConstants.SSH2_AGENT_IDENTITIES_ANSWER);
            reply.putInt(0);
            reply.flip();
            while (reply.hasRemaining()) {
                socket.write(reply);
            }
        } catch (IOException ex) {
            throw new IllegalStateException(ex);
        }
    }

    private static void readFully(SocketChannel socket, ByteBuffer buffer) throws IOException {
        while (buffer.hasRemaining()) {
            if (socket.read(buffer) < 0) {
                throw new IOException("Agent test client closed unexpectedly");
            }
        }
    }
}
