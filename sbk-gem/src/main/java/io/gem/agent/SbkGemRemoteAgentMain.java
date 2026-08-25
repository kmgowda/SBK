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

import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;

import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.PosixFilePermission;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.HexFormat;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.jar.JarFile;
import java.util.stream.Stream;

/** Java-only remote deployment and execution agent for SBK-GEM. */
public final class SbkGemRemoteAgentMain {
    private static final String MARKER = ".sbk-runtime.sha256";
    private static final String DESCRIPTOR = "deployment.properties";
    private static final String CHECKSUMS = "deployment-files.sha256";
    private static final int BUFFER_SIZE = 64 * 1024;

    private SbkGemRemoteAgentMain() {
    }

    /**
     * Execute one request supplied on standard input.
     * @param args unused
     */
    public static void main(String[] args) {
        try {
            final RemoteAgentProtocol.Request request = RemoteAgentProtocol.read(new DataInputStream(System.in));
            switch (request.operation()) {
                case "probe" -> probe(request.values());
                case "activate" -> activate(request.values());
                case "verify" -> verify(request.values());
                case "run" -> System.exit(run(request.values()));
                default -> throw new IOException("Unknown operation: " + request.operation());
            }
        } catch (Throwable failure) {
            System.err.println("SBK-GEM remote agent failed: " + failure.getClass().getSimpleName() + ": "
                    + failure.getMessage());
            System.exit(70);
        }
    }

    private static void probe(List<String> values) throws IOException {
        requireCount(values, 1);
        final int expected = Integer.parseInt(values.getFirst());
        final int actual = Runtime.version().feature();
        if (!isJavaCompatible(actual, expected)) {
            throw new IOException("Java major is too old: required " + expected + " or newer, found " + actual);
        }
        final Path home = Path.of(System.getProperty("java.home")).toAbsolutePath().normalize();
        if (!Files.isExecutable(home.resolve("bin/javac"))) {
            throw new IOException("JDK compiler is missing under " + home);
        }
        System.out.println("SBK_OS=" + operatingSystem());
        System.out.println("SBK_JAVA_HOME=" + home);
        System.out.println("SBK_JAVA_MAJOR=" + actual);
    }

    static boolean isJavaCompatible(int actual, int minimum) {
        return actual >= minimum;
    }

    private static void activate(List<String> values) throws IOException {
        requireCount(values, 6);
        final Path archive = absolute(values.get(0));
        final String archiveDigest = values.get(1);
        final String contentDigest = values.get(2);
        final Path staging = absolute(values.get(3));
        final Path destination = absolute(values.get(4));
        try {
            if (!archiveDigest.equals(sha256(archive))) {
                throw new IOException(RemoteAgentProtocol.ARCHIVE_DIGEST_MISMATCH);
            }
            deleteRecursively(staging);
            Files.createDirectories(staging);
            extract(archive, staging);
            final Path extracted = staging.resolve("runtime");
            final Properties descriptor = loadProperties(extracted.resolve(DESCRIPTOR));
            requireProperty(descriptor, "content.sha256", contentDigest);
            requireProperty(descriptor, "platform.os", values.get(5));
            verifyChecksums(extracted);
            final Path marker = destination.resolve(MARKER);
            if (Files.exists(destination, LinkOption.NOFOLLOW_LINKS)) {
                if (Files.isRegularFile(marker) && contentDigest.equals(Files.readString(marker).trim())) {
                    return;
                }
                deleteRecursively(destination);
            }
            move(extracted, destination);
            Files.writeString(destination.resolve(MARKER), contentDigest + System.lineSeparator(),
                    StandardCharsets.UTF_8);
        } finally {
            Files.deleteIfExists(archive);
            deleteRecursively(staging);
        }
    }

    private static void verify(List<String> values) throws IOException {
        requireCount(values, 4);
        final Path runtime = absolute(values.get(0));
        if (!values.get(1).equals(Files.readString(runtime.resolve(MARKER)).trim())) {
            throw new IOException("SBK runtime content digest mismatch");
        }
        final Properties descriptor = loadProperties(runtime.resolve(DESCRIPTOR));
        requireProperty(descriptor, "content.sha256", values.get(1));
        requireProperty(descriptor, "sbk.version", values.get(2));
        requireProperty(descriptor, "platform.os", values.get(3));
        runtimeJars(runtime.resolve("sbk"), values.get(2));
        System.out.println("SBK_RUNTIME_CONTENT=" + values.get(1));
        System.out.println("SBK_VERSION=" + values.get(2));
    }

