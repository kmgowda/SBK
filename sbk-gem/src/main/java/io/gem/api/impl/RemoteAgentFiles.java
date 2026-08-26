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

import io.gem.agent.RemoteDeploymentContract;
import io.gem.agent.RemoteRuntimeFiles;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.UUID;

/** Installs the small remote agent JAR through Apache MINA SFTP. */
final class RemoteAgentFiles {
    private static final int BUFFER_SIZE = 64 * 1024;

    private RemoteAgentFiles() {
    }

    static AgentBootstrap prepare(java.nio.file.FileSystem fileSystem, String configuredDirectory, Path localAgent,
                                  String version, String digest) throws IOException {
        final String resolvedDirectory = RemoteRuntimeFiles.resolveDirectory(fileSystem, configuredDirectory);
        final String agentPath = install(fileSystem.getPath(resolvedDirectory), localAgent, version, digest);
        return new AgentBootstrap(resolvedDirectory, agentPath);
    }

    static String digest(Path localAgent) throws IOException {
        return sha256(localAgent);
    }

    private static String install(Path parent, Path localAgent, String version, String digest) throws IOException {
        final Path destination = parent.resolve(".sbk-gem-agent-" + version + ".jar");
        final Path marker = parent.resolve(".sbk-gem-agent-" + version + ".sha256");
        if (Files.isRegularFile(destination) && Files.isRegularFile(marker)
                && digest.equals(Files.readString(marker).trim())) {
            return destination.toString();
        }
        final Path temporary = parent.resolve(destination.getFileName() + "." + UUID.randomUUID());
        try (InputStream input = new BufferedInputStream(Files.newInputStream(localAgent));
             OutputStream output = new BufferedOutputStream(Files.newOutputStream(temporary))) {
            input.transferTo(output);
        }
        Files.move(temporary, destination, StandardCopyOption.REPLACE_EXISTING);
        Files.writeString(marker, digest + System.lineSeparator(), StandardCharsets.UTF_8);
        return destination.toString();
    }

    private static String sha256(Path path) throws IOException {
        final MessageDigest digest;
        try {
            digest = MessageDigest.getInstance(RemoteDeploymentContract.SHA_256);
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException(exception);
        }
        try (InputStream input = new BufferedInputStream(Files.newInputStream(path))) {
            final byte[] buffer = new byte[BUFFER_SIZE];
            int count;
            while ((count = input.read(buffer)) >= 0) {
                digest.update(buffer, 0, count);
            }
        }
        return HexFormat.of().formatHex(digest.digest());
    }

    record AgentBootstrap(String directory, String agentPath) {
    }
}
