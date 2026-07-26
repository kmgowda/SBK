/**
 * Copyright (c) KMG. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 */
package io.sbk.driver.AsyncFile;

import java.io.IOException;
import java.util.concurrent.Phaser;
import java.util.concurrent.locks.LockSupport;

/**
 * Tracks asynchronous file operations without adding an object per request.
 *
 * <p>A {@link Phaser} represents pending operation count without allocating a
 * queue node, future, or tracking wrapper for every request. Its permanent
 * coordinator party prevents termination; each submitted operation registers
 * one party and its callback deregisters that party.
 */
final class AsyncFileOperations {
    private static final long WAIT_NANOS = 100_000;
    private final Phaser pending = new Phaser(1);
    private volatile Throwable failure;

    void submitted() {
        pending.register();
    }

    void completed() {
        pending.arriveAndDeregister();
    }

    void failed(Throwable throwable) {
        failure = throwable;
        pending.arriveAndDeregister();
    }

    void awaitCompletion() throws IOException {
        while (pending.getRegisteredParties() > 1) {
            if (Thread.currentThread().isInterrupted()) {
                Thread.currentThread().interrupt();
                throw new IOException("Interrupted while awaiting asynchronous file operations");
            }
            LockSupport.parkNanos(WAIT_NANOS);
        }
        if (failure != null) {
            throw new IOException("Asynchronous file operation failed", failure);
        }
    }
}
