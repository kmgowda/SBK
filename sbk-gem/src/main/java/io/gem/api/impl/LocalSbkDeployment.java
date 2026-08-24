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

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.jar.Attributes;
import java.util.jar.JarFile;
import java.util.Objects;
import java.util.regex.Pattern;
import java.util.stream.Stream;

/**
 * Discovers and validates the SBK version in the local distribution selected for deployment.
 */
final class LocalSbkDeployment {
    private static final Pattern MAIN_JAR = Pattern.compile("sbk-[0-9].*[.]jar");
    private LocalSbkDeployment() {
    }

    /**
     * Read the selected local SBK JAR manifest without invoking a launcher script.
     *
     * @param distribution local SBK distribution
     * @return version reported by the main JAR manifest
     * @throws IOException when the distribution or manifest is invalid
     */
    static String discoverVersion(Path distribution) throws IOException {
        final Path lib = distribution.resolve("lib");
        final java.util.List<Path> candidates;
        try (Stream<Path> entries = Files.list(lib)) {
            candidates = entries.filter(path -> MAIN_JAR.matcher(
                    Objects.requireNonNull(path.getFileName()).toString()).matches()).toList();
        }
        if (candidates.size() != 1) {
            throw new IOException("Expected one main SBK JAR under " + lib + ", found " + candidates.size());
        }
        try (JarFile jar = new JarFile(candidates.getFirst().toFile())) {
            final String version = jar.getManifest().getMainAttributes()
                    .getValue(Attributes.Name.IMPLEMENTATION_VERSION);
            if (version == null || version.isBlank()) {
                throw new IOException("SBK main JAR has no Implementation-Version: " + candidates.getFirst());
            }
            return version;
        }
    }
}
