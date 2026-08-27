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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/** Verifies fixed and automatic deployment-transfer concurrency selection. */
final class TransferExecutorSizingTest {
    @Test
    void selectsAutomaticConcurrencyWithinConfiguredBounds() {
        final GemConfig config = automaticConfig();

        assertEquals(4, TransferExecutorSizing.initialThreads(config));
        assertEquals(4, TransferExecutorSizing.selectedThreads(config, 1));
        assertEquals(4, TransferExecutorSizing.selectedThreads(config, 32));
        assertEquals(8, TransferExecutorSizing.selectedThreads(config, 64));
        assertEquals(13, TransferExecutorSizing.selectedThreads(config, 100));
        assertEquals(32, TransferExecutorSizing.selectedThreads(config, 256));
        assertEquals(64, TransferExecutorSizing.selectedThreads(config, 512));
        assertEquals(64, TransferExecutorSizing.selectedThreads(config, 10_000));
    }

    @Test
    void preservesAConfiguredFixedOverride() {
        final GemConfig config = automaticConfig();
        config.transferExecutorThreads = 12;

        assertEquals(12, TransferExecutorSizing.initialThreads(config));
        assertEquals(12, TransferExecutorSizing.selectedThreads(config, 100));
    }

    @Test
    void honorsCustomAutomaticBoundsAndWaveTarget() {
        final GemConfig config = automaticConfig();
        config.transferExecutorMinimumThreads = 2;
        config.transferExecutorMaximumThreads = 10;
        config.transferTargetWaves = 4;

        assertEquals(2, TransferExecutorSizing.selectedThreads(config, 1));
        assertEquals(5, TransferExecutorSizing.selectedThreads(config, 20));
        assertEquals(10, TransferExecutorSizing.selectedThreads(config, 100));
    }

    @Test
    void rejectsAnEmptyAutomaticTargetInventory() {
        assertThrows(IllegalArgumentException.class,
                () -> TransferExecutorSizing.selectedThreads(automaticConfig(), 0));
    }

    private static GemConfig automaticConfig() {
        final GemConfig config = new GemConfig();
        config.transferExecutorThreads = 0;
        config.transferExecutorMinimumThreads = 4;
        config.transferExecutorMaximumThreads = 64;
        config.transferTargetWaves = 8;
        return config;
    }
}
