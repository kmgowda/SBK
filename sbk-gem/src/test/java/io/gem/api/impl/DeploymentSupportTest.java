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

import io.gem.config.GemConfig;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Path;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Tests shared deployment timeout, selection, and cache-path helpers. */
final class DeploymentSupportTest {
    @TempDir
    private Path temporaryDirectory;

    @Test
    void resolvesAbsoluteRuntimeCacheAndTargetSelection() {
        final GemConfig config = new GemConfig();
        config.runtimeCacheDirectory = temporaryDirectory.toString();

        assertEquals(temporaryDirectory, DeploymentSupport.runtimeCacheDirectory(config));
        assertTrue(DeploymentSupport.hasSelectedTarget(new boolean[]{false, true}));
        assertFalse(DeploymentSupport.hasSelectedTarget(new boolean[]{false, false}));
    }

    @Test
    void reportsBoundedWaitTimeout() {
        final IOException failure = assertThrows(IOException.class,
                () -> DeploymentSupport.waitFor(new CompletableFuture<>(), 0, "test operation"));

        assertTrue(failure.getMessage().contains("test operation timed out"));
    }

    @Test
    void preservesCompletedFutureFailure() {
        final CompletableFuture<Void> failed = CompletableFuture.failedFuture(
                new IllegalStateException("failed"));

        assertThrows(ExecutionException.class, () -> DeploymentSupport.waitFor(failed, 1, "test"));
    }
}
