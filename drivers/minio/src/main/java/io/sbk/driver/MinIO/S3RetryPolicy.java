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

import io.minio.errors.ErrorResponseException;

import java.io.IOException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

/**
 * Bounded retry policy for transient SDK failures.
 *
 * <p>Retries are disabled by the default single attempt. When enabled, the
 * complete retry sequence remains one timed SBK operation.
 */
public final class S3RetryPolicy {
    private final int maxAttempts;
    private final long backoffMs;
    private final Strategy strategy;
    private final long maxBackoffMs;
    private final boolean jitter;
    private final Runnable retryListener;

    /**
     * Create a retry policy.
     *
     * @param maxAttempts total attempts including the first
     * @param backoffMs fixed delay between attempts
     * @throws IllegalArgumentException when attempts or delay are invalid
     */
    public S3RetryPolicy(int maxAttempts, long backoffMs) {
        this(maxAttempts, backoffMs, Strategy.FIXED, 0, false, () -> { });
    }

    /**
     * Create a retry policy with a slow-path retry observer.
     *
     * @param maxAttempts total attempts including the first
     * @param backoffMs fixed delay between attempts
     * @param retryListener action invoked only when another attempt will run
     * @throws IllegalArgumentException when attempts or delay are invalid
     */
    public S3RetryPolicy(int maxAttempts, long backoffMs, Runnable retryListener) {
        this(maxAttempts, backoffMs, Strategy.FIXED, 0, false, retryListener);
    }

    /**
     * Create a configurable retry policy with a slow-path retry observer.
     *
     * @param maxAttempts total attempts including the first
     * @param backoffMs initial/fixed delay between attempts
     * @param strategy retry delay strategy
     * @param maxBackoffMs maximum exponential delay, or zero for no explicit cap
     * @param jitter whether to randomize each calculated delay
     * @param retryListener action invoked only when another attempt will run
     * @throws IllegalArgumentException when attempts, delay, or strategy are invalid
     */
    public S3RetryPolicy(int maxAttempts, long backoffMs, Strategy strategy,
                         long maxBackoffMs, boolean jitter, Runnable retryListener) {
        if (maxAttempts < 1 || backoffMs < 0 || maxBackoffMs < 0 || strategy == null) {
            throw new IllegalArgumentException("retry attempts must be positive and delay non-negative");
        }
        this.maxAttempts = maxAttempts;
        this.backoffMs = backoffMs;
        this.strategy = strategy;
        this.maxBackoffMs = maxBackoffMs;
        this.jitter = jitter;
        this.retryListener = retryListener;
    }

    /**
     * Execute a synchronous SDK operation.
     *
     * @param supplier operation supplier
     * @param <T> result type
     * @return successful result
     * @throws Exception final operation failure
     */
    public <T> T execute(ThrowingSupplier<T> supplier) throws Exception {
        int attempt = 1;
        while (true) {
            try {
                return supplier.get();
            } catch (Exception ex) {
                if (attempt >= maxAttempts || !isRetryable(ex)) {
                    throw ex;
                }
                retryListener.run();
                delay(attempt);
                attempt++;
            }
        }
    }

    /**
     * Execute an asynchronous SDK operation.
     *
     * @param supplier future supplier
     * @param <T> result type
     * @return future representing all attempts
     */
    public <T> CompletableFuture<T> executeAsync(
            ThrowingSupplier<CompletableFuture<T>> supplier) {
        return attemptAsync(supplier, 1);
    }

    private <T> CompletableFuture<T> attemptAsync(
            ThrowingSupplier<CompletableFuture<T>> supplier, int attempt) {
        final CompletableFuture<T> future;
        try {
            future = supplier.get();
        } catch (Exception ex) {
            return retryOrFail(supplier, attempt, ex);
        }
        return future.handle((result, thrown) -> {
            if (thrown == null) {
                return CompletableFuture.completedFuture(result);
            }
            return retryOrFail(supplier, attempt, unwrap(thrown));
        }).thenCompose(value -> value);
    }

    private <T> CompletableFuture<T> retryOrFail(
            ThrowingSupplier<CompletableFuture<T>> supplier, int attempt, Throwable thrown) {
        if (attempt >= maxAttempts || !isRetryable(thrown)) {
            return CompletableFuture.failedFuture(thrown);
        }
        retryListener.run();
        long retryDelay = retryDelay(attempt);
        return CompletableFuture.supplyAsync(() -> null,
                        CompletableFuture.delayedExecutor(retryDelay, TimeUnit.MILLISECONDS))
                .thenCompose(ignored -> attemptAsync(supplier, attempt + 1));
    }

    private void delay(int failedAttempt) throws InterruptedException {
        long retryDelay = retryDelay(failedAttempt);
        if (retryDelay > 0) {
            Thread.sleep(retryDelay);
        }
    }

    private long retryDelay(int failedAttempt) {
        long calculated = backoffMs;
        if (strategy == Strategy.EXPONENTIAL && backoffMs > 0) {
            int shift = Math.min(Long.SIZE - 2, Math.max(0, failedAttempt - 1));
            calculated = backoffMs > (Long.MAX_VALUE >> shift)
                    ? Long.MAX_VALUE : backoffMs << shift;
        }
        if (maxBackoffMs > 0) {
            calculated = Math.min(calculated, maxBackoffMs);
        }
        if (jitter && calculated > 0) {
            return calculated == Long.MAX_VALUE
                    ? ThreadLocalRandom.current().nextLong(Long.MAX_VALUE)
                    : ThreadLocalRandom.current().nextLong(calculated + 1);
        }
        return calculated;
    }

    private static boolean isRetryable(Throwable thrown) {
        Throwable cause = unwrap(thrown);
        if (cause instanceof ErrorResponseException response) {
            int code = response.response().code();
            return code == 429 || code >= 500;
        }
        return cause instanceof IOException && !S3AsyncExecutor.isCleanShutdown(cause);
    }

    private static Throwable unwrap(Throwable thrown) {
        Throwable cause = thrown;
        while (cause instanceof CompletionException && cause.getCause() != null) {
            cause = cause.getCause();
        }
        return cause;
    }

    /** Retry-delay strategy selected once during driver startup. */
    public enum Strategy {
        FIXED,
        EXPONENTIAL;

        /**
         * Parse a command-line retry strategy.
         *
         * @param value strategy name
         * @return parsed strategy
         * @throws IllegalArgumentException when the strategy is unsupported
         */
        public static Strategy parse(String value) {
            if (value == null || value.isBlank()) {
                return FIXED;
            }
            try {
                return valueOf(value.trim().toUpperCase(java.util.Locale.ROOT));
            } catch (RuntimeException ex) {
                throw new IllegalArgumentException(
                        "retry-strategy must be fixed or exponential", ex);
            }
        }
    }

    /**
     * Supplier whose operation may throw an SDK exception.
     *
     * @param <T> supplied type
     */
    @FunctionalInterface
    public interface ThrowingSupplier<T> {
        /**
         * Produce a value.
         *
         * @return supplied value
         * @throws Exception operation failure
         */
        T get() throws Exception;
    }
}
