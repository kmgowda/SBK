/**
 * Copyright (c) KMG. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.sbk.driver.MinIO;

import java.io.IOException;
import java.io.InterruptedIOException;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BiConsumer;

/**
 * Per-worker bounded async-operation tracker.
 *
 * <p>The semaphore applies backpressure before an operation starts, so the
 * measured latency does not include time waiting for a local concurrency slot.
 * Pending futures are retained until completion so {@link #await()} can provide
 * deterministic shutdown.
 */
public final class S3AsyncExecutor {
    private final Semaphore permits;
    private final Semaphore globalPermits;
    private final Set<CompletableFuture<?>> pending;
    private final AtomicReference<Throwable> failure;

    /**
     * Create a bounded async tracker.
     *
     * @param depth maximum operations in flight for one worker
     * @throws IllegalArgumentException when depth is less than one
     */
    public S3AsyncExecutor(int depth) {
        this(depth, null);
    }

    /**
     * Create a worker-local tracker that also observes a process-wide limit.
     *
     * @param depth maximum operations in flight for one worker
     * @param globalPermits shared process-wide permits, or {@code null}
     * @throws IllegalArgumentException when depth is less than one
     */
    public S3AsyncExecutor(int depth, Semaphore globalPermits) {
        if (depth < 1) {
            throw new IllegalArgumentException("async-depth must be at least 1");
        }
        permits = new Semaphore(depth);
        this.globalPermits = globalPermits;
        pending = ConcurrentHashMap.newKeySet();
        failure = new AtomicReference<>();
    }

    /**
     * Acquire an async slot before starting latency measurement.
     *
     * @throws IOException when interrupted or a previous async operation failed
     */
    public void acquire() throws IOException {
        throwIfFailed();
        boolean localAcquired = false;
        try {
            permits.acquire();
            localAcquired = true;
            if (globalPermits != null) {
                globalPermits.acquire();
            }
        } catch (InterruptedException ex) {
            if (localAcquired) {
                permits.release();
            }
            Thread.currentThread().interrupt();
            throw new IOException("Interrupted while waiting for an async S3 concurrency slot", ex);
        }
        try {
            throwIfFailed();
        } catch (IOException ex) {
            permits.release();
            if (globalPermits != null) {
                globalPermits.release();
            }
            throw ex;
        }
    }

    /**
     * Track a future after the caller has acquired a slot.
     *
     * @param future MinIO SDK operation future
     * @param <T> operation result type
     * @return the same future
     */
    public <T> CompletableFuture<T> track(CompletableFuture<T> future) {
        pending.add(future);
        future.whenComplete((ignored, thrown) -> {
            pending.remove(future);
            if (thrown != null && !isCleanShutdown(thrown)) {
                failure.compareAndSet(null, unwrap(thrown));
            }
            permits.release();
            if (globalPermits != null) {
                globalPermits.release();
            }
        });
        return future;
    }

    /**
     * Attach a completion action and track the complete callback chain.
     *
     * <p>The returned future does not complete, and the async slot is not released,
     * until {@code completion} returns. This guarantees that {@link #await()} cannot
     * finish before a successful operation has published its performance measurement.
     *
     * @param future MinIO SDK operation future
     * @param completion action that publishes the operation result
     * @param <T> operation result type
     * @return future representing both the SDK operation and completion action
     */
    public <T> CompletableFuture<T> track(CompletableFuture<T> future,
                                          BiConsumer<? super T, ? super Throwable> completion) {
        return track(future.whenComplete(completion));
    }

    /**
     * Release a slot when construction of an SDK future fails synchronously.
     */
    public void releaseFailedStart() {
        permits.release();
        if (globalPermits != null) {
            globalPermits.release();
        }
    }

    /**
     * Wait for every submitted operation and surface the first failure.
     *
     * @throws IOException when an operation failed or the wait was interrupted
     */
    public void await() throws IOException {
        while (!pending.isEmpty()) {
            CompletableFuture<?>[] futures = pending.toArray(CompletableFuture[]::new);
            try {
                CompletableFuture.allOf(futures).join();
            } catch (CompletionException ignored) {
                // The original cause is retained in failure and reported below.
            }
        }
        throwIfFailed();
    }

    /**
     * Number of currently pending operations.
     *
     * @return pending operation count
     */
    public int pendingCount() {
        return pending.size();
    }

    /**
     * Identify an exception caused by normal benchmark shutdown.
     *
     * @param thrown operation failure
     * @return true when an interrupt or closed SDK executor caused the failure
     */
    public static boolean isCleanShutdown(Throwable thrown) {
        Throwable cause = thrown;
        while (cause != null) {
            if (cause instanceof InterruptedException
                    || cause instanceof InterruptedIOException
                    || cause instanceof RejectedExecutionException) {
                return true;
            }
            cause = cause.getCause();
        }
        return false;
    }

    private void throwIfFailed() throws IOException {
        Throwable thrown = failure.get();
        if (thrown != null) {
            throw new IOException("Asynchronous S3 operation failed: " + thrown.getMessage(), thrown);
        }
    }

    private static Throwable unwrap(Throwable thrown) {
        Throwable cause = thrown;
        while ((cause instanceof CompletionException) && cause.getCause() != null) {
            cause = cause.getCause();
        }
        return cause;
    }
}
