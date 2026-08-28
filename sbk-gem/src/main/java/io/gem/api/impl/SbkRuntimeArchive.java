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
import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveOutputStream;
import org.apache.commons.compress.archivers.tar.TarConstants;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.security.DigestOutputStream;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.List;
import java.util.Objects;

/** Serializes a validated SBK runtime file inventory into its immutable tar format. */
final class SbkRuntimeArchive {
    private static final int BUFFER_SIZE = 64 * 1024;
    private static final int REGULAR_FILE_MODE = 0644;
    private static final int DIRECTORY_MODE = 0755;

    private SbkRuntimeArchive() {
    }

    static String create(Path archive, String sbkVersion, int javaVersion, DeploymentPlatform platform,
                         String contentDigest, int formatVersion, List<Entry> entries) throws IOException {
        final Path parent = Objects.requireNonNull(archive.toAbsolutePath().getParent(),
                "Runtime archive must have a parent directory");
        final Path temporary = Files.createTempFile(parent, fileName(archive), ".partial");
        final MessageDigest digest = DigestSupport.newSha256();
        try {
            try (OutputStream file = Files.newOutputStream(temporary);
                 DigestOutputStream hashing = new DigestOutputStream(file, digest);
                 BufferedOutputStream buffered = new BufferedOutputStream(hashing, BUFFER_SIZE);
                 TarArchiveOutputStream tar = new TarArchiveOutputStream(buffered)) {
                tar.setLongFileMode(TarArchiveOutputStream.LONGFILE_POSIX);
                tar.setBigNumberMode(TarArchiveOutputStream.BIGNUMBER_POSIX);
                addDirectory(tar, SbkRuntimeBundle.ARCHIVE_ROOT + "/");
                for (Entry entry : entries) {
                    addFilesystemEntry(tar, entry);
                }
                addText(tar, SbkRuntimeBundle.ARCHIVE_ROOT + "/" + SbkRuntimeBundle.DESCRIPTOR_FILE,
                        descriptor(sbkVersion, javaVersion, platform, contentDigest, formatVersion));
                addText(tar, SbkRuntimeBundle.ARCHIVE_ROOT + "/" + SbkRuntimeBundle.CHECKSUM_FILE,
                        checksums(entries));
            }
            moveAtomically(temporary, archive);
            return HexFormat.of().formatHex(digest.digest());
        } finally {
            Files.deleteIfExists(temporary);
        }
    }

    private static void addFilesystemEntry(TarArchiveOutputStream output, Entry entry) throws IOException {
        String archiveName = SbkRuntimeBundle.ARCHIVE_ROOT + "/" + entry.relativePath();
        if (entry.type() == Type.DIRECTORY) {
            archiveName += "/";
        }
        final TarArchiveEntry tarEntry;
        if (entry.type() == Type.SYMBOLIC_LINK) {
            tarEntry = new TarArchiveEntry(archiveName, TarConstants.LF_SYMLINK);
            tarEntry.setLinkName(entry.linkTarget());
            tarEntry.setSize(0);
        } else {
            tarEntry = new TarArchiveEntry(archiveName);
            tarEntry.setSize(entry.size());
        }
        tarEntry.setMode(entry.mode());
        output.putArchiveEntry(tarEntry);
        if (entry.type() == Type.REGULAR_FILE) {
            try (BufferedInputStream input = new BufferedInputStream(Files.newInputStream(entry.source()),
                    BUFFER_SIZE)) {
                input.transferTo(output);
            }
        }
        output.closeArchiveEntry();
    }

    private static void addDirectory(TarArchiveOutputStream output, String name) throws IOException {
        final TarArchiveEntry entry = new TarArchiveEntry(name);
        entry.setMode(DIRECTORY_MODE);
        output.putArchiveEntry(entry);
        output.closeArchiveEntry();
    }

    private static void addText(TarArchiveOutputStream output, String name, String value) throws IOException {
        final byte[] bytes = value.getBytes(StandardCharsets.UTF_8);
        final TarArchiveEntry entry = new TarArchiveEntry(name);
        entry.setMode(REGULAR_FILE_MODE);
        entry.setSize(bytes.length);
        output.putArchiveEntry(entry);
        output.write(bytes);
        output.closeArchiveEntry();
    }

    private static String descriptor(String sbkVersion, int javaVersion, DeploymentPlatform platform,
                                     String contentDigest, int formatVersion) {
        return RemoteDeploymentContract.FORMAT_VERSION_PROPERTY + "=" + formatVersion + "\n"
                + RemoteDeploymentContract.SBK_VERSION_PROPERTY + "=" + sbkVersion + "\n"
                + RemoteDeploymentContract.JAVA_VERSION_PROPERTY + "=" + javaVersion + "\n"
                + RemoteDeploymentContract.PLATFORM_OS_PROPERTY + "=" + platform.operatingSystem() + "\n"
                + RemoteDeploymentContract.CONTENT_SHA_256_PROPERTY + "=" + contentDigest + "\n"
                + RemoteDeploymentContract.INCLUDES_JAVA_PROPERTY + "=false\n";
    }

    private static String checksums(List<Entry> entries) {
        final StringBuilder value = new StringBuilder();
        for (Entry entry : entries) {
            if (entry.type() == Type.REGULAR_FILE) {
                value.append(entry.digest()).append("  ").append(entry.relativePath()).append('\n');
            }
        }
        return value.toString();
    }

    private static void moveAtomically(Path source, Path target) throws IOException {
        try {
            Files.move(source, target, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
        } catch (AtomicMoveNotSupportedException ignored) {
            Files.move(source, target, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    private static String fileName(Path path) {
        return Objects.requireNonNull(path.getFileName(), "Archive must have a file name").toString();
    }

    enum Type {
        DIRECTORY,
        REGULAR_FILE,
        SYMBOLIC_LINK
    }

    record Entry(Path source, String relativePath, Type type, long size, String digest,
                 String linkTarget, int mode) {
    }
}
