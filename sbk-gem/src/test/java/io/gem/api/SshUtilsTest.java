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

import org.apache.sshd.client.SshClient;
import org.apache.sshd.client.session.ClientSession;
import org.apache.sshd.common.config.keys.PublicKeyEntry;
import org.apache.sshd.common.keyprovider.KeyPairProvider;
import org.apache.sshd.server.SshServer;
import org.apache.sshd.server.auth.pubkey.KeySetPublickeyAuthenticator;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.GeneralSecurityException;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Integration tests for SBK-GEM SSH authentication and host-key verification.
 */
final class SshUtilsTest {
    private static final String USER = "sbk-test";

    @TempDir
    private Path temporaryDirectory;

    private SshServer server;
    private KeyPair hostKey;
    private KeyPair alternateHostKey;
    private KeyPair userKey;

    @BeforeEach
    void startServer() throws IOException, GeneralSecurityException {
        hostKey = generateRsaKey();
        alternateHostKey = generateEcKey();
        userKey = generateRsaKey();
        server = SshServer.setUpDefaultServer();
        server.setHost("127.0.0.1");
        server.setPort(0);
        server.setKeyPairProvider(KeyPairProvider.wrap(alternateHostKey, hostKey));
        server.setPublickeyAuthenticator(new KeySetPublickeyAuthenticator("sbk-test-keys",
                List.of(userKey.getPublic())));
        server.start();
    }

    @AfterEach
    void stopServer() throws IOException {
        if (server != null) {
            server.stop(true);
        }
    }

    @Test
    void authenticatesWithPublicKeyAndNoPassword() throws IOException {
        final Path knownHosts = writeKnownHosts(hostKey);
        final ConnectionConfig config = connectionConfig(knownHosts);
        try (SshClient client = SshUtils.createClient(config)) {
            client.addPublicKeyIdentity(userKey);
            client.start();
            try (ClientSession session = SshUtils.createSession(client, config, 5)) {
                assertTrue(session.isAuthenticated());
            }
        }
    }

    @Test
    void rejectsServerMissingFromKnownHosts() throws IOException {
        final Path knownHosts = temporaryDirectory.resolve("empty-known-hosts");
        Files.createFile(knownHosts);
        final ConnectionConfig config = connectionConfig(knownHosts);
        try (SshClient client = SshUtils.createClient(config)) {
            client.addPublicKeyIdentity(userKey);
            client.start();
            assertThrows(IOException.class, () -> SshUtils.createSession(client, config, 5));
        }
    }

    private Path writeKnownHosts(KeyPair keyPair) throws IOException {
        final Path knownHosts = temporaryDirectory.resolve("known-hosts");
        final String host = "[127.0.0.1]:" + server.getPort();
        Files.writeString(knownHosts, host + " " + PublicKeyEntry.toString(keyPair.getPublic()) + System.lineSeparator());
        return knownHosts;
    }

    private ConnectionConfig connectionConfig(Path knownHosts) {
        return new ConnectionConfig("127.0.0.1", USER, "", server.getPort(), temporaryDirectory.toString(), true,
                knownHosts.toString());
    }

    private static KeyPair generateRsaKey() throws GeneralSecurityException {
        final KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
        generator.initialize(2048);
        return generator.generateKeyPair();
    }

    private static KeyPair generateEcKey() throws GeneralSecurityException {
        final KeyPairGenerator generator = KeyPairGenerator.getInstance("EC");
        generator.initialize(256);
        return generator.generateKeyPair();
    }
}
