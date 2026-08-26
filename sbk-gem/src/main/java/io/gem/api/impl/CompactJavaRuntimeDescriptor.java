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
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.HexFormat;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import java.util.regex.Pattern;

/** Validated Gradle build contract for a compact Java runtime image. */
final class CompactJavaRuntimeDescriptor {
    static final String FILE_NAME = "java-runtime.properties";
    private static final String FORMAT_VERSION = "1";
    private static final Pattern MODULE_NAME = Pattern.compile("[A-Za-z][A-Za-z0-9_.]*");
    private static final Set<String> SUPPORTED_JLINK_OPTIONS = Set.of(
            "--strip-debug", "--no-header-files", "--no-man-pages", "--compress=zip-6");

    private final int javaMajor;
    private final List<String> modules;
    private final List<String> options;
    private final String identity;

    private CompactJavaRuntimeDescriptor(int javaMajor, List<String> modules, List<String> options,
                                         String identity) {
        this.javaMajor = javaMajor;
        this.modules = List.copyOf(modules);
        this.options = List.copyOf(options);
        this.identity = identity;
    }

    /**
     * Load the independently generated compact-Java descriptor from an installed distribution.
     *
     * @param sbkDirectory complete installed SBK distribution
     * @param expectedJavaMajor controller Java major version
     * @return validated compact runtime descriptor
     * @throws IOException when metadata is absent, malformed, or incompatible
     */
    static CompactJavaRuntimeDescriptor load(Path sbkDirectory, int expectedJavaMajor) throws IOException {
        final Path descriptor = sbkDirectory.resolve(DriverRuntimeManifest.DIRECTORY).resolve(FILE_NAME);
        final Properties properties = new Properties();
        if (!Files.isRegularFile(descriptor)) {
            throw new IOException("Compact Java runtime metadata is missing: " + descriptor
                    + ". Rebuild the distribution with './gradlew installDist'.");
        }
        try (InputStream input = Files.newInputStream(descriptor)) {
            properties.load(input);
        }
        if (!FORMAT_VERSION.equals(properties.getProperty(RemoteDeploymentContract.FORMAT_VERSION_PROPERTY))) {
            throw new IOException("Unsupported compact Java runtime descriptor format: " + descriptor);
        }
        final int javaMajor;
        try {
            javaMajor = Integer.parseInt(required(properties, "java.major", descriptor));
        } catch (NumberFormatException exception) {
            throw new IOException("Invalid java.major in " + descriptor, exception);
        }
        if (javaMajor != expectedJavaMajor) {
            throw new IOException("Compact Java runtime descriptor requires Java " + javaMajor
                    + " but the controller requires Java " + expectedJavaMajor + ": " + descriptor);
        }
        final List<String> modules = values(properties, "java.modules", descriptor).stream().sorted().toList();
        if (modules.stream().anyMatch(module -> !MODULE_NAME.matcher(module).matches())
                || modules.stream().distinct().count() != modules.size() || !modules.contains("java.base")) {
            throw new IOException("Invalid Java module list in " + descriptor);
        }
        final List<String> options = values(properties, "jlink.options", descriptor);
        if (options.stream().distinct().count() != options.size()
                || !SUPPORTED_JLINK_OPTIONS.containsAll(options)) {
            throw new IOException("Unsupported jlink option in " + descriptor);
        }
        final String canonical = FORMAT_VERSION + "\0" + javaMajor + "\0" + String.join(",", modules)
                + "\0" + String.join(",", options);
        return new CompactJavaRuntimeDescriptor(javaMajor, modules, options, sha256(canonical));
    }

    int javaMajor() {
        return javaMajor;
    }

    List<String> modules() {
        return modules;
    }

    List<String> options() {
        return options;
    }

    String identity() {
        return identity;
    }

    private static List<String> values(Properties properties, String name, Path descriptor) throws IOException {
        final List<String> values = Arrays.stream(required(properties, name, descriptor).split(","))
                .map(String::trim).filter(value -> !value.isEmpty()).toList();
        if (values.isEmpty()) {
            throw new IOException("Empty " + name + " in " + descriptor);
        }
        return values;
    }

    private static String required(Properties properties, String name, Path descriptor) throws IOException {
        final String value = properties.getProperty(name, "").trim();
        if (value.isEmpty()) {
            throw new IOException("Missing " + name + " in " + descriptor);
        }
        return value;
    }

    private static String sha256(String value) {
        try {
            final MessageDigest digest = MessageDigest.getInstance(RemoteDeploymentContract.SHA_256);
            return HexFormat.of().formatHex(digest.digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException(exception);
        }
    }
}
