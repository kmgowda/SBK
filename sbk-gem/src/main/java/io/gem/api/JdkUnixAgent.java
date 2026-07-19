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

import org.apache.sshd.agent.common.AbstractAgentProxy;
import org.apache.sshd.common.SshException;
import org.apache.sshd.common.util.buffer.Buffer;
import org.apache.sshd.common.util.buffer.ByteArrayBuffer;

import java.io.EOFException;
import java.io.IOException;
import java.net.StandardProtocolFamily;
import java.net.UnixDomainSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.nio.file.Path;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * OpenSSH-agent protocol client backed by the JDK Unix-domain socket API.
 *
 * <p>Apache SSHD handles the agent protocol payload through {@link AbstractAgentProxy}; this class only transports
 * length-prefixed request and response frames over {@code SSH_AUTH_SOCK}. It avoids Apache SSHD's optional APR native
 * transport and therefore needs no additional native library.
 */
final class JdkUnixAgent extends AbstractAgentProxy {
    private static final int MAX_RESPONSE_SIZE = 16 * 1024 * 1024;

    private final SocketChannel channel;
    private final AtomicBoolean open;

    /**
     * Connect to an OpenSSH agent socket.
     *
     * @param socketPath Unix-domain socket path from {@code SSH_AUTH_SOCK}
     * @throws IOException if the socket cannot be opened
     */
    JdkUnixAgent(String socketPath) throws IOException {
        super(null);
        channel = SocketChannel.open(StandardProtocolFamily.UNIX);
        open = new AtomicBoolean(true);
        try {
            channel.connect(UnixDomainSocketAddress.of(Path.of(socketPath)));
        } catch (IOException | RuntimeException ex) {
            open.set(false);
            try {
                channel.close();
            } catch (IOException closeException) {
                ex.addSuppressed(closeException);
            }
            throw ex;
        }
    }

    @Override
    public boolean isOpen() {
        return open.get() && channel.isOpen();
    }

    @Override
    public void close() throws IOException {
        if (open.getAndSet(false)) {
            channel.close();
        }
        super.close();
    }

    @Override
    protected synchronized Buffer request(Buffer buffer) throws IOException {
        if (!isOpen()) {
            throw new SshException("SSH agent connection is closed");
        }

        writeFully(ByteBuffer.wrap(buffer.getCompactData()));
        final ByteBuffer sizeBuffer = ByteBuffer.allocate(Integer.BYTES);
        readFully(sizeBuffer);
        sizeBuffer.flip();
        final int responseSize = sizeBuffer.getInt();
        if (responseSize < 1 || responseSize > MAX_RESPONSE_SIZE) {
            throw new SshException("Invalid SSH agent response size: " + responseSize);
        }

        final ByteBuffer response = ByteBuffer.allocate(responseSize);
        readFully(response);
        return new ByteArrayBuffer(response.array());
    }

    private void writeFully(ByteBuffer buffer) throws IOException {
        while (buffer.hasRemaining()) {
            channel.write(buffer);
        }
    }

    private void readFully(ByteBuffer buffer) throws IOException {
        while (buffer.hasRemaining()) {
            if (channel.read(buffer) < 0) {
                throw new EOFException("SSH agent closed the socket before sending a complete response");
            }
        }
    }
}
