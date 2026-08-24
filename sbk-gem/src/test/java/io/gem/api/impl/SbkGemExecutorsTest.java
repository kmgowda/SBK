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

import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Verifies that SBK-GEM orchestration workloads use bounded or lightweight execution resources. */
final class SbkGemExecutorsTest {

    @Test
    void boundsPlatformThreadsAndUsesVirtualThreadsForRemoteCommands() throws Exception {
        try (SbkGemExecutors executors = SbkGemExecutors.create(3, 2)) {
            final ThreadPoolExecutor control = (ThreadPoolExecutor) executors.control();
            final ThreadPoolExecutor transfer = (ThreadPoolExecutor) executors.transfer();

            assertEquals(3, control.getMaximumPoolSize());
            assertEquals(2, transfer.getMaximumPoolSize());
            assertTrue(executors.control().submit(() -> Thread.currentThread().getName()
                    .startsWith("sbk-gem-control-")).get(5, TimeUnit.SECONDS));
            assertTrue(executors.transfer().submit(() -> Thread.currentThread().getName()
                    .startsWith("sbk-gem-transfer-")).get(5, TimeUnit.SECONDS));
            assertTrue(executors.command().submit(() -> Thread.currentThread().isVirtual())
                    .get(5, TimeUnit.SECONDS));
        }
    }
}
