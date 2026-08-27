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

import io.sbk.system.Printer;

import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

/** Emits bounded lifecycle progress without logging every file or network buffer. */
final class LifecycleProgress implements AutoCloseable {
    private final String operation;
    private final Supplier<String> detail;
    private final long startedNanos;
    private final ScheduledFuture<?> progressTask;

    LifecycleProgress(String operation, int intervalSeconds, ScheduledExecutorService scheduler,
                      Supplier<String> detail) {
        this.operation = operation;
        this.detail = detail;
        this.startedNanos = System.nanoTime();
        this.progressTask = scheduler.scheduleWithFixedDelay(this::logProgress, intervalSeconds,
                intervalSeconds, TimeUnit.SECONDS);
    }

    private void logProgress() {
        Printer.log.info("SBK-GEM: {} is still running; elapsed {} second(s); {}",
                operation, elapsedSeconds(), detail.get());
    }

    long elapsedSeconds() {
        return TimeUnit.NANOSECONDS.toSeconds(System.nanoTime() - startedNanos);
    }

    long elapsedMillis() {
        return TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startedNanos);
    }

    @Override
    public void close() {
        progressTask.cancel(false);
    }
}
