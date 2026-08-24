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
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.PosixFilePermission;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Comparator;
import java.util.HexFormat;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Stream;

/** Content-addressed controller JDK copied independently from the SBK runtime. */
final class ManagedJavaRuntime {
    private static final String MARKER = ".sbk-java.sha256";
    private static final int BUFFER_SIZE = 64 * 1024;
    private static final int IDENTITY_CHARACTERS = 24;
    private final Path localHome;
    private final String digest;
    private final String directoryName;

    private ManagedJavaRuntime(Path localHome, String digest, String directoryName) {
        this.localHome = localHome;
        this.digest = digest;
        this.directoryName = directoryName;
    }

    static ManagedJavaRuntime create(Path javaHome, int major) throws IOException {
        final Path home = javaHome.toAbsolutePath().normalize();
        if (!Files.isExecutable(home.resolve("bin/java")) || !Files.isExecutable(home.resolve("bin/javac"))) {
            throw new IOException("Controller JDK is incomplete: " + home);
        }
        final MessageDigest digest = newDigest();
        try (Stream<Path> entries = Files.walk(home)) {
            for (Path path : entries.sorted(Comparator.comparing(Path::toString)).toList()) {
                final String relative = home.relativize(path).toString().replace('\\', '/');
                update(digest, relative + "\0");
                if (Files.isSymbolicLink(path)) {
                    update(digest, "L\0" + Files.readSymbolicLink(path) + "\0");
                } else if (Files.isDirectory(path, LinkOption.NOFOLLOW_LINKS)) {
                    update(digest, "D\0");
                } else if (Files.isRegularFile(path, LinkOption.NOFOLLOW_LINKS)) {
                    update(digest, "F\0" + Files.size(path) + "\0");
                    hashFile(digest, path);
                }
                if (!Files.isSymbolicLink(path)) {
                    update(digest, "P\0" + permissionIdentity(path) + "\0");
                }
            }
        }
        final String identity = HexFormat.of().formatHex(digest.digest());
        return new ManagedJavaRuntime(home, identity,
                "sbk-java-" + major + "-" + identity.substring(0, IDENTITY_CHARACTERS));
    }

    String directoryName() {
        return directoryName;
    }

    String install(java.nio.file.FileSystem fileSystem, String parentDirectory) throws IOException {
        final Path parent = fileSystem.getPath(parentDirectory);
        final Path destination = parent.resolve(directoryName);
        final Path marker = destination.resolve(MARKER);
        if (hasUsableExpectedIdentity(destination)) {
            return destination.toString();
        }
        retireInvalidIdentity(destination);
        final Path staging = parent.resolve(directoryName + ".staging." + UUID.randomUUID());
        Files.createDirectories(staging);
        try {
            copyTree(staging);
            Files.writeString(staging.resolve(MARKER), digest + System.lineSeparator(), StandardCharsets.UTF_8);
            if (hasUsableExpectedIdentity(destination)) {
                return destination.toString();
            }
            try {
                move(staging, destination);
            } catch (IOException exception) {
                if (!hasUsableExpectedIdentity(destination)) {
                    throw new IOException("Managed JDK destination exists without the expected identity: "
                            + destination, exception);
                }
            }
            return destination.toString();
        } finally {
            deleteRecursively(staging);
        }
    }

    private boolean hasUsableExpectedIdentity(Path destination) throws IOException {
        final Path marker = destination.resolve(MARKER);
        return Files.isRegularFile(marker) && digest.equals(Files.readString(marker).trim())
                && Files.isExecutable(destination.resolve("bin/java"))
                && Files.isExecutable(destination.resolve("bin/javac"));
    }

