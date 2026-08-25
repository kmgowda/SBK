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

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveOutputStream;
import org.apache.commons.compress.archivers.tar.TarConstants;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.channels.OverlappingFileLockException;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.DigestOutputStream;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HexFormat;
import java.util.List;
import java.util.Objects;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.locks.ReentrantLock;
import java.util.jar.Attributes;
import java.util.jar.JarFile;
import java.util.stream.Stream;

/**
 * Immutable, content-addressed SBK runtime deployment archive.
 *
 * <p>The archive contains the installed SBK runtime files, a deployment
 * descriptor and per-file SHA-256 checksums. Gradle derives the build identity
 * from the runtime inputs, allowing an unchanged cached archive to be selected
 * without rehashing the complete installation on every execution.
 */
final class SbkRuntimeBundle {
    static final String ARCHIVE_ROOT = "runtime";
    static final String SBK_DIRECTORY = "sbk";
    static final String DESCRIPTOR_FILE = "deployment.properties";
    static final String CHECKSUM_FILE = "deployment-files.sha256";
    static final String REMOTE_DIGEST_FILE = ".sbk-runtime.sha256";
    static final String RUNTIME_IDENTITY_FILE = "sbk-runtime-identity.properties";
    private static final String SHA_256 = "SHA-256";
    private static final int SHA_256_HEX_LENGTH = 64;
    private static final int DIGEST_NAME_CHARACTERS = 24;
    private static final int BUFFER_SIZE = 64 * 1024;
    private static final int REGULAR_FILE_MODE = 0644;
    private static final int EXECUTABLE_FILE_MODE = 0755;
    private static final int DIRECTORY_MODE = 0755;
    private static final int SYMBOLIC_LINK_MODE = 0777;
    private static final int BUNDLE_FORMAT_VERSION = 5;
    private static final int RUNTIME_IDENTITY_FORMAT_VERSION = 1;
    private static final String ARCHIVE_EXTENSION = ".tar";
    private static final String LEGACY_ARCHIVE_EXTENSION = ".tar.gz";
    private static final String ARCHIVE_DIGEST_SUFFIX = ".sha256";
    private static final String ARCHIVE_SIZE_SUFFIX = ".size";
    private static final String CACHE_LOCK_SUFFIX = ".lock";
    private static final String CACHE_MANAGEMENT_LOCK = ".sbk-runtime-cache-management.lock";
    private static final ConcurrentMap<Path, ReentrantLock> CACHE_LOCKS = new ConcurrentHashMap<>();

    private final Path archive;
    private volatile String archiveDigest;
    private final String contentDigest;
    private final String deploymentName;
    private final String relativeSbkCommand;
    private final Path cacheLockFile;
    private final Path archiveDigestFile;
    private final Path archiveSizeFile;
    private final Path sbkDirectory;
    private final String sbkVersion;
    private final int javaVersion;
    private final DeploymentPlatform platform;

    private SbkRuntimeBundle(Path archive, String archiveDigest, String contentDigest,
                             String deploymentName, String relativeSbkCommand, Path cacheLockFile,
                             Path archiveDigestFile, Path archiveSizeFile, Path sbkDirectory,
                             String sbkVersion, int javaVersion, DeploymentPlatform platform) {
        this.archive = archive;
        this.archiveDigest = archiveDigest;
        this.contentDigest = contentDigest;
        this.deploymentName = deploymentName;
        this.relativeSbkCommand = relativeSbkCommand;
        this.cacheLockFile = cacheLockFile;
        this.archiveDigestFile = archiveDigestFile;
        this.archiveSizeFile = archiveSizeFile;
        this.sbkDirectory = sbkDirectory;
        this.sbkVersion = sbkVersion;
        this.javaVersion = javaVersion;
        this.platform = platform;
    }

