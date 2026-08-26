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

import io.gem.api.SshSession;
import io.sbk.config.ExitCode;
import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveOutputStream;
import org.apache.commons.compress.archivers.tar.TarConstants;

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
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.HexFormat;
import java.util.List;
import java.util.Objects;
import java.util.Properties;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.function.LongConsumer;
import java.util.stream.Stream;

/** Content-addressed full or compact Java tree copied independently from the SBK runtime. */
final class ManagedJavaRuntime {
    private static final String MARKER = ".sbk-java.sha256";
    private static final String COMPACT_IMAGE_MARKER = ".sbk-java-runtime.identity";
    private static final String IDENTITY_CACHE_FORMAT = "1";
    private static final String SHA_256 = "SHA-256";
    private static final int SHA_256_HEX_LENGTH = 64;
    private static final int HASH_BUFFER_SIZE = 64 * 1024;
    private static final int COPY_BUFFER_SIZE = 256 * 1024;
    private static final int FILE_COPY_CONCURRENCY = 8;
    private static final int IDENTITY_CHARACTERS = 24;
    private static final int MAXIMUM_JLINK_DIAGNOSTIC_BYTES = 64 * 1024;
    private static final int REGULAR_FILE_MODE = 0644;
    private static final int EXECUTABLE_FILE_MODE = 0755;
    private static final int SYMBOLIC_LINK_MODE = 0777;
    private static final LongConsumer NO_COPY_PROGRESS = ignored -> { };
    private final Path localHome;
    private final String digest;
    private final String directoryName;
    private final long contentBytes;
    private final Path cacheDirectory;
    private final boolean compilerRequired;
    private volatile Path archive;
    private volatile long archiveBytes;
    private volatile boolean archiveReused;

    private ManagedJavaRuntime(Path localHome, String digest, String directoryName, long contentBytes,
                               Path cacheDirectory, boolean compilerRequired) {
        this.localHome = localHome;
        this.digest = digest;
        this.directoryName = directoryName;
        this.contentBytes = contentBytes;
        this.cacheDirectory = cacheDirectory;
        this.compilerRequired = compilerRequired;
    }

    static ManagedJavaRuntime create(Path javaHome, int major) throws IOException {
        return create(javaHome, major, null, true, "sbk-java");
    }

    private static ManagedJavaRuntime create(Path javaHome, int major, Path cacheDirectory,
                                             boolean compilerRequired, String directoryPrefix) throws IOException {
        final Path home = javaHome.toAbsolutePath().normalize();
        validateJavaHome(home, compilerRequired);
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
        return runtime(home, major, identity, contentBytes, cacheDirectory, compilerRequired, directoryPrefix);
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
        return createCached(javaHome, major, cacheDirectory, true, "sbk-java");
    }

