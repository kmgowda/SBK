/**
 * Copyright (c) KMG. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.gem.agent;

import java.io.IOException;
import java.io.InputStream;
import java.net.SocketTimeoutException;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.FileVisitResult;
import java.nio.file.FileSystem;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

/** Implements managed-runtime ownership using local file-system operations on the remote node. */
public final class RemoteRuntimeFiles {
    static final String RUNTIME_PREFIX = "sbk-runtime-";
    static final String CURRENT_FILE = ".sbk-runtime-current";
    static final String LEASE_DIRECTORY = ".sbk-runtime-leases";
    static final String LOCK_DIRECTORY = ".sbk-runtime-management.lock";
    static final String RETIRED_PREFIX = ".sbk-runtime-retired.";
    private static final String ACTIVE_PREFIX = "active:";
    private static final String DESCRIPTOR_FILE = "deployment.properties";
    private static final String REMOTE_DIGEST_FILE = ".sbk-runtime.sha256";
    private static final String OWNER_FILE = "owner";
    private static final String CREATED_FILE = "created";
    private static final long LOCK_RETRY_MILLIS = 100;
    private static final long LOCK_INITIALIZATION_GRACE_SECONDS = 2;
    private static final int MAX_CONTROL_FILE_BYTES = 1024;
    private static final Pattern SAFE_IDENTIFIER = Pattern.compile("[A-Za-z0-9._-]+");

    private RemoteRuntimeFiles() {
    }

    /**
     * Identify an SBK-GEM-managed runtime artifact.
     *
     * @param name top-level file name
     * @return true when the name is reserved for managed runtime state
     */
    public static boolean isManagedArtifact(String name) {
        return name.startsWith(RUNTIME_PREFIX) || name.equals(CURRENT_FILE)
                || name.equals(LEASE_DIRECTORY) || name.startsWith(LOCK_DIRECTORY)
                || name.startsWith(RETIRED_PREFIX);
    }

    /**
     * Return the path of a managed runtime lease.
     *
     * @param parentDirectory managed runtime parent directory
     * @param deploymentName runtime deployment name
     * @param leaseId lease identifier
     * @return remote lease path
     */
    public static String leasePath(String parentDirectory, String deploymentName, String leaseId) {
        validateIdentifier(deploymentName, "deployment name");
        validateIdentifier(leaseId, "lease identifier");
        return parentDirectory + "/" + LEASE_DIRECTORY + "/" + deploymentName + "/" + leaseId;
    }

    /**
     * Resolve and create a remote working directory through its file-system provider.
     *
     * @param fileSystem remote file system
     * @param remoteDirectory configured directory
     * @return absolute normalized directory
     * @throws IllegalArgumentException when the configured directory is blank
     * @throws IOException when the directory cannot be resolved
     */
    public static String resolveDirectory(FileSystem fileSystem, String remoteDirectory) throws IOException {
        if (remoteDirectory == null || remoteDirectory.isBlank()) {
            throw new IllegalArgumentException("Remote directory must not be blank");
        }
        final Path directory = fileSystem.getPath(remoteDirectory).normalize();
        Files.createDirectories(directory);
        final String absoluteDirectory = directory.toRealPath().toString();
        if (!absoluteDirectory.startsWith("/")) {
            throw new IOException("Apache MINA SFTP returned a non-absolute remote directory: "
                    + absoluteDirectory);
        }
        return absoluteDirectory;
    }

    static void reserve(Path parent, String deploymentName, String leaseId,
                        long lockTimeoutSeconds, long lockStaleSeconds)
            throws IOException, InterruptedException {
        validateArguments(deploymentName, "0", leaseId, lockTimeoutSeconds, lockStaleSeconds, 1);
        withLock(parent, leaseId, lockTimeoutSeconds, lockStaleSeconds, () -> {
            final Path lease = lease(parent, deploymentName, leaseId);
            Files.createDirectories(Objects.requireNonNull(lease.getParent()));
            writeControlFile(lease, ACTIVE_PREFIX + epochSeconds());
        });
    }

