/**
 * Copyright (c) KMG. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.gem.api.impl;

import io.gem.api.ConnectionConfig;
import io.gem.api.SshSession;
import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.apache.sshd.common.config.keys.PublicKeyEntry;
import org.apache.sshd.common.file.virtualfs.VirtualFileSystemFactory;
import org.apache.sshd.common.keyprovider.KeyPairProvider;
import org.apache.sshd.server.SshServer;
import org.apache.sshd.scp.server.ScpCommandFactory;
import org.apache.sshd.sftp.server.SftpSubsystemFactory;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.GeneralSecurityException;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Integration coverage for bulk managed-JDK transfer over Apache MINA SCP. */
final class ManagedJavaRuntimeScpTest {
    private static final String USER = "sbk-test";

    @TempDir
    private Path temporaryDirectory;

    private SshServer server;

    @AfterEach
    void stopServer() throws IOException {
        if (server != null) {
            server.stop(true);
        }
    }

    @Test
    void createsAndBulkCopiesOneManagedJdkArchive() throws Exception {
        final Path javaHome = createJavaHome();
        final Path remoteRoot = Files.createDirectories(temporaryDirectory.resolve("remote"));
        final KeyPair hostKey = generateRsaKey();
        startServer(remoteRoot, hostKey);
        final Path knownHosts = temporaryDirectory.resolve("known-hosts");
        Files.writeString(knownHosts, "[127.0.0.1]:" + server.getPort() + " "
                + PublicKeyEntry.toString(hostKey.getPublic()) + System.lineSeparator());
        final ConnectionConfig connection = new ConnectionConfig("127.0.0.1", USER, "password",
                server.getPort(), "/", true, knownHosts.toString());
        final var executor = Executors.newFixedThreadPool(2);
        final SshSession session = new SshSession(connection, executor);
        final ManagedJavaRuntime runtime = ManagedJavaRuntime.create(javaHome, 25,
                Files.createDirectories(temporaryDirectory.resolve("cache")));
        final Path archive = runtime.prepareArchive();
        final long archiveModified = Files.getLastModifiedTime(archive).toMillis();
        assertEquals(archive, runtime.prepareArchive());
        assertEquals(archiveModified, Files.getLastModifiedTime(archive).toMillis());
        final Map<String, TarArchiveEntry> archiveEntries = readArchive(archive);
        assertTrue(archiveEntries.containsKey("bin/java"));
        assertTrue(archiveEntries.containsKey("bin/javac"));
        assertTrue(archiveEntries.containsKey("lib/modules"));
        assertTrue(archiveEntries.get("legal/module-1/COPYRIGHT").isSymbolicLink());
        assertEquals("../module-0/LICENSE", archiveEntries.get("legal/module-1/COPYRIGHT").getLinkName());
        final AtomicLong copiedBytes = new AtomicLong();
        try {
            session.createSessionAsync(5).get(5, TimeUnit.SECONDS);
            session.copyFileAsync(archive.toString(), "/jdk.tar", 30, copiedBytes::addAndGet)
                    .get(30, TimeUnit.SECONDS);

            assertEquals(runtime.archiveBytes(), copiedBytes.get());
            assertEquals(-1, Files.mismatch(archive, remoteRoot.resolve("jdk.tar")));
        } finally {
            session.stop();
            executor.shutdownNow();
        }
    }

    private static Map<String, TarArchiveEntry> readArchive(Path archive) throws IOException {
        final Map<String, TarArchiveEntry> entries = new HashMap<>();
        try (InputStream file = Files.newInputStream(archive);
             TarArchiveInputStream input = new TarArchiveInputStream(file)) {
            TarArchiveEntry entry;
            while ((entry = input.getNextEntry()) != null) {
                entries.put(entry.getName(), entry);
            }
        }
        return entries;
    }

    private void startServer(Path remoteRoot, KeyPair hostKey) throws IOException {
        server = SshServer.setUpDefaultServer();
        server.setHost("127.0.0.1");
        server.setPort(0);
        server.setKeyPairProvider(KeyPairProvider.wrap(hostKey));
        server.setPasswordAuthenticator((username, password, session) ->
                USER.equals(username) && "password".equals(password));
        server.setFileSystemFactory(new VirtualFileSystemFactory(remoteRoot));
        server.setCommandFactory(new ScpCommandFactory.Builder().build());
        server.setSubsystemFactories(List.of(new SftpSubsystemFactory.Builder().build()));
        server.start();
    }

    private Path createJavaHome() throws IOException {
        final Path home = Files.createDirectories(temporaryDirectory.resolve("local-jdk"));
        final Path bin = Files.createDirectories(home.resolve("bin"));
        executable(bin.resolve("java"));
        executable(bin.resolve("javac"));
        final Path lib = Files.createDirectories(home.resolve("lib"));
        Files.write(lib.resolve("modules"), new byte[2 * 1024 * 1024 + 17]);
        for (int index = 0; index < 12; index++) {
            final Path legal = Files.createDirectories(home.resolve("legal/module-" + index));
            Files.writeString(legal.resolve("LICENSE"), "license-" + index, StandardCharsets.UTF_8);
        }
        Files.createSymbolicLink(home.resolve("legal/module-1/COPYRIGHT"), Path.of("../module-0/LICENSE"));
        Files.writeString(home.resolve("release"), "JAVA_VERSION=25", StandardCharsets.UTF_8);
        return home;
    }

    private static void executable(Path path) throws IOException {
        Files.writeString(path, "binary", StandardCharsets.UTF_8);
        assertTrue(path.toFile().setExecutable(true));
    }

    private static KeyPair generateRsaKey() throws GeneralSecurityException {
        final KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
        generator.initialize(2048);
        return generator.generateKeyPair();
    }
}