    private static int run(List<String> values) throws IOException, InterruptedException {
        if (values.size() < 4) {
            throw new IOException("Invalid run request");
        }
        final Path runtime = absolute(values.get(0));
        final int jvmCount = Integer.parseInt(values.get(2));
        if (jvmCount < 0 || 3 + jvmCount > values.size()) {
            throw new IOException("Invalid JVM argument count");
        }
        final Path sbk = runtime.resolve("sbk");
        final List<Path> jars = runtimeJars(sbk, values.get(1));
        final List<String> command = new ArrayList<>();
        command.add(Path.of(System.getProperty("java.home"), "bin/java").toString());
        command.addAll(values.subList(3, 3 + jvmCount));
        command.add("-Dsbk.applicationName=sbk");
        command.add("-Dsbk.appHome=" + sbk);
        command.add("-Dsbk.jmxExport=false");
        command.add("-classpath");
        command.add(jars.get(0) + System.getProperty("path.separator") + jars.get(1));
        command.add("io.sbk.main.SbkMain");
        command.addAll(values.subList(3 + jvmCount, values.size()));
        final Process process = new ProcessBuilder(command).directory(Objects.requireNonNull(runtime.getParent(),
                        "SBK runtime must have a parent directory").toFile())
                .inheritIO().start();
        final Thread cleanup = new Thread(() -> stopProcessTree(process), "sbk-gem-agent-cleanup");
        Runtime.getRuntime().addShutdownHook(cleanup);
        try {
            return process.waitFor();
        } finally {
            try {
                Runtime.getRuntime().removeShutdownHook(cleanup);
            } catch (IllegalStateException ignored) {
                // JVM shutdown already owns the hook.
            }
        }
    }

    private static List<Path> runtimeJars(Path sbk, String version) throws IOException {
        final Path lib = sbk.resolve("lib");
        final List<Path> pathing;
        try (Stream<Path> files = Files.list(lib)) {
            pathing = files.filter(path -> Objects.requireNonNull(path.getFileName()).toString()
                            .startsWith("sbk-pathing-"))
                    .filter(path -> Objects.requireNonNull(path.getFileName()).toString().endsWith(".jar"))
                    .toList();
        }
        final Path main = lib.resolve("sbk-" + version + ".jar");
        if (pathing.size() != 1 || !Files.isRegularFile(main)) {
            throw new IOException("Incomplete SBK runtime under " + sbk);
        }
        try (JarFile jar = new JarFile(pathing.getFirst().toFile())) {
            if (jar.getManifest() == null) {
                throw new IOException("SBK pathing JAR has no manifest");
            }
        }
        return List.of(pathing.getFirst(), main);
    }

    private static void extract(Path archive, Path staging) throws IOException {
        try (InputStream file = new BufferedInputStream(Files.newInputStream(archive));
             TarArchiveInputStream tar = new TarArchiveInputStream(file)) {
            TarArchiveEntry entry;
            while ((entry = tar.getNextEntry()) != null) {
                final Path target = staging.resolve(entry.getName()).normalize();
                if (!target.startsWith(staging)) {
                    throw new IOException("Archive entry escapes staging: " + entry.getName());
                }
                if (entry.isDirectory()) {
                    Files.createDirectories(target);
                } else if (entry.isSymbolicLink()) {
                    final Path parent = Objects.requireNonNull(target.getParent(), "Archive entry has no parent");
                    Files.createDirectories(parent);
                    final Path link = Path.of(entry.getLinkName());
                    if (link.isAbsolute() || !parent.resolve(link).normalize().startsWith(staging)) {
                        throw new IOException("Unsafe archive symbolic link: " + entry.getName());
                    }
                    Files.createSymbolicLink(target, link);
                } else if (entry.isFile()) {
                    Files.createDirectories(Objects.requireNonNull(target.getParent(),
                            "Archive entry has no parent"));
                    Files.copy(tar, target, StandardCopyOption.REPLACE_EXISTING);
                } else {
                    throw new IOException("Unsupported archive entry: " + entry.getName());
                }
                if (!entry.isSymbolicLink()) {
                    setPermissions(target, entry.getMode());
                }
            }
        }
    }

