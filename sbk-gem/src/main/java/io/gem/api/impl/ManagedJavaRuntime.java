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
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Comparator;
import java.util.HexFormat;
import java.util.Objects;
import java.util.Properties;
import java.util.Set;
import java.util.UUID;
import java.util.function.LongConsumer;
import java.util.stream.Stream;

/** Content-addressed controller JDK copied independently from the SBK runtime. */
final class ManagedJavaRuntime {
    private static final String MARKER = ".sbk-java.sha256";
    private static final String IDENTITY_CACHE_FORMAT = "1";
    private static final int HASH_BUFFER_SIZE = 64 * 1024;
    private static final int COPY_BUFFER_SIZE = 256 * 1024;
    private static final int IDENTITY_CHARACTERS = 24;
    private static final LongConsumer NO_COPY_PROGRESS = ignored -> { };
    private final Path localHome;
    private final String digest;
    private final String directoryName;
    private final long contentBytes;

    private ManagedJavaRuntime(Path localHome, String digest, String directoryName, long contentBytes) {
        this.localHome = localHome;
        this.digest = digest;
        this.directoryName = directoryName;
        this.contentBytes = contentBytes;
    }

    static ManagedJavaRuntime create(Path javaHome, int major) throws IOException {
        final Path home = javaHome.toAbsolutePath().normalize();
        if (!Files.isExecutable(home.resolve("bin/java")) || !Files.isExecutable(home.resolve("bin/javac"))) {
            throw new IOException("Controller JDK is incomplete: " + home);
        }
        final MessageDigest digest = newDigest();
        long contentBytes = 0;
        try (Stream<Path> entries = Files.walk(home)) {
            for (Path path : entries.sorted(Comparator.comparing(Path::toString)).toList()) {
                final String relative = home.relativize(path).toString().replace('\\', '/');
                update(digest, relative + "\0");
                if (Files.isSymbolicLink(path)) {
                    update(digest, "L\0" + Files.readSymbolicLink(path) + "\0");
                } else if (Files.isDirectory(path, LinkOption.NOFOLLOW_LINKS)) {
                    update(digest, "D\0");
                } else if (Files.isRegularFile(path, LinkOption.NOFOLLOW_LINKS)) {
                    final long fileBytes = Files.size(path);
                    contentBytes += fileBytes;
                    update(digest, "F\0" + fileBytes + "\0");
                    hashFile(digest, path);
                }
                if (!Files.isSymbolicLink(path)) {
                    update(digest, "P\0" + permissionIdentity(path) + "\0");
                }
            }
        }
        final String identity = HexFormat.of().formatHex(digest.digest());
        return runtime(home, major, identity, contentBytes);
    }

    /**
     * Create a managed JDK identity, reusing a persisted full-content digest while filesystem metadata is unchanged.
     *
     * @param javaHome controller JDK home
     * @param major controller Java major version
     * @param cacheDirectory local SBK-GEM cache directory
     * @return content-addressed managed Java runtime
     * @throws IOException when the JDK or identity cache cannot be read
     */
    static ManagedJavaRuntime create(Path javaHome, int major, Path cacheDirectory) throws IOException {
        final Path home = javaHome.toAbsolutePath().normalize();
        if (!Files.isExecutable(home.resolve("bin/java")) || !Files.isExecutable(home.resolve("bin/javac"))) {
            throw new IOException("Controller JDK is incomplete: " + home);
        }
        Files.createDirectories(cacheDirectory);
        final String cacheKey = sha256((home + "\0" + major).getBytes(StandardCharsets.UTF_8));
        final Path identityFile = cacheDirectory.resolve("sbk-java-identity-" + cacheKey + ".properties");
        final Path lockFile = identityFile.resolveSibling(identityFile.getFileName() + ".lock");
        synchronized (ManagedJavaRuntime.class) {
            try (FileChannel channel = FileChannel.open(lockFile, java.nio.file.StandardOpenOption.CREATE,
                    java.nio.file.StandardOpenOption.WRITE);
                 FileLock ignored = channel.lock()) {
                final MetadataIdentity metadata = metadataIdentity(home);
                final Properties cached = loadIdentity(identityFile);
                final String cachedDigest = cached.getProperty("content.sha256", "");
                if (IDENTITY_CACHE_FORMAT.equals(cached.getProperty("format.version"))
                        && home.toString().equals(cached.getProperty("java.home"))
                        && Integer.toString(major).equals(cached.getProperty("java.major"))
                        && metadata.digest().equals(cached.getProperty("metadata.sha256"))
                        && isSha256(cachedDigest)) {
                    return runtime(home, major, cachedDigest, metadata.contentBytes());
                }
                final ManagedJavaRuntime runtime = create(home, major);
                final Properties identity = new Properties();
                identity.setProperty("format.version", IDENTITY_CACHE_FORMAT);
                identity.setProperty("java.home", home.toString());
                identity.setProperty("java.major", Integer.toString(major));
                identity.setProperty("metadata.sha256", metadata.digest());
                identity.setProperty("content.sha256", runtime.digest);
                writeIdentity(identityFile, identity);
                return runtime;
            }
        }
    }

