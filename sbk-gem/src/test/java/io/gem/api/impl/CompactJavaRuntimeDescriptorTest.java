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
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/** Tests the independent Gradle-to-SBK-GEM compact Java runtime contract. */
final class CompactJavaRuntimeDescriptorTest {
    @TempDir
    private Path temporaryDirectory;

    @Test
    void loadsValidatedCompactRuntimeContract() throws IOException {
        writeDescriptor("java.base,jdk.unsupported", "--strip-debug,--compress=zip-6");

        final CompactJavaRuntimeDescriptor descriptor = CompactJavaRuntimeDescriptor.load(temporaryDirectory, 25);

        assertEquals(25, descriptor.javaMajor());
        assertEquals(List.of("java.base", "jdk.unsupported"), descriptor.modules());
        assertEquals(List.of("--strip-debug", "--compress=zip-6"), descriptor.options());
    }

    @Test
    void rejectsUnsupportedJlinkOption() throws IOException {
        writeDescriptor("java.base", "--output=/tmp/untrusted");

        assertThrows(IOException.class, () -> CompactJavaRuntimeDescriptor.load(temporaryDirectory, 25));
    }

    @Test
    void rejectsDifferentJavaMajor() throws IOException {
        writeDescriptor("java.base", "--strip-debug");

        assertThrows(IOException.class, () -> CompactJavaRuntimeDescriptor.load(temporaryDirectory, 24));
    }

    private void writeDescriptor(String modules, String options) throws IOException {
        final Path directory = Files.createDirectories(
                temporaryDirectory.resolve(DriverRuntimeManifest.DIRECTORY));
        Files.writeString(directory.resolve(CompactJavaRuntimeDescriptor.FILE_NAME),
                "format.version=1\n"
                        + "java.major=25\n"
                        + "java.modules=" + modules + "\n"
                        + "jlink.options=" + options + "\n",
                StandardCharsets.UTF_8);
    }
}