    private static ManagedJavaRuntime createCached(Path javaHome, int major, Path cacheDirectory,
                                                   boolean compilerRequired, String directoryPrefix)
            throws IOException {
        final Path home = javaHome.toAbsolutePath().normalize();
        validateJavaHome(home, compilerRequired);
        Files.createDirectories(cacheDirectory);
        final String cacheKey = sha256((home + "\0" + major + "\0" + directoryPrefix)
                .getBytes(StandardCharsets.UTF_8));
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
                    return runtime(home, major, cachedDigest, metadata.contentBytes(), cacheDirectory,
                            compilerRequired, directoryPrefix);
                }
                final ManagedJavaRuntime runtime = create(home, major, cacheDirectory,
                        compilerRequired, directoryPrefix);
                final Properties identity = new Properties();
                identity.setProperty("format.version", IDENTITY_CACHE_FORMAT);
                identity.setProperty("java.home", home.toString());
                identity.setProperty("java.major", Integer.toString(major));
                identity.setProperty("metadata.sha256", metadata.digest());
                identity.setProperty("content.sha256", runtime.digest);
                writeIdentity(identityFile, identity);
                return runtime(home, major, runtime.digest, runtime.contentBytes, cacheDirectory,
                        compilerRequired, directoryPrefix);
            }
        }
    }

    /**
     * Generate or reuse a compact Java runtime image using the Gradle-produced runtime descriptor.
     *
     * @param javaHome complete controller JDK containing jlink and jmods
     * @param major required Java major version
     * @param cacheDirectory local content-addressed SBK-GEM cache
     * @param descriptor validated compact-runtime build contract
     * @return managed compact Java runtime
     * @throws IOException when jlink is unavailable or runtime generation fails
     */
    static ManagedJavaRuntime createCompact(Path javaHome, int major, Path cacheDirectory,
                                            CompactJavaRuntimeDescriptor descriptor) throws IOException {
        if (descriptor.javaMajor() != major) {
            throw new IOException("Compact Java runtime descriptor does not match Java " + major);
        }
        final ManagedJavaRuntime source = createCached(javaHome, major, cacheDirectory, true, "sbk-java");
        final String imageIdentity = sha256((source.digest + "\0" + descriptor.identity())
                .getBytes(StandardCharsets.UTF_8));
        final Path image = cacheDirectory.resolve("sbk-java-runtime-image-"
                + imageIdentity.substring(0, IDENTITY_CHARACTERS));
        final Path lock = image.resolveSibling(image.getFileName() + ".lock");
        synchronized (ManagedJavaRuntime.class) {
            try (FileChannel channel = FileChannel.open(lock, java.nio.file.StandardOpenOption.CREATE,
                    java.nio.file.StandardOpenOption.WRITE);
                 FileLock ignored = channel.lock()) {
                if (!hasCompactImage(image, imageIdentity)) {
                    createCompactImage(javaHome.toAbsolutePath().normalize(), image, imageIdentity, descriptor);
                }
            }
        }
        return createRuntimeImage(image, major, cacheDirectory);
    }

    static ManagedJavaRuntime createRuntimeImage(Path javaHome, int major, Path cacheDirectory) throws IOException {
        return createCached(javaHome, major, cacheDirectory, false, "sbk-java-runtime");
    }

    private static ManagedJavaRuntime runtime(Path home, int major, String identity, long contentBytes,
                                              Path cacheDirectory, boolean compilerRequired,
                                              String directoryPrefix) {
        return new ManagedJavaRuntime(home, identity,
                directoryPrefix + "-" + major + "-" + identity.substring(0, IDENTITY_CHARACTERS), contentBytes,
                cacheDirectory, compilerRequired);
    }

    private static void validateJavaHome(Path home, boolean compilerRequired) throws IOException {
        if (!Files.isExecutable(home.resolve("bin/java"))) {
            throw new IOException("Java runtime is incomplete: " + home);
        }
        if (compilerRequired && !Files.isExecutable(home.resolve("bin/javac"))) {
            throw new IOException("Controller JDK compiler is missing: " + home);
        }
    }

    private static boolean hasCompactImage(Path image, String expectedIdentity) throws IOException {
        final Path marker = image.resolve(COMPACT_IMAGE_MARKER);
        return Files.isRegularFile(marker)
                && expectedIdentity.equals(Files.readString(marker, StandardCharsets.UTF_8).trim())
                && Files.isExecutable(image.resolve("bin/java"))
                && !Files.exists(image.resolve("bin/javac"), LinkOption.NOFOLLOW_LINKS);
    }

    private static void createCompactImage(Path javaHome, Path image, String imageIdentity,
                                           CompactJavaRuntimeDescriptor descriptor) throws IOException {
        final Path jlink = javaHome.resolve("bin/jlink");
        final Path jmods = javaHome.resolve("jmods");
        if (!Files.isExecutable(jlink) || !Files.isDirectory(jmods)) {
            throw new IOException("Controller JDK cannot generate a compact Java runtime: " + javaHome);
        }
        final Path staging = image.resolveSibling(image.getFileName() + ".staging." + UUID.randomUUID());
        final List<String> command = new ArrayList<>();
        command.add(jlink.toString());
        command.add("--module-path");
        command.add(jmods.toString());
        command.add("--add-modules");
        command.add(String.join(",", descriptor.modules()));
        command.addAll(descriptor.options());
        command.add("--output");
        command.add(staging.toString());
        try {
            final Process process = new ProcessBuilder(command).redirectErrorStream(true).start();
            final byte[] diagnostic;
            try (InputStream output = process.getInputStream()) {
                diagnostic = output.readNBytes(MAXIMUM_JLINK_DIAGNOSTIC_BYTES);
                output.transferTo(OutputStream.nullOutputStream());
            }
            final int exitCode;
            try {
                exitCode = process.waitFor();
            } catch (InterruptedException exception) {
                process.destroyForcibly();
                Thread.currentThread().interrupt();
                throw new IOException("Interrupted while generating the compact Java runtime", exception);
            }
            if (exitCode != ExitCode.SUCCESS || !Files.isExecutable(staging.resolve("bin/java"))
                    || Files.exists(staging.resolve("bin/javac"), LinkOption.NOFOLLOW_LINKS)) {
                throw new IOException("jlink failed to generate the compact Java runtime (exit " + exitCode
                        + "): " + new String(diagnostic, StandardCharsets.UTF_8));
            }
            Files.writeString(staging.resolve(COMPACT_IMAGE_MARKER), imageIdentity + System.lineSeparator(),
                    StandardCharsets.UTF_8);
            deleteRecursively(image);
            move(staging, image);
        } finally {
            deleteRecursively(staging);
        }
    }

    String directoryName() {
        return directoryName;
    }

    long contentBytes() {
        return contentBytes;
    }

    Path prepareArchive() throws IOException {
        if (cacheDirectory == null) {
            throw new IOException("Managed JDK archive cache is unavailable");
        }
        final Path target = cacheDirectory.resolve(directoryName + ".tar");
        final Path descriptor = target.resolveSibling(target.getFileName() + ".properties");
        final Path lock = target.resolveSibling(target.getFileName() + ".lock");
        synchronized (this) {
            try (FileChannel channel = FileChannel.open(lock, java.nio.file.StandardOpenOption.CREATE,
                    java.nio.file.StandardOpenOption.WRITE);
                 FileLock ignored = channel.lock()) {
                final Properties metadata = loadIdentity(descriptor);
                if (Files.isRegularFile(target) && digest.equals(metadata.getProperty("content.sha256"))
                        && Long.toString(Files.size(target)).equals(metadata.getProperty("archive.bytes"))) {
                    archive = target;
                    archiveBytes = Files.size(target);
                    archiveReused = true;
                    return target;
                }
                createArchive(target);
                final Properties updated = new Properties();
                updated.setProperty("content.sha256", digest);
                updated.setProperty("archive.bytes", Long.toString(Files.size(target)));
                writeIdentity(descriptor, updated);
                archive = target;
                archiveBytes = Files.size(target);
                archiveReused = false;
                return target;
            }
        }
    }

    long archiveBytes() {
        return archiveBytes;
    }

    boolean archiveReused() {
        return archiveReused;
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
        final Path retired = retireInvalidIdentity(destination);
        final Path staging = parent.resolve(directoryName + ".staging." + UUID.randomUUID());
        Files.createDirectories(staging);
        try {
            copyTree(staging, retired, copyProgress);
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

    CompletableFuture<String> installBulk(SshSession session, String parentDirectory, long timeoutSeconds,
                                           LongConsumer copyProgress) throws java.net.ConnectException {
        final Path localArchive = Objects.requireNonNull(archive, "Managed JDK archive was not prepared");
        final CompletableFuture<BulkInstall> prepared = session.runRemoteTransferOperationAsync(fileSystem -> {
            final Path parent = fileSystem.getPath(parentDirectory);
            final Path destination = parent.resolve(directoryName);
            if (hasUsableExpectedIdentity(destination)) {
                return new BulkInstall(destination.toString(), null, null, null, true);
            }
            final Path retired = retireInvalidIdentity(destination);
            final Path staging = parent.resolve(directoryName + ".staging." + UUID.randomUUID());
            Files.createDirectories(staging);
            return new BulkInstall(destination.toString(), staging.toString(), staging + ".tar",
                    retired == null ? null : retired.toString(), false);
        }, timeoutSeconds);
        return prepared.thenCompose(plan -> {
            if (plan.available()) {
                return CompletableFuture.completedFuture(plan.destination());
            }
            try {
                final CompletableFuture<String> deployment = session.copyFileAsync(localArchive.toString(),
                                plan.remoteArchive(), timeoutSeconds, copyProgress)
                        .thenCompose(ignored -> extractBulkArchive(session, plan, timeoutSeconds))
                        .thenCompose(ignored -> activateBulk(session, plan, timeoutSeconds));
                deployment.whenComplete((installed, failure) -> cleanupBulkInstall(session, plan, timeoutSeconds));
                return deployment;
            } catch (java.net.ConnectException exception) {
                return CompletableFuture.failedFuture(exception);
            }
        });
    }

    private CompletableFuture<Void> extractBulkArchive(SshSession session, BulkInstall plan, long timeoutSeconds) {
        final String command = "tar -xf " + quote(plan.remoteArchive()) + " -C " + quote(plan.staging());
        try {
            return session.runCommandAsync(command, true, timeoutSeconds).thenCompose(response -> {
                if (response.returnCode == ExitCode.SUCCESS) {
                    return CompletableFuture.completedFuture(null);
                }
                return CompletableFuture.failedFuture(new IOException("Remote JDK archive extraction failed: "
                        + response.errOutputStream));
            });
        } catch (java.net.ConnectException exception) {
            return CompletableFuture.failedFuture(exception);
        }
    }

    private CompletableFuture<String> activateBulk(SshSession session, BulkInstall plan, long timeoutSeconds) {
        try {
            final CompletableFuture<String> activation = session.runRemoteTransferOperationAsync(fileSystem -> {
                final Path staging = fileSystem.getPath(plan.staging());
                final Path destination = fileSystem.getPath(plan.destination());
                copyPermissions(localHome.resolve("bin/java"), staging.resolve("bin/java"));
                if (compilerRequired) {
                    copyPermissions(localHome.resolve("bin/javac"), staging.resolve("bin/javac"));
                }
                if (!isExecutable(staging.resolve("bin/java"))
                        || (compilerRequired && !isExecutable(staging.resolve("bin/javac")))) {
                    throw new IOException("Bulk SCP transfer did not preserve executable Java files under "
                            + staging + "; entries: " + listNames(staging));
                }
                Files.writeString(staging.resolve(MARKER), digest + System.lineSeparator(),
                        StandardCharsets.UTF_8);
                if (!hasUsableExpectedIdentity(destination)) {
                    try {
                        move(staging, destination);
                    } catch (IOException exception) {
                        if (!hasUsableExpectedIdentity(destination)) {
                            throw new IOException("Managed JDK destination exists without the expected identity: "
                                    + destination, exception);
                        }
                    }
                }
                return destination.toString();
            }, timeoutSeconds);
            return activation;
        } catch (java.net.ConnectException exception) {
            return CompletableFuture.failedFuture(exception);
        }
    }

    private static void cleanupBulkInstall(SshSession session, BulkInstall plan, long timeoutSeconds) {
        try {
            session.runRemoteTransferOperationAsync(fileSystem -> {
                if (plan.staging() != null) {
                    deleteRecursively(fileSystem.getPath(plan.staging()));
                }
                if (plan.retired() != null) {
                    deleteRecursively(fileSystem.getPath(plan.retired()));
                }
                if (plan.remoteArchive() != null) {
                    Files.deleteIfExists(fileSystem.getPath(plan.remoteArchive()));
                }
                return null;
            }, timeoutSeconds);
        } catch (java.net.ConnectException ignored) {
            // A later deployment cleanup can remove interrupted staging and retired trees.
        }
    }

    private static List<String> listNames(Path directory) throws IOException {
        try (Stream<Path> entries = Files.list(directory)) {
            return entries.map(path -> Objects.requireNonNull(path.getFileName()).toString()).sorted().toList();
        }
    }

    private static String quote(String value) {
        if (value == null || value.isBlank() || value.indexOf('\n') >= 0 || value.indexOf('\r') >= 0
                || value.indexOf('\0') >= 0) {
            throw new IllegalArgumentException("Invalid remote JDK path");
        }
        return "'" + value.replace("'", "'\\''") + "'";
    }

    private boolean hasUsableExpectedIdentity(Path destination) throws IOException {
        final Path marker = destination.resolve(MARKER);
        return Files.isRegularFile(marker) && digest.equals(Files.readString(marker).trim())
                && isExecutable(destination.resolve("bin/java"))
                && (!compilerRequired || isExecutable(destination.resolve("bin/javac")));
    }

    private static boolean isExecutable(Path path) throws IOException {
        try {
            final Set<PosixFilePermission> permissions = Files.getPosixFilePermissions(path,
                    LinkOption.NOFOLLOW_LINKS);
            return permissions.contains(PosixFilePermission.OWNER_EXECUTE)
                    || permissions.contains(PosixFilePermission.GROUP_EXECUTE)
                    || permissions.contains(PosixFilePermission.OTHERS_EXECUTE);
        } catch (UnsupportedOperationException exception) {
            return Files.isExecutable(path);
        }
    }

    private static Path retireInvalidIdentity(Path destination) throws IOException {
        if (!Files.exists(destination, LinkOption.NOFOLLOW_LINKS)) {
            return null;
        }
        final Path retired = destination.resolveSibling(destination.getFileName() + ".invalid." + UUID.randomUUID());
        try {
            move(destination, retired);
            return retired;
        } catch (NoSuchFileException exception) {
            if (Files.exists(destination, LinkOption.NOFOLLOW_LINKS)) {
                throw exception;
            }
            return null;
        }
    }

    private void copyTree(Path staging, Path retired, LongConsumer copyProgress) throws IOException {
        try (Stream<Path> entries = Files.walk(localHome)) {
            copyEntries(staging, retired, copyProgress, entries.filter(path -> !path.equals(localHome)).toList());
        }
    }

    private void copyEntries(Path staging, Path retired, LongConsumer copyProgress, List<Path> entries)
            throws IOException {
        final List<JdkFile> files = new ArrayList<>();
        for (Path entry : entries) {
            if (Files.isRegularFile(entry, LinkOption.NOFOLLOW_LINKS)) {
                files.add(new JdkFile(entry, Files.size(entry)));
            }
        }
        files.sort(Comparator.comparingLong(JdkFile::size).reversed());
        final Set<Path> createdDirectories = new HashSet<>();
        final int workers = Math.min(FILE_COPY_CONCURRENCY, Math.max(1, files.size()));
        try (ExecutorService executor = Executors.newFixedThreadPool(workers,
                Thread.ofVirtual().name("sbk-gem-jdk-copy-", 0).factory())) {
            final List<Future<?>> copies = new ArrayList<>(files.size() + 1);
            if (retired != null) {
                copies.add(executor.submit(() -> {
                    deleteRecursively(retired);
                    return null;
                }));
            }
            for (JdkFile file : files) {
                final Path source = file.path();
                createParentDirectories(source.getParent(), staging, createdDirectories);
                copies.add(executor.submit(() -> {
                    copyFile(source, staging.resolve(localHome.relativize(source).toString()), copyProgress);
                    return null;
                }));
            }
            for (Path source : entries) {
                if (Files.isDirectory(source, LinkOption.NOFOLLOW_LINKS)) {
                    createParentDirectories(source.getParent(), staging, createdDirectories);
                    createDirectory(source, staging, createdDirectories);
                } else if (!Files.isSymbolicLink(source)
                        && !Files.isRegularFile(source, LinkOption.NOFOLLOW_LINKS)) {
                    throw new IOException("Unsupported JDK filesystem entry: " + source);
                }
            }
            awaitCopies(copies);

            final List<Future<?>> links = new ArrayList<>();
            for (Path source : entries) {
                if (Files.isSymbolicLink(source)) {
                    final Path target = staging.resolve(localHome.relativize(source).toString());
                    final Path linkTarget = Files.readSymbolicLink(source);
                    links.add(executor.submit(() -> {
                        Files.createSymbolicLink(target, remoteLinkTarget(target, linkTarget));
                        return null;
                    }));
                }
            }
            awaitCopies(links);

            final List<Future<?>> permissions = new ArrayList<>();
            for (Path source : entries.stream()
                    .filter(path -> !Files.isSymbolicLink(path))
                    .sorted(Comparator.comparingInt(Path::getNameCount).reversed()).toList()) {
                final Path target = staging.resolve(localHome.relativize(source).toString());
                permissions.add(executor.submit(() -> {
                    copyPermissions(source, target);
                    return null;
                }));
            }
            awaitCopies(permissions);
        }
    }

    private void createParentDirectories(Path sourceParent, Path staging, Set<Path> createdDirectories)
            throws IOException {
        if (sourceParent == null || sourceParent.equals(localHome)) {
            return;
        }
        createParentDirectories(sourceParent.getParent(), staging, createdDirectories);
        createDirectory(sourceParent, staging, createdDirectories);
    }

    private void createDirectory(Path source, Path staging, Set<Path> createdDirectories) throws IOException {
        if (createdDirectories.add(source)) {
            Files.createDirectory(staging.resolve(localHome.relativize(source).toString()));
        }
    }

    private static void copyFile(Path source, Path target, LongConsumer copyProgress) throws IOException {
        final byte[] copyBuffer = new byte[COPY_BUFFER_SIZE];
        try (InputStream input = new BufferedInputStream(Files.newInputStream(source), COPY_BUFFER_SIZE);
             OutputStream output = new BufferedOutputStream(Files.newOutputStream(target), COPY_BUFFER_SIZE)) {
            int copied;
            while ((copied = input.read(copyBuffer)) >= 0) {
                output.write(copyBuffer, 0, copied);
                copyProgress.accept(copied);
            }
        }
    }

    private static void awaitCopies(List<Future<?>> copies) throws IOException {
        try {
            for (Future<?> copy : copies) {
                copy.get();
            }
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IOException("Interrupted while copying the managed JDK", exception);
        } catch (ExecutionException exception) {
            final Throwable cause = exception.getCause();
            if (cause instanceof IOException ioException) {
                throw ioException;
            }
            throw new IOException("Managed JDK copy failed", cause);
        }
    }

    static Path remoteLinkTarget(Path target, Path localLinkTarget) {
        return target.getFileSystem().getPath(localLinkTarget.toString());
    }

    private static void copyPermissions(Path source, Path target) throws IOException {
        if (!Files.isRegularFile(source, LinkOption.NOFOLLOW_LINKS) || !Files.isExecutable(source)) {
            return;
        }
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
        move(source, destination, false);
    }

    private static void move(Path source, Path destination, boolean replace) throws IOException {
        final StandardCopyOption[] atomicOptions = replace
                ? new StandardCopyOption[]{StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING}
                : new StandardCopyOption[]{StandardCopyOption.ATOMIC_MOVE};
        final StandardCopyOption[] regularOptions = replace
                ? new StandardCopyOption[]{StandardCopyOption.REPLACE_EXISTING}
                : new StandardCopyOption[0];
        try {
            Files.move(source, destination, atomicOptions);
        } catch (AtomicMoveNotSupportedException exception) {
            Files.move(source, destination, regularOptions);
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
            return MessageDigest.getInstance(SHA_256);
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
        if (value.length() != SHA_256_HEX_LENGTH) {
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

    private void createArchive(Path target) throws IOException {
        final Path temporary = Files.createTempFile(Objects.requireNonNull(target.getParent()),
                Objects.requireNonNull(target.getFileName()).toString(), ".partial");
        try {
            try (OutputStream file = Files.newOutputStream(temporary);
                 BufferedOutputStream buffered = new BufferedOutputStream(file, COPY_BUFFER_SIZE);
                 TarArchiveOutputStream output = new TarArchiveOutputStream(buffered)) {
                output.setLongFileMode(TarArchiveOutputStream.LONGFILE_POSIX);
                output.setBigNumberMode(TarArchiveOutputStream.BIGNUMBER_POSIX);
                try (Stream<Path> paths = Files.walk(localHome)) {
                    for (Path source : paths.filter(path -> !path.equals(localHome))
                            .sorted(Comparator.comparing(Path::toString)).toList()) {
                        final String relative = localHome.relativize(source).toString().replace('\\', '/');
                        final TarArchiveEntry entry;
                        if (Files.isSymbolicLink(source)) {
                            final Path link = Files.readSymbolicLink(source);
                            final Path resolved = Objects.requireNonNull(source.getParent()).resolve(link).normalize();
                            if (link.isAbsolute() || !resolved.startsWith(localHome)) {
                                throw new IOException("Managed JDK symbolic link escapes Java home: " + source);
                            }
                            entry = new TarArchiveEntry(relative, TarConstants.LF_SYMLINK);
                            entry.setLinkName(link.toString().replace('\\', '/'));
                        } else if (Files.isDirectory(source, LinkOption.NOFOLLOW_LINKS)) {
                            entry = new TarArchiveEntry(relative + "/");
                        } else if (Files.isRegularFile(source, LinkOption.NOFOLLOW_LINKS)) {
                            entry = new TarArchiveEntry(relative);
                            entry.setSize(Files.size(source));
                        } else {
                            throw new IOException("Unsupported managed JDK entry: " + source);
                        }
                        entry.setMode(posixMode(source));
                        output.putArchiveEntry(entry);
                        if (Files.isRegularFile(source, LinkOption.NOFOLLOW_LINKS)) {
                            Files.copy(source, output);
                        }
                        output.closeArchiveEntry();
                    }
                }
                output.finish();
            }
            move(temporary, target, true);
        } finally {
            Files.deleteIfExists(temporary);
        }
    }

    private static int posixMode(Path path) throws IOException {
        if (Files.isSymbolicLink(path)) {
            return SYMBOLIC_LINK_MODE;
        }
        try {
            int mode = 0;
            for (PosixFilePermission permission : Files.getPosixFilePermissions(path,
                    LinkOption.NOFOLLOW_LINKS)) {
                mode |= 1 << (8 - permission.ordinal());
            }
            return mode;
        } catch (UnsupportedOperationException exception) {
            return Files.isExecutable(path) ? EXECUTABLE_FILE_MODE : REGULAR_FILE_MODE;
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

    private record BulkInstall(String destination, String staging, String remoteArchive, String retired,
                               boolean available) {
    }

    private record JdkFile(Path path, long size) {
    }
}
