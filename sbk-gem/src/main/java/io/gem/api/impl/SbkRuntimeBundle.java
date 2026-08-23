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

import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveOutputStream;
import org.apache.commons.compress.archivers.tar.TarConstants;
import org.apache.commons.compress.compressors.gzip.GzipCompressorOutputStream;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HexFormat;
import java.util.List;
import java.util.Objects;
import java.util.jar.Attributes;
import java.util.jar.JarFile;
import java.util.stream.Stream;

/**
 * Immutable, content-addressed SBK runtime deployment archive.
 *
 * <p>The archive contains the complete installed SBK distribution, an
 * optional Java runtime, a deployment descriptor, and per-file SHA-256
 * checksums. Its content digest is derived from every source entry, so two
 * builds carrying the same SBK version but different bytes do not share a
 * remote deployment identity.
 */
final class SbkRuntimeBundle {
    static final String ARCHIVE_ROOT = "runtime";
    static final String SBK_DIRECTORY = "sbk";
    static final String JAVA_DIRECTORY = "java";
    static final String DESCRIPTOR_FILE = "deployment.properties";
    static final String CHECKSUM_FILE = "deployment-files.sha256";
    static final String REMOTE_DIGEST_FILE = ".sbk-runtime.sha256";
    private static final String SHA_256 = "SHA-256";
    private static final int DIGEST_NAME_CHARACTERS = 24;
    private static final int BUFFER_SIZE = 64 * 1024;
    private static final int REGULAR_FILE_MODE = 0644;
    private static final int EXECUTABLE_FILE_MODE = 0755;
    private static final int DIRECTORY_MODE = 0755;
    private static final int BUNDLE_FORMAT_VERSION = 2;

    private final Path archive;
    private final String archiveDigest;
    private final String contentDigest;
    private final String deploymentName;
    private final String sbkCommand;
    private final String javaHome;

    private SbkRuntimeBundle(Path archive, String archiveDigest, String contentDigest,
                             String deploymentName, String sbkCommand, String javaHome) {
        this.archive = archive;
        this.archiveDigest = archiveDigest;
        this.contentDigest = contentDigest;
        this.deploymentName = deploymentName;
        this.sbkCommand = sbkCommand;
        this.javaHome = javaHome;
    }

    /**
     * Create or reuse a cached runtime archive.
     *
     * @param sbkDirectory complete local {@code installDist} directory
     * @param relativeSbkCommand launcher path relative to {@code sbkDirectory}
     * @param javaDirectory local Java home to include, or {@code null}
     * @param sbkVersion discovered SBK version
     * @param javaVersion required Java major version
     * @param platform homogeneous deployment platform
     * @param cacheDirectory local runtime bundle cache
     * @return immutable runtime bundle
     * @throws IOException when validation, hashing, or archive creation fails
     */
    static SbkRuntimeBundle create(Path sbkDirectory, String relativeSbkCommand, Path javaDirectory,
                                   String sbkVersion, int javaVersion, DeploymentPlatform platform,
                                   Path cacheDirectory) throws IOException {
        final Path normalizedSbkDirectory = sbkDirectory.toAbsolutePath().normalize();
        final Path normalizedJavaDirectory = javaDirectory == null ? null
                : javaDirectory.toAbsolutePath().normalize();
        validateSbkDistribution(normalizedSbkDirectory, relativeSbkCommand);
        if (normalizedJavaDirectory != null) {
            validateJavaHome(normalizedJavaDirectory);
        }

        final List<BundleEntry> entries = new ArrayList<>();
        collectEntries(normalizedSbkDirectory, SBK_DIRECTORY, entries);
        if (normalizedJavaDirectory != null) {
            collectEntries(normalizedJavaDirectory, JAVA_DIRECTORY, entries);
        }
        entries.sort(Comparator.comparing(BundleEntry::relativePath));

        final String contentDigest = calculateContentDigest(sbkVersion, javaVersion, platform, entries);
        final String deploymentName = "sbk-runtime-" + sbkVersion + "-" + platform.id() + "-"
                + contentDigest.substring(0, DIGEST_NAME_CHARACTERS);
        Files.createDirectories(cacheDirectory);
        final Path archive = cacheDirectory.resolve(deploymentName + ".tar.gz");
        if (!Files.isRegularFile(archive)) {
            createArchive(archive, sbkVersion, javaVersion, platform, contentDigest, entries,
                    normalizedJavaDirectory != null);
        }
        return new SbkRuntimeBundle(archive, sha256(archive), contentDigest, deploymentName,
                SBK_DIRECTORY + "/" + normalizeRelativePath(relativeSbkCommand),
                normalizedJavaDirectory == null ? null : JAVA_DIRECTORY);
    }

