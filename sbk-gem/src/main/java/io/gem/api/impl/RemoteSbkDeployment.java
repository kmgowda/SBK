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

import io.gem.api.SshResponse;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Builds the remote SBK version probe and interprets its response.
 */
final class RemoteSbkDeployment {
    private static final Pattern VERSION_PATTERN = Pattern.compile("(?m)^SBK Version:\\s*(\\S+)\\s*$");

    private RemoteSbkDeployment() {
    }

    /**
     * Build a POSIX-shell command that reports the installed SBK version.
     *
     * @param commandPath absolute or working-directory-relative remote SBK executable path
     * @return remote shell command
     */
    static String versionProbeCommand(String commandPath) {
        final String quotedCommand = shellQuote(commandPath);
        return "if [ -x " + quotedCommand + " ]; then " + quotedCommand + " -version; else exit 127; fi";
    }

    /**
     * Decide whether the remote response identifies the expected SBK version.
     *
     * @param response remote command response
     * @param expectedVersion expected SBK version
     * @return true only when the command succeeded and printed the exact expected version
     */
    static boolean hasExpectedVersion(SshResponse response, String expectedVersion) {
        if (response == null || response.returnCode != 0 || expectedVersion == null || expectedVersion.isBlank()) {
            return false;
        }
        final Matcher matcher = VERSION_PATTERN.matcher(response.stdOutputStream.toString());
        return matcher.find() && expectedVersion.equals(matcher.group(1));
    }

    /**
     * Decide whether an SBK copy is required.
     *
     * @param forceCopy whether the caller explicitly requested an unconditional copy
     * @param response remote version-probe response
     * @param expectedVersion expected SBK version
     * @return true for force-copy, missing SBK, failed probes, and version mismatches
     */
    static boolean requiresCopy(boolean forceCopy, SshResponse response, String expectedVersion) {
        return forceCopy || !hasExpectedVersion(response, expectedVersion);
    }

    /**
     * Quote a value for use as one POSIX-shell argument.
     *
     * @param value unquoted value
     * @return safely single-quoted value
     */
    static String shellQuote(String value) {
        return "'" + value.replace("'", "'\\''") + "'";
    }
}
