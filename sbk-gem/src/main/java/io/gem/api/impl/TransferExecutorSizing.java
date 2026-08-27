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

/** Selects bounded deployment-transfer concurrency from module configuration. */
final class TransferExecutorSizing {
    private TransferExecutorSizing() {
    }

    static int initialThreads(GemConfig config) {
        return config.transferExecutorThreads == 0
                ? config.transferExecutorMinimumThreads : config.transferExecutorThreads;
    }

    static int selectedThreads(GemConfig config, int uniqueTargets) {
        if (config.transferExecutorThreads > 0) {
            return config.transferExecutorThreads;
        }
        if (uniqueTargets < 1) {
            throw new IllegalArgumentException("At least one remote deployment target is required");
        }
        final int targetThreads = Math.ceilDiv(uniqueTargets, config.transferTargetWaves);
        return Math.clamp(targetThreads, config.transferExecutorMinimumThreads,
                config.transferExecutorMaximumThreads);
    }
}
