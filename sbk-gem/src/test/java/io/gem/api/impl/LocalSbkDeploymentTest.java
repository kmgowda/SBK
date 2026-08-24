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

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.jar.Attributes;
import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/** Tests script-free local SBK version discovery. */
final class LocalSbkDeploymentTest {
    @TempDir
    private Path temporaryDirectory;

    @Test
    void discoversVersionFromMainJarManifest() throws IOException {
        createJar("10.7");

        assertEquals("10.7", LocalSbkDeployment.discoverVersion(temporaryDirectory));
    }

    @Test
    void rejectsMissingManifestVersion() throws IOException {
        createJar(null);

        assertThrows(IOException.class, () -> LocalSbkDeployment.discoverVersion(temporaryDirectory));
    }

    private void createJar(String version) throws IOException {
        final Path lib = Files.createDirectories(temporaryDirectory.resolve("lib"));
        final Manifest manifest = new Manifest();
        manifest.getMainAttributes().put(Attributes.Name.MANIFEST_VERSION, "1.0");
        if (version != null) {
            manifest.getMainAttributes().put(Attributes.Name.IMPLEMENTATION_VERSION, version);
        }
        try (JarOutputStream output = new JarOutputStream(Files.newOutputStream(lib.resolve("sbk-10.7.jar")),
                manifest)) {
            output.finish();
        }
    }
}
