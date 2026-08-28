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

import io.gem.agent.RemoteDeploymentContract;
import io.gem.agent.RemotePath;
import io.gem.config.GemConfig;
import io.sbk.system.Printer;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/** Shared non-stateful deployment helpers. */
final class DeploymentSupport {
    private DeploymentSupport() {
    }

    static String remoteJavaExecutable(String javaHome) {
        return RemotePath.join(javaHome, RemoteDeploymentContract.JAVA_EXECUTABLE);
    }

    static String remoteSbkDirectory(String runtimeDirectory) {
        return RemotePath.join(runtimeDirectory, RemoteDeploymentContract.SBK_DIRECTORY);
    }

    static Path runtimeCacheDirectory(GemConfig config) {
        final Path configuredCache = Path.of(config.runtimeCacheDirectory);
        return configuredCache.isAbsolute() ? configuredCache
                : Path.of(System.getProperty("user.home")).resolve(configuredCache);
    }

    static String[] endpointIdentities(List<RemoteNodeState> nodes) {
        return nodes.stream().map(RemoteNodeState::endpointIdentity).toArray(String[]::new);
    }

    static boolean hasSelectedTarget(boolean[] selected) {
        for (boolean value : selected) {
            if (value) {
                return true;
            }
        }
        return false;
    }

    static void waitFor(CompletableFuture<?> future, long timeoutSeconds, String operation)
            throws IOException, InterruptedException, ExecutionException {
        try {
            future.get(timeoutSeconds, TimeUnit.SECONDS);
        } catch (TimeoutException exception) {
            final String message = "SBK-GEM: " + operation + " timed out after "
                    + timeoutSeconds + " seconds";
            Printer.log.error(message);
            throw new IOException(message, exception);
        }
    }

    static String diagnosticSummary(String output, GemConfig config) {
        return diagnosticSummary(output, config.maximumDiagnosticCharacters,
                config.diagnosticPrefixCharacters);
    }

    static String diagnosticSummary(String output, int maximumCharacters, int prefixCharacters) {
        if (output == null || output.isBlank()) {
            return "";
        }
        final String normalized = output.replaceAll("\\s+", " ").trim();
        if (normalized.length() <= maximumCharacters) {
            return normalized;
        }
        final int suffixCharacters = maximumCharacters - prefixCharacters
                - GemConfig.DIAGNOSTIC_TRUNCATION_MARKER.length();
        return normalized.substring(0, prefixCharacters) + GemConfig.DIAGNOSTIC_TRUNCATION_MARKER
                + normalized.substring(normalized.length() - suffixCharacters);
    }

    static String failureDescription(Throwable failure) {
        final Throwable cause = unwrap(failure);
        if (cause == null) {
            return "unknown failure";
        }
        final String message = cause.getMessage();
        return message == null || message.isBlank() ? cause.getClass().getSimpleName() : message;
    }

    private static Throwable unwrap(Throwable failure) {
        Throwable cause = failure;
        while ((cause instanceof CompletionException || cause instanceof ExecutionException)
                && cause.getCause() != null) {
            cause = cause.getCause();
        }
        return cause;
    }
}
