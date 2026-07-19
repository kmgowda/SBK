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

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.concurrent.TimeUnit;

/**
 * Discovers and validates the SBK version in the local distribution selected for deployment.
 */
final class LocalSbkDeployment {
    private LocalSbkDeployment() {
    }

    /**
     * Run the selected local SBK launcher and read its authoritative version.
     *
     * @param executable local SBK executable
     * @param timeoutSeconds finite discovery timeout in seconds
     * @return version reported by the executable
     * @throws IOException when the launcher fails, times out, or reports invalid output
     * @throws InterruptedException when version discovery is interrupted
     */
    static String discoverVersion(Path executable, long timeoutSeconds) throws IOException, InterruptedException {
        final Process process = new ProcessBuilder(executable.toString(), "-version")
                .redirectErrorStream(true)
                .start();
        final boolean completed = process.waitFor(timeoutSeconds, TimeUnit.SECONDS);
        if (!completed) {
            process.destroyForcibly();
            throw new IOException("Local SBK version discovery timed out after " + timeoutSeconds + " seconds");
        }
        final String output = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
        final String version = RemoteSbkDeployment.parseVersion(output);
        if (process.exitValue() != 0 || version == null || version.isBlank()) {
            throw new IOException("Unable to determine SBK version from " + executable +
                    "; return code " + process.exitValue() + ", output: " + output.trim());
        }
        return version;
    }
}
