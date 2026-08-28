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

import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.PosixFilePermission;
import java.util.Comparator;
import java.util.Objects;
import java.util.stream.Stream;

/** Writes a validated Java home as the managed, executable-preserving tar archive. */
final class ManagedJavaArchive {
    private static final int BUFFER_SIZE = 4 * 1024 * 1024;
    private static final int REGULAR_FILE_MODE = 0644;
    private static final int EXECUTABLE_FILE_MODE = 0755;
    private static final int SYMBOLIC_LINK_MODE = 0777;

    private ManagedJavaArchive() {
    }

    static void create(Path javaHome, Path target) throws IOException {
        final Path temporary = Files.createTempFile(Objects.requireNonNull(target.getParent()),
                Objects.requireNonNull(target.getFileName()).toString(), ".partial");
        try {
            try (OutputStream file = Files.newOutputStream(temporary);
                 BufferedOutputStream buffered = new BufferedOutputStream(file, BUFFER_SIZE);
                 TarArchiveOutputStream output = new TarArchiveOutputStream(buffered)) {
                output.setLongFileMode(TarArchiveOutputStream.LONGFILE_POSIX);
                output.setBigNumberMode(TarArchiveOutputStream.BIGNUMBER_POSIX);
                try (Stream<Path> paths = Files.walk(javaHome)) {
                    for (Path source : paths.filter(path -> !path.equals(javaHome))
                            .sorted(Comparator.comparing(Path::toString)).toList()) {
                        addEntry(javaHome, source, output);
                    }
                }
                output.finish();
            }
            moveAtomically(temporary, target);
        } finally {
            Files.deleteIfExists(temporary);
        }
    }

    private static void addEntry(Path javaHome, Path source, TarArchiveOutputStream output) throws IOException {
        final String relative = javaHome.relativize(source).toString().replace('\\', '/');
        final TarArchiveEntry entry;
        if (Files.isSymbolicLink(source)) {
            final Path link = Files.readSymbolicLink(source);
            final Path resolved = Objects.requireNonNull(source.getParent()).resolve(link).normalize();
            if (link.isAbsolute() || !resolved.startsWith(javaHome)) {
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

    private static void moveAtomically(Path source, Path target) throws IOException {
        try {
            Files.move(source, target, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
        } catch (AtomicMoveNotSupportedException ignored) {
            Files.move(source, target, StandardCopyOption.REPLACE_EXISTING);
        }
    }
}