    Path archive() {
        return archive;
    }

    String archiveDigest() {
        return archiveDigest;
    }

    String contentDigest() {
        return contentDigest;
    }

    String deploymentName() {
        return deploymentName;
    }

    String sbkCommand() {
        return sbkCommand;
    }

    String javaHome() {
        return javaHome;
    }

    private static void validateSbkDistribution(Path directory, String relativeCommand) throws IOException {
        if (!Files.isDirectory(directory)) {
            throw new IOException("SBK runtime bundle source is not a directory: " + directory);
        }
        final Path command = resolveContained(directory, relativeCommand);
        if (!Files.isExecutable(command)) {
            throw new IOException("SBK runtime bundle launcher is not executable: " + command);
        }
        final Path libraryDirectory = directory.resolve("lib");
        if (!Files.isDirectory(libraryDirectory)) {
            throw new IOException("SBK runtime bundle is missing its lib directory: " + libraryDirectory);
        }
        final List<Path> pathingJars;
        try (Stream<Path> paths = Files.list(libraryDirectory)) {
            pathingJars = paths.filter(path -> fileName(path).startsWith("sbk-pathing-"))
                    .filter(path -> fileName(path).endsWith(".jar"))
                    .toList();
        }
        if (pathingJars.size() != 1) {
            throw new IOException("Expected exactly one SBK pathing JAR under " + libraryDirectory
                    + ", found " + pathingJars.size());
        }
        try (JarFile pathingJar = new JarFile(pathingJars.getFirst().toFile())) {
            final Attributes attributes = pathingJar.getManifest().getMainAttributes();
            final String classPath = attributes.getValue(Attributes.Name.CLASS_PATH);
            if (classPath == null || classPath.isBlank()) {
                throw new IOException("SBK pathing JAR has no Class-Path manifest: " + pathingJars.getFirst());
            }
            for (String dependency : classPath.trim().split("\\s+")) {
                final Path dependencyPath = resolveContained(libraryDirectory, dependency);
                if (!Files.isRegularFile(dependencyPath)) {
                    throw new IOException("SBK pathing dependency is missing: " + dependencyPath);
                }
            }
        }
    }

    private static void validateJavaHome(Path directory) throws IOException {
        for (String executable : List.of("java", "javac")) {
            final Path path = directory.resolve("bin").resolve(executable);
            if (!Files.isExecutable(path)) {
                throw new IOException("SBK runtime bundle Java home is missing executable " + path);
            }
        }
    }

    private static Path resolveContained(Path parent, String relativePath) throws IOException {
        if (relativePath == null || relativePath.isBlank() || Path.of(relativePath).isAbsolute()) {
            throw new IOException("Runtime bundle path must be relative: " + relativePath);
        }
        final Path resolved = parent.resolve(relativePath).normalize();
        if (!resolved.startsWith(parent)) {
            throw new IOException("Runtime bundle path escapes its directory: " + relativePath);
        }
        return resolved;
    }

