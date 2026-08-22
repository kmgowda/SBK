/**
 * Copyright (c) KMG. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 */
package io.perl.api;

import io.perl.exception.BenchmarkIdleTimeoutException;

import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutionException;

/**
 * Describes why a benchmark lifecycle ended.
 */
public enum BenchmarkTermination {
    /** The configured {@code -seconds} duration elapsed successfully. */
    SECONDS_COMPLETED,
    /** The configured {@code -records} work completed successfully. */
    RECORDS_COMPLETED,
    /** A caller or process lifecycle requested an orderly stop. */
    STOP_REQUESTED,
    /** No performance event arrived before {@code -idletimeoutseconds}. */
    IDLE_TIMEOUT,
    /** An internal benchmark, logger, transport, or cleanup operation failed. */
    INTERNAL_FAILURE;

    /**
     * Selects the successful completion condition configured for a run.
     *
     * @param secondsToRun configured duration
     * @param recordsCount configured record target
     * @return duration, record, or explicit-stop termination
     */
    public static BenchmarkTermination configured(long secondsToRun, long recordsCount) {
        if (secondsToRun > 0) {
            return SECONDS_COMPLETED;
        }
        if (recordsCount > 0) {
            return RECORDS_COMPLETED;
        }
        return STOP_REQUESTED;
    }

    /**
     * Resolves a requested completion reason against an optional failure.
     *
     * @param requested reason expected by the lifecycle coordinator
     * @param failure terminal failure, or {@code null}
     * @return the authoritative termination reason
     */
    public static BenchmarkTermination resolve(BenchmarkTermination requested, Throwable failure) {
        if (failure == null) {
            return requested;
        }
        return BenchmarkIdleTimeoutException.find(failure) == null
                ? INTERNAL_FAILURE : IDLE_TIMEOUT;
    }

    /**
     * Builds a consistent operator-facing termination description.
     *
     * @param secondsToRun configured duration
     * @param recordsCount configured record target
     * @param idleTimeoutSeconds configured idle timeout
     * @param failure terminal failure, or {@code null}
     * @return termination description
     */
    public String describe(long secondsToRun, long recordsCount, int idleTimeoutSeconds, Throwable failure) {
        return switch (this) {
            case SECONDS_COMPLETED -> "completed successfully in -seconds " + secondsToRun + " mode";
            case RECORDS_COMPLETED -> "completed successfully in -records " + recordsCount + " mode";
            case STOP_REQUESTED -> "stopped by a lifecycle request";
            case IDLE_TIMEOUT -> "exited due to -idletimeoutseconds " + idleTimeoutSeconds + ": "
                    + failureDetail(failure);
            case INTERNAL_FAILURE -> "exited due to internal exception: " + failureDetail(failure);
        };
    }

    /**
     * Reports whether the termination represents configured benchmark success.
     *
     * @return {@code true} for duration or record completion
     */
    public boolean isSuccessfulCompletion() {
        return this == SECONDS_COMPLETED || this == RECORDS_COMPLETED;
    }

    private static String failureDetail(Throwable failure) {
        Throwable cause = failure;
        while ((cause instanceof CompletionException || cause instanceof ExecutionException)
                && cause.getCause() != null) {
            cause = cause.getCause();
        }
        if (cause == null) {
            return "unknown failure";
        }
        final String message = cause.getMessage();
        return cause.getClass().getSimpleName() + (message == null || message.isBlank() ? "" : ": " + message);
    }
}
