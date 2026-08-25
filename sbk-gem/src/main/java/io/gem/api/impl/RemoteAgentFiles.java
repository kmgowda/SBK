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
    private static final String SHA_256 = "SHA-256";
    private static final int BUFFER_SIZE = 64 * 1024;

    private RemoteAgentFiles() {
    }

    static String install(java.nio.file.FileSystem fileSystem, String parentDirectory, Path localAgent,
                          String version) throws IOException {
        final Path parent = fileSystem.getPath(parentDirectory);
        Files.createDirectories(parent);
        final Path destination = parent.resolve(".sbk-gem-agent-" + version + ".jar");
        final Path marker = parent.resolve(".sbk-gem-agent-" + version + ".sha256");
        final String digest = sha256(localAgent);
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
            digest = MessageDigest.getInstance(SHA_256);
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
}
