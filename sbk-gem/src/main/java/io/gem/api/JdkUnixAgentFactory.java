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

import io.sbk.system.Printer;
import org.apache.sshd.agent.SshAgent;
import org.apache.sshd.agent.SshAgentFactory;
import org.apache.sshd.agent.SshAgentServer;
import org.apache.sshd.common.FactoryManager;
import org.apache.sshd.common.channel.ChannelFactory;
import org.apache.sshd.common.session.ConnectionService;
import org.apache.sshd.common.session.Session;

import java.io.IOException;
import java.util.Collections;
import java.util.List;

/**
 * Authentication-only SSH-agent factory using the JDK Unix-domain socket implementation.
 *
 * <p>An unavailable or stale agent socket returns no agent so Apache SSHD can continue with configured and default
 * private-key files. Agent forwarding is intentionally not enabled.
 */
final class JdkUnixAgentFactory implements SshAgentFactory {
    private final String socketPath;

    /**
     * Create a factory for a specific OpenSSH agent socket.
     *
     * @param socketPath Unix-domain socket path
     */
    JdkUnixAgentFactory(String socketPath) {
        this.socketPath = socketPath;
    }

    @Override
    public List<ChannelFactory> getChannelForwardingFactories(FactoryManager manager) {
        return Collections.emptyList();
    }

    @Override
    public SshAgent createClient(Session session, FactoryManager manager) {
        try {
            return new JdkUnixAgent(socketPath);
        } catch (IOException | RuntimeException ex) {
            Printer.log.warn("SBK-GEM: Unable to use SSH_AUTH_SOCK '" + socketPath + "': " + ex.getMessage() +
                    "; continuing with SSH key files and password authentication");
            return null;
        }
    }

    @Override
    public SshAgentServer createServer(ConnectionService service) throws IOException {
        return null;
    }
}
