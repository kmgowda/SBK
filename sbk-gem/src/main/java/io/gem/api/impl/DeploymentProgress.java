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

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/** Formats progress for concurrent remote deployment operations. */
final class DeploymentProgress {
    private DeploymentProgress() {
    }

    static String pendingHosts(CompletableFuture<?>[] futures, String[] targetHosts) {
        final List<String> pendingHosts = new ArrayList<>();
        for (int i = 0; i < targetHosts.length; i++) {
            if (targetHosts[i] != null && !futures[i].isDone()) {
                pendingHosts.add(targetHosts[i]);
            }
        }
        return pendingHosts.isEmpty() ? "finalizing" : "waiting for " + String.join(", ", pendingHosts);
    }
}