    static void acquire(Path parent, String deploymentName, String contentDigest, String leaseId,
                        boolean cleanupEnabled, long lockTimeoutSeconds, long lockStaleSeconds,
                        long reservationSeconds) throws IOException, InterruptedException {
        validateArguments(deploymentName, contentDigest, leaseId, lockTimeoutSeconds,
                lockStaleSeconds, reservationSeconds);
        withLock(parent, leaseId, lockTimeoutSeconds, lockStaleSeconds, () -> {
            final Path runtime = parent.resolve(deploymentName);
            final String actualDigest = readControlFile(runtime.resolve(REMOTE_DIGEST_FILE));
            if (!contentDigest.equals(actualDigest)) {
                throw new IOException("Managed runtime digest mismatch for " + runtime);
            }
            final Path lease = lease(parent, deploymentName, leaseId);
            Files.createDirectories(Objects.requireNonNull(lease.getParent()));
            writeControlFile(lease, ACTIVE_PREFIX + epochSeconds());
            writeAtomically(parent.resolve(CURRENT_FILE), deploymentName, leaseId);
            if (cleanupEnabled) {
                retireInactiveRuntimes(parent, deploymentName, leaseId, reservationSeconds);
            }
        });
    }

    static void heartbeat(Path parent, String deploymentName, String leaseId,
                          long lockTimeoutSeconds, long lockStaleSeconds)
            throws IOException, InterruptedException {
        validateArguments(deploymentName, "0", leaseId, lockTimeoutSeconds, lockStaleSeconds, 1);
        withLock(parent, leaseId, lockTimeoutSeconds, lockStaleSeconds, () -> {
            final Path lease = lease(parent, deploymentName, leaseId);
            if (!Files.isRegularFile(lease, LinkOption.NOFOLLOW_LINKS)) {
                throw new IOException("Managed runtime lease is missing: " + lease);
            }
            writeControlFile(lease, ACTIVE_PREFIX + epochSeconds());
        });
    }

    static void release(Path parent, String deploymentName, String leaseId, boolean cleanupEnabled,
                        long lockTimeoutSeconds, long lockStaleSeconds, long reservationSeconds)
            throws IOException, InterruptedException {
        validateArguments(deploymentName, "0", leaseId, lockTimeoutSeconds,
                lockStaleSeconds, reservationSeconds);
        withLock(parent, leaseId, lockTimeoutSeconds, lockStaleSeconds, () -> {
            Files.deleteIfExists(lease(parent, deploymentName, leaseId));
            if (cleanupEnabled) {
                retireInactiveRuntimes(parent, currentRuntime(parent), leaseId, reservationSeconds);
            }
        });
    }

    static int deleteRetired(Path parent) throws IOException {
        if (!Files.isDirectory(parent, LinkOption.NOFOLLOW_LINKS)) {
            return 0;
        }
        int deleted = 0;
        try (var entries = Files.newDirectoryStream(parent, RETIRED_PREFIX + "*")) {
            for (Path entry : entries) {
                if (Files.isDirectory(entry, LinkOption.NOFOLLOW_LINKS)) {
                    deleteRecursively(entry);
                    deleted++;
                }
            }
        }
        return deleted;
    }

    private static void withLock(Path parent, String owner, long timeoutSeconds, long staleSeconds,
                                 IoOperation operation) throws IOException, InterruptedException {
        Files.createDirectories(parent);
        Files.createDirectories(parent.resolve(LEASE_DIRECTORY));
        final Path lock = parent.resolve(LOCK_DIRECTORY);
        final long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(timeoutSeconds);
        acquireLock(lock, owner, deadline, staleSeconds);
        try {
            operation.run();
        } finally {
            releaseLock(lock, owner);
        }
    }

