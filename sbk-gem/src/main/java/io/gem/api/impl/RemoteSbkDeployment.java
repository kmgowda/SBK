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
     * Build a POSIX-shell command that resolves and verifies the remote SBK executable.
     *
     * @param remoteDirectory remote deployment directory, absolute or relative to the SSH login directory
     * @param relativeCommandPath SBK executable path relative to the deployment directory
     * @return remote shell command that prints the verified absolute executable path
     * @throws IllegalArgumentException when either path is null, blank, or the command path is absolute
     */
    static String executablePathProbeCommand(String remoteDirectory, String relativeCommandPath) {
        if (remoteDirectory == null || remoteDirectory.isBlank()) {
            throw new IllegalArgumentException("Remote directory must not be blank");
        }
        if (relativeCommandPath == null || relativeCommandPath.isBlank() || relativeCommandPath.startsWith("/")) {
            throw new IllegalArgumentException("Remote SBK command path must be relative");
        }
        return "cd -- " + shellQuote(remoteDirectory) + " && base=\"$(pwd -P)\" && sbk_path=\"$base/\"" +
                shellQuote(relativeCommandPath) + " && if [ -x \"$sbk_path\" ]; then printf '%s\\n' " +
                "\"$sbk_path\"; else printf '%s\\n' \"SBK executable not found or not executable: $sbk_path\" " +
                ">&2; exit 127; fi";
    }

    /**
     * Extract a successfully resolved absolute executable path from a remote response.
     *
     * @param response remote command response
     * @return absolute executable path, or null when resolution failed or returned invalid output
     */
    static String absoluteExecutablePath(SshResponse response) {
        if (response == null || response.returnCode != 0) {
            return null;
        }
        final String path = response.stdOutputStream.toString().trim();
        if (!path.startsWith("/") || path.contains("\n") || path.contains("\r")) {
            return null;
        }
        return path;
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
     * @param copyEnabled whether copying missing or mismatched SBK is enabled
     * @param response remote version-probe response
     * @param expectedVersion expected SBK version
     * @return true when copying is enabled and the expected version is unavailable
     */
    static boolean requiresCopy(boolean copyEnabled, SshResponse response, String expectedVersion) {
        return copyEnabled && response != null && (response.returnCode == 0 || response.returnCode == 127) &&
                !hasExpectedVersion(response, expectedVersion);
    }

    /**
     * Decide whether an existing remote SBK should be deleted before replacement.
     *
     * @param deleteEnabled whether pre-copy deletion is enabled
     * @param response remote version-probe response
     * @param expectedVersion expected SBK version
     * @return true when a remote executable exists but does not provide the expected version
     */
    static boolean requiresDeleteBeforeCopy(boolean deleteEnabled, SshResponse response, String expectedVersion) {
        return deleteEnabled && response != null && response.returnCode == 0 &&
                !hasExpectedVersion(response, expectedVersion);
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