    /**
     * Create or reuse a cached runtime archive.
     *
     * @param sbkDirectory complete local {@code installDist} directory
     * @param relativeSbkCommand launcher path relative to {@code sbkDirectory}
     * @param sbkVersion discovered SBK version
     * @param javaVersion required Java major version
     * @param platform homogeneous deployment platform
     * @param cacheDirectory local runtime bundle cache
     * @return immutable runtime bundle
     * @throws IOException when validation, hashing, or archive creation fails
     */
    static SbkRuntimeBundle create(Path sbkDirectory, String relativeSbkCommand,
                                   String sbkVersion, int javaVersion, DeploymentPlatform platform,
                                   Path cacheDirectory) throws IOException {
        final Path normalizedSbkDirectory = sbkDirectory.toAbsolutePath().normalize();
        validateSbkDistribution(normalizedSbkDirectory, relativeSbkCommand);
        final String buildIdentity = loadBuildIdentity(normalizedSbkDirectory, sbkVersion);
        final String contentDigest = calculateContentDigest(sbkVersion, javaVersion, platform, buildIdentity);
        final String deploymentName = "sbk-runtime-" + sbkVersion + "-" + platform.id() + "-"
                + contentDigest.substring(0, DIGEST_NAME_CHARACTERS);
        Files.createDirectories(cacheDirectory);
        final Path archive = cacheDirectory.resolve(deploymentName + ARCHIVE_EXTENSION);
        final Path archiveDigestFile = cacheDirectory.resolve(deploymentName + ARCHIVE_EXTENSION
                + ARCHIVE_DIGEST_SUFFIX);
        final Path archiveSizeFile = cacheDirectory.resolve(deploymentName + ARCHIVE_EXTENSION
                + ARCHIVE_SIZE_SUFFIX);
        final Path cacheLockFile = cacheDirectory.resolve(deploymentName + CACHE_LOCK_SUFFIX);
        final Path managementLockFile = cacheDirectory.resolve(CACHE_MANAGEMENT_LOCK);
        final ReentrantLock managementLock = cacheLock(managementLockFile);
        managementLock.lock();
        try (FileChannel managementChannel = openLockChannel(managementLockFile);
             FileLock ignoredManagement = managementChannel.lock()) {
            final ReentrantLock processLock = cacheLock(cacheLockFile);
            processLock.lock();
            try (FileChannel lockChannel = openLockChannel(cacheLockFile);
                 FileLock ignored = lockChannel.lock()) {
                String archiveDigest = cachedArchiveDigest(archive, archiveDigestFile, archiveSizeFile);
                if (archiveDigest == null) {
                    final List<BundleEntry> entries = collectRuntimeEntries(normalizedSbkDirectory);
                    archiveDigest = createArchive(archive, sbkVersion, javaVersion, platform, contentDigest, entries);
                    writeAtomically(archiveDigestFile, archiveDigest + "\n");
                    writeAtomically(archiveSizeFile, Files.size(archive) + "\n");
                }
                return new SbkRuntimeBundle(archive, archiveDigest, contentDigest, deploymentName,
                        relativeSbkCommand, cacheLockFile,
                        archiveDigestFile, archiveSizeFile, normalizedSbkDirectory, sbkVersion, javaVersion,
                        platform);
            } finally {
                processLock.unlock();
            }
        } finally {
            managementLock.unlock();
        }
    }

    /**
     * Prevent cleanup from deleting this archive while an asynchronous transfer uses it.
     *
     * @return archive-use lease
     * @throws IOException when the cache lock cannot be acquired
     */
    @SuppressFBWarnings(value = "UL_UNRELEASED_LOCK",
            justification = "Lock ownership is transferred to the returned AutoCloseable ArchiveUse")
    ArchiveUse acquireArchiveUse() throws IOException {
        final ReentrantLock processLock = cacheLock(cacheLockFile);
        processLock.lock();
        try {
            final FileChannel channel = openLockChannel(cacheLockFile);
            try {
                return new ArchiveUse(processLock, channel, channel.lock());
            } catch (IOException | RuntimeException exception) {
                channel.close();
                throw exception;
            }
        } catch (IOException | RuntimeException exception) {
            processLock.unlock();
            throw exception;
        }
    }

    /**
     * Rebuild a locally corrupted archive from the immutable installed distribution.
     *
     * @throws IOException when the installed build identity changed or archive recreation fails
     */
    void rebuildArchive() throws IOException {
        validateSbkDistribution(sbkDirectory, relativeSbkCommand);
        final String currentIdentity = loadBuildIdentity(sbkDirectory, sbkVersion);
        final String currentContentDigest = calculateContentDigest(sbkVersion, javaVersion, platform,
                currentIdentity);
        if (!contentDigest.equals(currentContentDigest)) {
            throw new IOException("SBK installed distribution changed while rebuilding runtime archive");
        }
        final ReentrantLock processLock = cacheLock(cacheLockFile);
        processLock.lock();
        try (FileChannel lockChannel = openLockChannel(cacheLockFile);
             FileLock ignored = lockChannel.lock()) {
            final List<BundleEntry> entries = collectRuntimeEntries(sbkDirectory);
            archiveDigest = createArchive(archive, sbkVersion, javaVersion, platform, contentDigest, entries);
            writeAtomically(archiveDigestFile, archiveDigest + "\n");
            writeAtomically(archiveSizeFile, Files.size(archive) + "\n");
        } finally {
            processLock.unlock();
        }
    }