    private static void acquireLock(Path lock, String owner, long deadline, long staleSeconds)
            throws IOException, InterruptedException {
        while (true) {
            try {
                Files.createDirectory(lock);
                writeControlFile(lock.resolve(OWNER_FILE), owner);
                writeControlFile(lock.resolve(CREATED_FILE), Long.toString(epochSeconds()));
                return;
            } catch (FileAlreadyExistsException exception) {
                reclaimStaleLock(lock, owner, staleSeconds);
            } catch (IOException exception) {
                if (!Files.isDirectory(lock, LinkOption.NOFOLLOW_LINKS)) {
                    throw exception;
                }
                reclaimStaleLock(lock, owner, staleSeconds);
            }
            if (System.nanoTime() >= deadline) {
                throw new SocketTimeoutException("Timed out waiting for runtime lifecycle lock: " + lock);
            }
            Thread.sleep(LOCK_RETRY_MILLIS);
        }
    }

    private static void reclaimStaleLock(Path lock, String owner, long staleSeconds) throws IOException {
        try {
            final long created = parseTimestamp(readControlFileIfPresent(lock.resolve(CREATED_FILE)));
            final long modified = Files.getLastModifiedTime(lock, LinkOption.NOFOLLOW_LINKS)
                    .toInstant().getEpochSecond();
            final long lockAge = epochSeconds() - (created > 0 ? created : modified);
            final long reclaimAge = created > 0 ? staleSeconds : LOCK_INITIALIZATION_GRACE_SECONDS;
            if (lockAge < reclaimAge) {
                return;
            }
            final Path stale = lock.resolveSibling(LOCK_DIRECTORY + ".stale." + owner
                    + "." + UUID.randomUUID());
            move(lock, stale, false);
            deleteRecursively(stale);
        } catch (java.nio.file.NoSuchFileException | FileAlreadyExistsException ignored) {
            // Another controller won the stale-lock reclamation race.
        }
    }

    private static void releaseLock(Path lock, String owner) throws IOException {
        if (owner.equals(readControlFileIfPresent(lock.resolve(OWNER_FILE)))) {
            deleteRecursively(lock);
        }
    }

    private static void retireInactiveRuntimes(Path parent, String current, String leaseId,
                                               long reservationSeconds) throws IOException {
        final long now = epochSeconds();
        try (var entries = Files.newDirectoryStream(parent, RUNTIME_PREFIX + "*")) {
            for (Path candidate : entries) {
                final String candidateName = String.valueOf(candidate.getFileName());
                if (candidateName.equals(current)
                        || !Files.isDirectory(candidate, LinkOption.NOFOLLOW_LINKS)
                        || !Files.isRegularFile(candidate.resolve(DESCRIPTOR_FILE),
                        LinkOption.NOFOLLOW_LINKS)
                        || !Files.isRegularFile(candidate.resolve(REMOTE_DIGEST_FILE),
                        LinkOption.NOFOLLOW_LINKS)
                        || hasActiveLease(parent, candidateName, now, reservationSeconds)) {
                    continue;
                }
                final Path retired = parent.resolve(RETIRED_PREFIX + candidateName + "." + leaseId
                        + "." + UUID.randomUUID());
                move(candidate, retired, false);
                deleteRecursively(parent.resolve(LEASE_DIRECTORY).resolve(candidateName));
            }
        }
    }

    private static boolean hasActiveLease(Path parent, String deploymentName, long now,
                                          long reservationSeconds) throws IOException {
        final Path leases = parent.resolve(LEASE_DIRECTORY).resolve(deploymentName);
        if (!Files.isDirectory(leases, LinkOption.NOFOLLOW_LINKS)) {
            return false;
        }
        boolean active = false;
        try (var entries = Files.newDirectoryStream(leases)) {
            for (Path entry : entries) {
                if (!Files.isRegularFile(entry, LinkOption.NOFOLLOW_LINKS)) {
                    continue;
                }
                final String value = readControlFileIfPresent(entry);
                final long timestamp = value.startsWith(ACTIVE_PREFIX)
                        ? parseTimestamp(value.substring(ACTIVE_PREFIX.length())) : 0;
                if (timestamp > 0 && now - timestamp <= reservationSeconds) {
                    active = true;
                } else {
                    Files.deleteIfExists(entry);
                }
            }
        }
        return active;
    }