    private static void collectEntries(Path sourceRoot, String archiveRoot, List<BundleEntry> entries)
            throws IOException {
        try (Stream<Path> paths = Files.walk(sourceRoot)) {
            for (Path path : paths.toList()) {
                final Path relative = sourceRoot.relativize(path);
                final String archivePath = relative.getNameCount() == 0 ? archiveRoot
                        : archiveRoot + "/" + normalizeRelativePath(relative.toString());
                if (Files.isSymbolicLink(path)) {
                    final String linkTarget = Files.readSymbolicLink(path).toString();
                    entries.add(new BundleEntry(path, archivePath, EntryType.SYMBOLIC_LINK, 0,
                            sha256(linkTarget.getBytes(StandardCharsets.UTF_8)), linkTarget, false));
                } else if (Files.isDirectory(path, LinkOption.NOFOLLOW_LINKS)) {
                    entries.add(new BundleEntry(path, archivePath, EntryType.DIRECTORY, 0, "", "",
                            Files.isExecutable(path)));
                } else if (Files.isRegularFile(path, LinkOption.NOFOLLOW_LINKS)) {
                    entries.add(new BundleEntry(path, archivePath, EntryType.REGULAR_FILE, Files.size(path),
                            sha256(path), "", Files.isExecutable(path)));
                } else {
                    throw new IOException("Unsupported runtime bundle filesystem entry: " + path);
                }
            }
        }
    }

    private static String calculateContentDigest(String sbkVersion, int javaVersion, DeploymentPlatform platform,
                                                 List<BundleEntry> entries) {
        final MessageDigest digest = newDigest();
        update(digest, "format=" + BUNDLE_FORMAT_VERSION + "\n");
        update(digest, "sbk.version=" + sbkVersion + "\n");
        update(digest, "java.version=" + javaVersion + "\n");
        update(digest, "platform=" + platform.id() + "\n");
        for (BundleEntry entry : entries) {
            update(digest, entry.type() + "\t" + entry.relativePath() + "\t" + entry.size() + "\t"
                    + entry.digest() + "\t" + entry.linkTarget() + "\t" + entry.executable() + "\n");
        }
        return HexFormat.of().formatHex(digest.digest());
    }

    private static void createArchive(Path archive, String sbkVersion, int javaVersion,
                                      DeploymentPlatform platform, String contentDigest,
                                      List<BundleEntry> entries, boolean includesJava) throws IOException {
        final Path archiveParent = Objects.requireNonNull(archive.toAbsolutePath().getParent(),
                "Runtime archive must have a parent directory");
        final Path temporaryArchive = Files.createTempFile(archiveParent, fileName(archive), ".partial");
        try {
            try (OutputStream fileOutput = new BufferedOutputStream(Files.newOutputStream(temporaryArchive));
                 GzipCompressorOutputStream gzipOutput = new GzipCompressorOutputStream(fileOutput);
                 TarArchiveOutputStream tarOutput = new TarArchiveOutputStream(gzipOutput)) {
                tarOutput.setLongFileMode(TarArchiveOutputStream.LONGFILE_POSIX);
                tarOutput.setBigNumberMode(TarArchiveOutputStream.BIGNUMBER_POSIX);
                addDirectoryEntry(tarOutput, ARCHIVE_ROOT + "/");
                for (BundleEntry entry : entries) {
                    addFilesystemEntry(tarOutput, entry);
                }
                addByteEntry(tarOutput, ARCHIVE_ROOT + "/" + DESCRIPTOR_FILE,
                        descriptor(sbkVersion, javaVersion, platform, contentDigest, includesJava));
                addByteEntry(tarOutput, ARCHIVE_ROOT + "/" + CHECKSUM_FILE, checksums(entries));
            }
            moveAtomically(temporaryArchive, archive);
        } finally {
            Files.deleteIfExists(temporaryArchive);
        }
    }

