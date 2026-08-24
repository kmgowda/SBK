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

import io.state.State;

import java.util.concurrent.CancellationException;

/** Short-held SBK-GEM lifecycle state machine, separate from blocking orchestration work. */
final class BenchmarkLifecycle {
    private State state = State.BEGIN;
    private boolean sbmStarted;

    /**
     * Enter the running state once.
     *
     * @return true when this invocation started the lifecycle
     */
    synchronized boolean begin() {
        if (state != State.BEGIN) {
            return false;
        }
        state = State.RUN;
        return true;
    }

    /**
     * Return the current state.
     *
     * @return lifecycle state
     */
    synchronized State state() {
        return state;
    }

    /**
     * Fail when blocking orchestration should no longer continue.
     *
     * @param operation current orchestration phase
     * @throws CancellationException when shutdown has started
     */
    synchronized void requireRunning(String operation) {
        if (state != State.RUN) {
            throw new CancellationException("SBK-GEM stopped during " + operation);
        }
    }

    /**
     * Mark the embedded SBM started if the benchmark is still running.
     *
     * @throws CancellationException when shutdown won the startup race
     */
    synchronized void markSbmStarted() {
        requireRunning("embedded SBM startup");
        sbmStarted = true;
    }

    /**
     * Enter the terminal state once.
     *
     * @return true when this invocation owns shutdown cleanup
     */
    synchronized boolean beginShutdown() {
        if (state == State.END) {
            return false;
        }
        state = State.END;
        return true;
    }

    /**
     * Atomically claim embedded-SBM shutdown.
     *
     * @return whether the embedded SBM had started
     */
    synchronized boolean takeSbmStarted() {
        final boolean started = sbmStarted;
        sbmStarted = false;
        return started;
    }
}