    private static Path lease(Path parent, String deploymentName, String leaseId) {
        return parent.resolve(LEASE_DIRECTORY).resolve(deploymentName).resolve(leaseId);
    }

    private static String currentRuntime(Path parent) throws IOException {
        return readControlFileIfPresent(parent.resolve(CURRENT_FILE));
    }

    private static void writeAtomically(Path destination, String value, String suffix) throws IOException {
        final Path temporary = destination.resolveSibling(destination.getFileName() + "." + suffix + ".tmp");
        writeControlFile(temporary, value);
        move(temporary, destination, true);
    }

    private static void writeControlFile(Path path, String value) throws IOException {
        Files.writeString(path, value + System.lineSeparator(), StandardCharsets.UTF_8);
    }

    private static String readControlFile(Path path) throws IOException {
        if (!Files.isRegularFile(path, LinkOption.NOFOLLOW_LINKS)) {
            throw new IOException("Required managed runtime file is missing: " + path);
        }
        return readControlFileIfPresent(path);
    }

    private static String readControlFileIfPresent(Path path) throws IOException {
        if (!Files.isRegularFile(path, LinkOption.NOFOLLOW_LINKS)) {
            return "";
        }
        try (InputStream input = Files.newInputStream(path)) {
            final byte[] bytes = input.readNBytes(MAX_CONTROL_FILE_BYTES + 1);
            if (bytes.length > MAX_CONTROL_FILE_BYTES) {
                throw new IOException("Managed runtime control file is too large: " + path);
            }
            return new String(bytes, StandardCharsets.UTF_8).trim();
        }
    }

    private static void move(Path source, Path destination, boolean replace) throws IOException {
        try {
            if (replace) {
                Files.move(source, destination, StandardCopyOption.ATOMIC_MOVE,
                        StandardCopyOption.REPLACE_EXISTING);
            } else {
                Files.move(source, destination, StandardCopyOption.ATOMIC_MOVE);
            }
        } catch (AtomicMoveNotSupportedException exception) {
            if (replace) {
                Files.move(source, destination, StandardCopyOption.REPLACE_EXISTING);
            } else {
                Files.move(source, destination);
            }
        }
    }

    private static void deleteRecursively(Path root) throws IOException {
        if (!Files.exists(root, LinkOption.NOFOLLOW_LINKS)) {
            return;
        }
        Files.walkFileTree(root, new SimpleFileVisitor<>() {
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attributes) throws IOException {
                Files.deleteIfExists(file);
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult postVisitDirectory(Path directory, IOException failure) throws IOException {
                if (failure != null) {
                    throw failure;
                }
                Files.deleteIfExists(directory);
                return FileVisitResult.CONTINUE;
            }
        });
    }

    private static long epochSeconds() {
        return Instant.now().getEpochSecond();
    }

    private static long parseTimestamp(String value) {
        try {
            return Long.parseLong(value);
        } catch (NumberFormatException exception) {
            return 0;
        }
    }

    private static void validateArguments(String deploymentName, String contentDigest, String leaseId,
                                          long lockTimeoutSeconds, long lockStaleSeconds,
                                          long reservationSeconds) {
        validateIdentifier(deploymentName, "deployment name");
        validateIdentifier(contentDigest, "content digest");
        validateIdentifier(leaseId, "lease identifier");
        if (lockTimeoutSeconds < 1 || lockStaleSeconds < 1 || reservationSeconds < 1) {
            throw new IllegalArgumentException("Remote runtime lifecycle timeouts must be positive");
        }
    }

    private static void validateIdentifier(String value, String description) {
        if (value == null || !SAFE_IDENTIFIER.matcher(value).matches()) {
            throw new IllegalArgumentException("Invalid " + description + ": " + value);
        }
    }

    @FunctionalInterface
    private interface IoOperation {
        void run() throws IOException;
    }
}