    private static void addFilesystemEntry(TarArchiveOutputStream output, BundleEntry bundleEntry)
            throws IOException {
        String archiveName = ARCHIVE_ROOT + "/" + bundleEntry.relativePath();
        if (bundleEntry.type() == EntryType.DIRECTORY) {
            archiveName += "/";
        }
        final TarArchiveEntry archiveEntry;
        if (bundleEntry.type() == EntryType.SYMBOLIC_LINK) {
            archiveEntry = new TarArchiveEntry(archiveName, TarConstants.LF_SYMLINK);
            archiveEntry.setLinkName(bundleEntry.linkTarget());
            archiveEntry.setMode(0777);
            archiveEntry.setSize(0);
        } else {
            archiveEntry = new TarArchiveEntry(archiveName);
            archiveEntry.setMode(bundleEntry.type() == EntryType.DIRECTORY ? DIRECTORY_MODE
                    : bundleEntry.executable() ? EXECUTABLE_FILE_MODE : REGULAR_FILE_MODE);
            archiveEntry.setSize(bundleEntry.size());
        }
        output.putArchiveEntry(archiveEntry);
        if (bundleEntry.type() == EntryType.REGULAR_FILE) {
            try (BufferedInputStream input = new BufferedInputStream(Files.newInputStream(bundleEntry.source()),
                    BUFFER_SIZE)) {
                input.transferTo(output);
            }
        }
        output.closeArchiveEntry();
    }

    private static void addDirectoryEntry(TarArchiveOutputStream output, String name) throws IOException {
        final TarArchiveEntry entry = new TarArchiveEntry(name);
        entry.setMode(DIRECTORY_MODE);
        output.putArchiveEntry(entry);
        output.closeArchiveEntry();
    }

    private static void addByteEntry(TarArchiveOutputStream output, String name, String value) throws IOException {
        final byte[] bytes = value.getBytes(StandardCharsets.UTF_8);
        final TarArchiveEntry entry = new TarArchiveEntry(name);
        entry.setMode(REGULAR_FILE_MODE);
        entry.setSize(bytes.length);
        output.putArchiveEntry(entry);
        output.write(bytes);
        output.closeArchiveEntry();
    }

    private static String descriptor(String sbkVersion, int javaVersion, DeploymentPlatform platform,
                                     String contentDigest, boolean includesJava) {
        return "format.version=" + BUNDLE_FORMAT_VERSION + "\n"
                + "sbk.version=" + sbkVersion + "\n"
                + "java.version=" + javaVersion + "\n"
                + "platform.os=" + platform.operatingSystem() + "\n"
                + "platform.arch=" + platform.architecture() + "\n"
                + "content.sha256=" + contentDigest + "\n"
                + "includes.java=" + includesJava + "\n";
    }

    private static String checksums(List<BundleEntry> entries) {
        final StringBuilder checksums = new StringBuilder();
        for (BundleEntry entry : entries) {
            if (entry.type() == EntryType.REGULAR_FILE) {
                checksums.append(entry.digest()).append("  ").append(entry.relativePath()).append('\n');
            }
        }
        return checksums.toString();
    }

    private static void moveAtomically(Path source, Path target) throws IOException {
        try {
            Files.move(source, target, StandardCopyOption.ATOMIC_MOVE);
        } catch (AtomicMoveNotSupportedException ignored) {
            Files.move(source, target, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    private static String normalizeRelativePath(String value) {
        return value.replace('\\', '/');
    }

    private static String fileName(Path path) {
        return Objects.requireNonNull(path.getFileName(), "Filesystem entry must have a file name").toString();
    }

    private static String sha256(Path path) throws IOException {
        final MessageDigest digest = newDigest();
        try (BufferedInputStream input = new BufferedInputStream(Files.newInputStream(path), BUFFER_SIZE)) {
            final byte[] buffer = new byte[BUFFER_SIZE];
            int count;
            while ((count = input.read(buffer)) >= 0) {
                digest.update(buffer, 0, count);
            }
        }
        return HexFormat.of().formatHex(digest.digest());
    }

    private static String sha256(byte[] bytes) {
        return HexFormat.of().formatHex(newDigest().digest(bytes));
    }

    private static MessageDigest newDigest() {
        try {
            return MessageDigest.getInstance(SHA_256);
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException(SHA_256 + " is unavailable", exception);
        }
    }

    private static void update(MessageDigest digest, String value) {
        digest.update(value.getBytes(StandardCharsets.UTF_8));
    }

    private enum EntryType {
        DIRECTORY,
        REGULAR_FILE,
        SYMBOLIC_LINK
    }

    private record BundleEntry(Path source, String relativePath, EntryType type, long size,
                               String digest, String linkTarget, boolean executable) {
    }
}