    private static ManagedJavaRuntime runtime(Path home, int major, String identity, long contentBytes) {
        return new ManagedJavaRuntime(home, identity,
                "sbk-java-" + major + "-" + identity.substring(0, IDENTITY_CHARACTERS), contentBytes);
    }

    String directoryName() {
        return directoryName;
    }

    long contentBytes() {
        return contentBytes;
    }

    String install(java.nio.file.FileSystem fileSystem, String parentDirectory) throws IOException {
        return install(fileSystem, parentDirectory, NO_COPY_PROGRESS);
    }

    String install(java.nio.file.FileSystem fileSystem, String parentDirectory, LongConsumer copyProgress)
            throws IOException {
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
            copyTree(staging, copyProgress);
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

    private void copyTree(Path staging, LongConsumer copyProgress) throws IOException {
        final byte[] copyBuffer = new byte[COPY_BUFFER_SIZE];
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
                        int copied;
                        while ((copied = input.read(copyBuffer)) >= 0) {
                            output.write(copyBuffer, 0, copied);
                            copyProgress.accept(copied);
                        }
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

    private static MetadataIdentity metadataIdentity(Path home) throws IOException {
        final MessageDigest digest = newDigest();
        long contentBytes = 0;
        try (Stream<Path> entries = Files.walk(home)) {
            for (Path path : entries.sorted(Comparator.comparing(Path::toString)).toList()) {
                update(digest, home.relativize(path).toString().replace('\\', '/') + "\0");
                if (Files.isSymbolicLink(path)) {
                    update(digest, "L\0" + Files.readSymbolicLink(path) + "\0");
                } else if (Files.isDirectory(path, LinkOption.NOFOLLOW_LINKS)) {
                    update(digest, "D\0");
                } else if (Files.isRegularFile(path, LinkOption.NOFOLLOW_LINKS)) {
                    final long fileBytes = Files.size(path);
                    contentBytes += fileBytes;
                    update(digest, "F\0" + fileBytes + "\0"
                            + Files.getLastModifiedTime(path, LinkOption.NOFOLLOW_LINKS).toMillis() + "\0");
                }
                if (!Files.isSymbolicLink(path)) {
                    update(digest, "P\0" + permissionIdentity(path) + "\0");
                }
            }
        }
        return new MetadataIdentity(HexFormat.of().formatHex(digest.digest()), contentBytes);
    }

    private static Properties loadIdentity(Path identityFile) throws IOException {
        final Properties identity = new Properties();
        if (Files.isRegularFile(identityFile)) {
            try (InputStream input = Files.newInputStream(identityFile)) {
                identity.load(input);
            }
        }
        return identity;
    }

    private static void writeIdentity(Path identityFile, Properties identity) throws IOException {
        final Path temporary = Files.createTempFile(Objects.requireNonNull(identityFile.getParent()),
                Objects.requireNonNull(identityFile.getFileName()).toString(), ".tmp");
        try {
            try (OutputStream output = Files.newOutputStream(temporary)) {
                identity.store(output, "SBK-GEM managed JDK identity cache");
            }
            try {
                Files.move(temporary, identityFile, StandardCopyOption.ATOMIC_MOVE,
                        StandardCopyOption.REPLACE_EXISTING);
            } catch (AtomicMoveNotSupportedException exception) {
                Files.move(temporary, identityFile, StandardCopyOption.REPLACE_EXISTING);
            }
        } finally {
            Files.deleteIfExists(temporary);
        }
    }

    private static String sha256(byte[] value) {
        final MessageDigest digest = newDigest();
        digest.update(value);
        return HexFormat.of().formatHex(digest.digest());
    }

    private static boolean isSha256(String value) {
        if (value.length() != 64) {
            return false;
        }
        try {
            HexFormat.of().parseHex(value);
            return true;
        } catch (IllegalArgumentException exception) {
            return false;
        }
    }

    private static void hashFile(MessageDigest digest, Path path) throws IOException {
        try (InputStream input = new BufferedInputStream(Files.newInputStream(path))) {
            final byte[] buffer = new byte[HASH_BUFFER_SIZE];
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

    private record MetadataIdentity(String digest, long contentBytes) {
    }
}
