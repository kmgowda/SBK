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
import org.apache.sshd.common.file.virtualfs.VirtualFileSystemFactory;
import org.apache.sshd.common.keyprovider.KeyPairProvider;
import org.apache.sshd.server.SshServer;
import org.apache.sshd.server.auth.pubkey.KeySetPublickeyAuthenticator;
import org.apache.sshd.sftp.server.SftpSubsystemFactory;
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
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

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
        server.setPasswordAuthenticator((username, password, session) ->
                USER.equals(username) && "sftp-password".equals(password));
        server.setFileSystemFactory(new VirtualFileSystemFactory(temporaryDirectory));
        server.setSubsystemFactories(List.of(new SftpSubsystemFactory.Builder().build()));
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

    @Test
    void executesRemoteFileOperationsThroughApacheMinaSftp() throws Exception {
        final Path knownHosts = writeKnownHosts(hostKey);
        final ConnectionConfig config = new ConnectionConfig("127.0.0.1", USER, "sftp-password",
                server.getPort(), temporaryDirectory.toString(), true, knownHosts.toString());
        final var executor = Executors.newFixedThreadPool(2);
        final SshSession sshSession = new SshSession(config, executor);
        try {
            sshSession.createSessionAsync(5).get(5, TimeUnit.SECONDS);
            final String result = sshSession.runRemoteFileOperationAsync(fileSystem -> {
                final Path directory = fileSystem.getPath("/runtime-leases");
                Files.createDirectories(directory);
                final Path marker = directory.resolve("marker");
                Files.writeString(marker, "active");
                return Files.readString(marker);
            }, 5).get(5, TimeUnit.SECONDS);

            assertEquals("active", result);
            assertEquals("active", Files.readString(temporaryDirectory.resolve("runtime-leases/marker")));
        } finally {
            sshSession.stop();
            executor.shutdownNow();
        }
    }

    @Test
    void activelyCancelsTimedOutSftpOperationAndKeepsSessionUsable() throws Exception {
        final Path knownHosts = writeKnownHosts(hostKey);
        final ConnectionConfig config = new ConnectionConfig("127.0.0.1", USER, "sftp-password",
                server.getPort(), temporaryDirectory.toString(), true, knownHosts.toString());
        final var executor = Executors.newFixedThreadPool(2);
        final SshSession sshSession = new SshSession(config, executor);
        final CountDownLatch started = new CountDownLatch(1);
        final CountDownLatch interrupted = new CountDownLatch(1);
        try {
            sshSession.createSessionAsync(5).get(5, TimeUnit.SECONDS);
            final var timedOut = sshSession.runRemoteFileOperationAsync(fileSystem -> {
                started.countDown();
                try {
                    new CountDownLatch(1).await();
                } catch (InterruptedException exception) {
                    interrupted.countDown();
                    throw exception;
                }
                return null;
            }, 1);

            assertTrue(started.await(2, TimeUnit.SECONDS));
            final ExecutionException failure = assertThrows(ExecutionException.class,
                    () -> timedOut.get(3, TimeUnit.SECONDS));
            assertInstanceOf(TimeoutException.class, failure.getCause());
            assertTrue(interrupted.await(2, TimeUnit.SECONDS));

            final String result = sshSession.runRemoteFileOperationAsync(fileSystem -> "available", 5)
                    .get(5, TimeUnit.SECONDS);
            assertEquals("available", result);
        } finally {
            sshSession.stop();
            executor.shutdownNow();
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
