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

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Execution resources separated by SBK-GEM orchestration workload.
 *
 * <p>Connection and control work has bounded platform-thread concurrency, large SFTP transfers use
 * a smaller independent platform-thread pool, and commands that remain active for a complete
 * benchmark use lightweight virtual threads. A slow transfer therefore cannot starve control work,
 * and a large node inventory does not create one platform thread per remote benchmark.</p>
 *
 * @param control bounded connection and control-operation executor
 * @param transfer bounded SFTP transfer executor
 * @param command virtual-thread-per-task remote command executor
 */
record SbkGemExecutors(ExecutorService control, ExecutorService transfer, ExecutorService command)
        implements AutoCloseable {

    static SbkGemExecutors create(int controlThreads, int transferThreads) {
        return new SbkGemExecutors(
                Executors.newFixedThreadPool(controlThreads,
                        Thread.ofPlatform().name("sbk-gem-control-", 0).factory()),
                Executors.newFixedThreadPool(transferThreads,
                        Thread.ofPlatform().name("sbk-gem-transfer-", 0).factory()),
                Executors.newThreadPerTaskExecutor(
                        Thread.ofVirtual().name("sbk-gem-command-", 0).factory()));
    }

    @Override
    public void close() {
        control.shutdownNow();
        transfer.shutdownNow();
        command.shutdownNow();
    }
}