    /**
     * Retain only the selected inactive local bundle cache identity.
     *
     * <p>An archive currently locked by another GEM deployment is retained and
     * becomes eligible for removal after that deployment releases its lock.
     * Lock metadata is intentionally retained so concurrent processes always
     * coordinate through the same filesystem inode.</p>
     *
     * @param cacheDirectory local runtime bundle cache
     * @param deploymentName selected deployment identity
     * @return number of inactive cached bundle identities removed
     * @throws IOException when the cache cannot be inspected
     */
    static int cleanupOtherCachedBundles(Path cacheDirectory, String deploymentName) throws IOException {
        validateIdentifier(deploymentName);
        Files.createDirectories(cacheDirectory);
        final Path managementLockFile = cacheDirectory.resolve(CACHE_MANAGEMENT_LOCK);
        final ReentrantLock managementLock = cacheLock(managementLockFile);
        managementLock.lock();
        try (FileChannel managementChannel = openLockChannel(managementLockFile);
             FileLock ignoredManagement = managementChannel.lock();
             Stream<Path> paths = Files.list(cacheDirectory)) {
            int removed = 0;
            for (Path archive : paths.filter(SbkRuntimeBundle::isBundleArchive).toList()) {
                final String archiveName = fileName(archive);
                final String candidateName = archiveName.substring(0,
                        archiveName.length() - archiveExtension(archiveName).length());
                if (candidateName.equals(deploymentName)) {
                    continue;
                }
                final Path candidateLockFile = cacheDirectory.resolve(candidateName + CACHE_LOCK_SUFFIX);
                final ReentrantLock candidateLock = cacheLock(candidateLockFile);
                if (!candidateLock.tryLock()) {
                    continue;
                }
                try (FileChannel candidateChannel = openLockChannel(candidateLockFile)) {
                    final FileLock fileLock = tryFileLock(candidateChannel);
                    if (fileLock == null) {
                        continue;
                    }
                    try (fileLock) {
                        Files.deleteIfExists(archive);
                        Files.deleteIfExists(cacheDirectory.resolve(archiveName + ARCHIVE_DIGEST_SUFFIX));
                        Files.deleteIfExists(cacheDirectory.resolve(archiveName + ARCHIVE_SIZE_SUFFIX));
                        removed++;
                    }
                } finally {
                    candidateLock.unlock();
                }
            }
            return removed;
        } finally {
            managementLock.unlock();
        }
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


    private static ReentrantLock cacheLock(Path path) {
        return CACHE_LOCKS.computeIfAbsent(path.toAbsolutePath().normalize(), ignored -> new ReentrantLock());
    }

    private static FileChannel openLockChannel(Path path) throws IOException {
        return FileChannel.open(path, StandardOpenOption.CREATE, StandardOpenOption.WRITE);
    }

    private static FileLock tryFileLock(FileChannel channel) throws IOException {
        try {
            return channel.tryLock();
        } catch (OverlappingFileLockException ignored) {
            return null;
        }
    }

    private static boolean isBundleArchive(Path path) {
        final String name = fileName(path);
        return Files.isRegularFile(path) && name.startsWith("sbk-runtime-")
                && (name.endsWith(ARCHIVE_EXTENSION) || name.endsWith(LEGACY_ARCHIVE_EXTENSION));
    }

    private static String archiveExtension(String archiveName) {
        return archiveName.endsWith(LEGACY_ARCHIVE_EXTENSION) ? LEGACY_ARCHIVE_EXTENSION : ARCHIVE_EXTENSION;
    }

    private static void validateIdentifier(String value) {
        if (value == null || !value.startsWith("sbk-runtime-") || value.indexOf('/') >= 0
                || value.indexOf('\\') >= 0) {
            throw new IllegalArgumentException("Invalid SBK runtime deployment name: " + value);
        }
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

    private static String loadBuildIdentity(Path directory, String sbkVersion) throws IOException {
        final Path identityFile = directory.resolve(RUNTIME_IDENTITY_FILE);
        if (!Files.isRegularFile(identityFile)) {
            throw new IOException("SBK runtime identity is missing: " + identityFile
                    + ". Rebuild the installed distribution with './gradlew installDist'.");
        }
        final Properties identity = new Properties();
        try (var input = Files.newInputStream(identityFile)) {
            identity.load(input);
        }
        if (!Integer.toString(RUNTIME_IDENTITY_FORMAT_VERSION).equals(identity.getProperty("format.version"))) {
            throw new IOException("Unsupported SBK runtime identity format in " + identityFile);
        }
        if (!sbkVersion.equals(identity.getProperty("sbk.version"))) {
            throw new IOException("SBK runtime identity version does not match " + sbkVersion + " in "
                    + identityFile);
        }
        final String buildIdentity = identity.getProperty("build.sha256", "").trim();
        if (buildIdentity.length() != SHA_256_HEX_LENGTH) {
            throw new IOException("Invalid SBK runtime build identity in " + identityFile);
        }
        return buildIdentity;
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
        final Path realSourceRoot = sourceRoot.toRealPath();
        addEntry(sourceRoot, sourceRoot, realSourceRoot, archiveRoot, entries);
        try (Stream<Path> children = Files.list(sourceRoot)) {
            for (Path child : children.toList()) {
                try (Stream<Path> paths = Files.walk(child)) {
                    for (Path path : paths.toList()) {
                        addEntry(sourceRoot, path, realSourceRoot, archiveRoot, entries);
                    }
                }
            }
        }
    }

    private static List<BundleEntry> collectRuntimeEntries(Path sbkDirectory) throws IOException {
        final List<BundleEntry> entries = new ArrayList<>();
        entries.add(new BundleEntry(sbkDirectory, SBK_DIRECTORY, EntryType.DIRECTORY, 0, "", "",
                DIRECTORY_MODE));
        collectEntries(sbkDirectory.resolve("bin"), SBK_DIRECTORY + "/bin", entries);
        collectEntries(sbkDirectory.resolve("lib"), SBK_DIRECTORY + "/lib", entries);
        addEntry(sbkDirectory, sbkDirectory.resolve(RUNTIME_IDENTITY_FILE), sbkDirectory.toRealPath(),
                SBK_DIRECTORY, entries);
        entries.sort(Comparator.comparing(BundleEntry::relativePath));
        return entries;
    }

    private static void addEntry(Path sourceRoot, Path path, Path realSourceRoot, String archiveRoot,
                                 List<BundleEntry> entries) throws IOException {
        final Path relative = sourceRoot.relativize(path);
        final String archivePath = relative.toString().isEmpty() ? archiveRoot
                : archiveRoot + "/" + normalizeRelativePath(relative.toString());
        if (Files.isSymbolicLink(path)) {
            final Path symbolicTarget = Files.readSymbolicLink(path);
            validateContainedSymbolicLink(sourceRoot, realSourceRoot, path, symbolicTarget);
            final String linkTarget = symbolicTarget.toString();
            entries.add(new BundleEntry(path, archivePath, EntryType.SYMBOLIC_LINK, 0,
                    sha256(linkTarget.getBytes(StandardCharsets.UTF_8)), linkTarget, SYMBOLIC_LINK_MODE));
        } else if (Files.isDirectory(path, LinkOption.NOFOLLOW_LINKS)) {
            entries.add(new BundleEntry(path, archivePath, EntryType.DIRECTORY, 0, "", "", DIRECTORY_MODE));
        } else if (Files.isRegularFile(path, LinkOption.NOFOLLOW_LINKS)) {
            entries.add(new BundleEntry(path, archivePath, EntryType.REGULAR_FILE, Files.size(path),
                    sha256(path), "", Files.isExecutable(path) ? EXECUTABLE_FILE_MODE : REGULAR_FILE_MODE));
        } else {
            throw new IOException("Unsupported runtime bundle filesystem entry: " + path);
        }
    }

    private static void validateContainedSymbolicLink(Path sourceRoot, Path realSourceRoot, Path link,
                                                      Path linkTarget) throws IOException {
        if (linkTarget.isAbsolute()) {
            throw new IOException("Runtime bundle symbolic link must be relative: " + link + " -> " + linkTarget);
        }
        final Path linkParent = Objects.requireNonNull(link.getParent(),
                "Runtime bundle symbolic link must have a parent directory");
        final Path resolvedTarget = linkParent.resolve(linkTarget).normalize();
        if (!resolvedTarget.startsWith(sourceRoot) || !resolvedTarget.toRealPath().startsWith(realSourceRoot)) {
            throw new IOException("Runtime bundle symbolic link escapes its source tree: " + link + " -> "
                    + linkTarget);
        }
    }

    private static String calculateContentDigest(String sbkVersion, int javaVersion, DeploymentPlatform platform,
                                                 String buildIdentity) {
        final MessageDigest digest = newDigest();
        update(digest, "format=" + BUNDLE_FORMAT_VERSION + "\n");
        update(digest, "sbk.version=" + sbkVersion + "\n");
        update(digest, "java.version=" + javaVersion + "\n");
        update(digest, "platform.os=" + platform.id() + "\n");
        update(digest, "build.sha256=" + buildIdentity + "\n");
        return HexFormat.of().formatHex(digest.digest());
    }

    private static String createArchive(Path archive, String sbkVersion, int javaVersion,
                                        DeploymentPlatform platform, String contentDigest,
                                        List<BundleEntry> entries) throws IOException {
        final Path archiveParent = Objects.requireNonNull(archive.toAbsolutePath().getParent(),
                "Runtime archive must have a parent directory");
        final Path temporaryArchive = Files.createTempFile(archiveParent, fileName(archive), ".partial");
        final MessageDigest archiveDigest = newDigest();
        try {
            try (OutputStream fileOutput = Files.newOutputStream(temporaryArchive);
                 DigestOutputStream digestOutput = new DigestOutputStream(fileOutput, archiveDigest);
                 BufferedOutputStream bufferedOutput = new BufferedOutputStream(digestOutput, BUFFER_SIZE);
                 TarArchiveOutputStream tarOutput = new TarArchiveOutputStream(bufferedOutput)) {
                tarOutput.setLongFileMode(TarArchiveOutputStream.LONGFILE_POSIX);
                tarOutput.setBigNumberMode(TarArchiveOutputStream.BIGNUMBER_POSIX);
                addDirectoryEntry(tarOutput, ARCHIVE_ROOT + "/");
                for (BundleEntry entry : entries) {
                    addFilesystemEntry(tarOutput, entry);
                }
                addByteEntry(tarOutput, ARCHIVE_ROOT + "/" + DESCRIPTOR_FILE,
                        descriptor(sbkVersion, javaVersion, platform, contentDigest));
                addByteEntry(tarOutput, ARCHIVE_ROOT + "/" + CHECKSUM_FILE, checksums(entries));
            }
            moveAtomically(temporaryArchive, archive);
            return HexFormat.of().formatHex(archiveDigest.digest());
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
            archiveEntry.setSize(0);
        } else {
            archiveEntry = new TarArchiveEntry(archiveName);
            archiveEntry.setSize(bundleEntry.size());
        }
        archiveEntry.setMode(bundleEntry.mode());
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
                                     String contentDigest) {
        return "format.version=" + BUNDLE_FORMAT_VERSION + "\n"
                + "sbk.version=" + sbkVersion + "\n"
                + "java.version=" + javaVersion + "\n"
                + "platform.os=" + platform.operatingSystem() + "\n"
                + "content.sha256=" + contentDigest + "\n"
                + "includes.java=false\n";
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
            Files.move(source, target, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
        } catch (AtomicMoveNotSupportedException ignored) {
            Files.move(source, target, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    private static String cachedArchiveDigest(Path archive, Path digestFile, Path sizeFile) throws IOException {
        if (!Files.isRegularFile(archive) || !Files.isRegularFile(digestFile)
                || !Files.isRegularFile(sizeFile)) {
            return null;
        }
        final String expectedDigest = Files.readString(digestFile, StandardCharsets.UTF_8).trim();
        if (expectedDigest.length() != SHA_256_HEX_LENGTH) {
            return null;
        }
        final long expectedSize;
        try {
            expectedSize = Long.parseLong(Files.readString(sizeFile, StandardCharsets.UTF_8).trim());
        } catch (NumberFormatException ignored) {
            return null;
        }
        return expectedSize >= 0 && Files.size(archive) == expectedSize ? expectedDigest : null;
    }

    private static void writeAtomically(Path target, String value) throws IOException {
        final Path parent = Objects.requireNonNull(target.toAbsolutePath().getParent(),
                "Runtime cache file must have a parent directory");
        final Path temporaryFile = Files.createTempFile(parent, fileName(target), ".partial");
        try {
            Files.writeString(temporaryFile, value, StandardCharsets.UTF_8);
            moveAtomically(temporaryFile, target);
        } finally {
            Files.deleteIfExists(temporaryFile);
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
                               String digest, String linkTarget, int mode) {
    }

    /** Holds the cache lock while the archive is consumed by an asynchronous transfer. */
    static final class ArchiveUse implements AutoCloseable {
        private final ReentrantLock processLock;
        private final FileChannel channel;
        private final FileLock fileLock;

        private ArchiveUse(ReentrantLock processLock, FileChannel channel, FileLock fileLock) {
            this.processLock = processLock;
            this.channel = channel;
            this.fileLock = fileLock;
        }

        @Override
        public void close() throws IOException {
            try {
                fileLock.close();
            } finally {
                try {
                    channel.close();
                } finally {
                    processLock.unlock();
                }
            }
        }
    }
}