    private static void retireInvalidIdentity(Path destination) throws IOException {
        if (!Files.exists(destination, LinkOption.NOFOLLOW_LINKS)) {
            return;
        }
        final Path retired = destination.resolveSibling(destination.getFileName() + ".invalid." + UUID.randomUUID());
        try {
            move(destination, retired);
            deleteRecursively(retired);
        } catch (NoSuchFileException exception) {
            if (Files.exists(destination, LinkOption.NOFOLLOW_LINKS)) {
                throw exception;
            }
        }
    }

    private void copyTree(Path staging) throws IOException {
        try (Stream<Path> entries = Files.walk(localHome)) {
            for (Path source : entries.toList()) {
                final Path relative = localHome.relativize(source);
                if (relative.getNameCount() == 0) {
                    continue;
                }
                final Path target = staging.resolve(relative.toString());
                if (Files.isSymbolicLink(source)) {
                    Files.createDirectories(Objects.requireNonNull(target.getParent(),
                            "JDK symbolic link has no parent"));
                    Files.createSymbolicLink(target,
                            remoteLinkTarget(target, Files.readSymbolicLink(source)));
                } else if (Files.isDirectory(source, LinkOption.NOFOLLOW_LINKS)) {
                    Files.createDirectories(target);
                    copyPermissions(source, target);
                } else if (Files.isRegularFile(source, LinkOption.NOFOLLOW_LINKS)) {
                    Files.createDirectories(Objects.requireNonNull(target.getParent(),
                            "JDK file has no parent"));
                    try (InputStream input = new BufferedInputStream(Files.newInputStream(source));
                         OutputStream output = new BufferedOutputStream(Files.newOutputStream(target))) {
                        input.transferTo(output);
                    }
                    copyPermissions(source, target);
                } else {
                    throw new IOException("Unsupported JDK filesystem entry: " + source);
                }
            }
        }
    }

    static Path remoteLinkTarget(Path target, Path localLinkTarget) {
        return target.getFileSystem().getPath(localLinkTarget.toString());
    }

    private static void copyPermissions(Path source, Path target) throws IOException {
        try {
            final Set<PosixFilePermission> permissions = Files.getPosixFilePermissions(source,
                    LinkOption.NOFOLLOW_LINKS);
            Files.setPosixFilePermissions(target, permissions);
        } catch (UnsupportedOperationException ignored) {
            if (Files.isExecutable(source)) {
                throw new IOException("Remote filesystem cannot preserve executable JDK files: " + target);
            }
        }
    }

    private static void move(Path source, Path destination) throws IOException {
        try {
            Files.move(source, destination, StandardCopyOption.ATOMIC_MOVE);
        } catch (AtomicMoveNotSupportedException exception) {
            Files.move(source, destination);
        }
    }

    private static void deleteRecursively(Path path) throws IOException {
        if (!Files.exists(path, LinkOption.NOFOLLOW_LINKS)) {
            return;
        }
        try (Stream<Path> entries = Files.walk(path)) {
            for (Path entry : entries.sorted(Comparator.reverseOrder()).toList()) {
                Files.deleteIfExists(entry);
            }
        }
    }

    private static MessageDigest newDigest() {
        try {
            return MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException(exception);
        }
    }

    private static void hashFile(MessageDigest digest, Path path) throws IOException {
        try (InputStream input = new BufferedInputStream(Files.newInputStream(path))) {
            final byte[] buffer = new byte[BUFFER_SIZE];
            int count;
            while ((count = input.read(buffer)) >= 0) {
                digest.update(buffer, 0, count);
            }
        }
    }

    private static void update(MessageDigest digest, String value) {
        digest.update(value.getBytes(StandardCharsets.UTF_8));
    }

    private static String permissionIdentity(Path path) throws IOException {
        try {
            return Files.getPosixFilePermissions(path, LinkOption.NOFOLLOW_LINKS).stream()
                    .map(Enum::name).sorted().reduce((left, right) -> left + "," + right).orElse("");
        } catch (UnsupportedOperationException exception) {
            return Files.isExecutable(path) ? "executable" : "not-executable";
        }
    }
}