    private static void verifyChecksums(Path root) throws IOException {
        for (String line : Files.readAllLines(root.resolve(CHECKSUMS), StandardCharsets.UTF_8)) {
            if (line.isBlank()) {
                continue;
            }
            final int separator = line.indexOf("  ");
            if (separator <= 0) {
                throw new IOException("Invalid SBK checksum manifest entry");
            }
            final Path file = root.resolve(line.substring(separator + 2)).normalize();
            if (!file.startsWith(root) || !line.substring(0, separator).equals(sha256(file))) {
                throw new IOException("SBK checksum verification failed for " + file);
            }
        }
    }

    private static void setPermissions(Path path, int mode) throws IOException {
        try {
            final Set<PosixFilePermission> permissions = EnumSet.noneOf(PosixFilePermission.class);
            if ((mode & 0400) != 0) {
                permissions.add(PosixFilePermission.OWNER_READ);
            }
            if ((mode & 0200) != 0) {
                permissions.add(PosixFilePermission.OWNER_WRITE);
            }
            if ((mode & 0100) != 0) {
                permissions.add(PosixFilePermission.OWNER_EXECUTE);
            }
            if ((mode & 0040) != 0) {
                permissions.add(PosixFilePermission.GROUP_READ);
            }
            if ((mode & 0010) != 0) {
                permissions.add(PosixFilePermission.GROUP_EXECUTE);
            }
            if ((mode & 0004) != 0) {
                permissions.add(PosixFilePermission.OTHERS_READ);
            }
            if ((mode & 0001) != 0) {
                permissions.add(PosixFilePermission.OTHERS_EXECUTE);
            }
            Files.setPosixFilePermissions(path, permissions);
        } catch (UnsupportedOperationException ignored) {
            // SFTP and POSIX filesystems preserve mode; tolerate providers without POSIX attributes.
        }
    }

    private static String operatingSystem() throws IOException {
        final String value = System.getProperty("os.name", "").toLowerCase(Locale.ROOT);
        if (value.equals("linux")) {
            return "linux";
        }
        if (value.startsWith("mac")) {
            return "macos";
        }
        throw new IOException("Unsupported operating system: " + value);
    }

    private static Properties loadProperties(Path path) throws IOException {
        final Properties properties = new Properties();
        try (InputStream input = Files.newInputStream(path)) {
            properties.load(input);
        }
        return properties;
    }

    private static void requireProperty(Properties properties, String key, String expected) throws IOException {
        if (!expected.equals(properties.getProperty(key))) {
            throw new IOException("SBK runtime descriptor mismatch for " + key);
        }
    }

    private static String sha256(Path path) throws IOException {
        final MessageDigest digest;
        try {
            digest = MessageDigest.getInstance("SHA-256");
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

    private static Path absolute(String value) throws IOException {
        final Path path = Path.of(value).normalize();
        if (!path.isAbsolute()) {
            throw new IOException("Agent path must be absolute: " + value);
        }
        return path;
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

    static void stopProcessTree(Process process) {
        final List<ProcessHandle> descendants = process.toHandle().descendants().toList();
        descendants.forEach(ProcessHandle::destroy);
        process.destroy();
        try {
            process.waitFor(5, TimeUnit.SECONDS);
            descendants.stream().filter(ProcessHandle::isAlive).forEach(ProcessHandle::destroyForcibly);
            process.toHandle().descendants().filter(ProcessHandle::isAlive)
                    .forEach(ProcessHandle::destroyForcibly);
            if (process.isAlive()) {
                process.destroyForcibly();
            }
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            descendants.stream().filter(ProcessHandle::isAlive).forEach(ProcessHandle::destroyForcibly);
            process.destroyForcibly();
        }
    }

    private static void requireCount(List<String> values, int count) throws IOException {
        if (values.size() != count) {
            throw new IOException("Invalid request value count");
        }
    }
}
