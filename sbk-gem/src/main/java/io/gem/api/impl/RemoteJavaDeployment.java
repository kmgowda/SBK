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
 * Builds remote Java discovery commands and interprets their responses.
 */
final class RemoteJavaDeployment {
    private static final Pattern QUOTED_VERSION_PATTERN = Pattern.compile("version \\\"(\\d+)(?:\\.(\\d+))?.*\\\"");
    private static final Pattern SIMPLE_VERSION_PATTERN = Pattern.compile("^(\\d+)(?:\\.(\\d+))?.*$");
    private static final Pattern JAVA_HOME_PATTERN = Pattern.compile("(?m)^SBK_JAVA_HOME=(.+)$");

    private RemoteJavaDeployment() {
    }

    /**
     * Build a command that discovers Java from the remote {@code PATH}.
     *
     * @return POSIX-shell discovery command
     */
    static String pathProbeCommand() {
        return "JAVA_BIN=$(command -v java) || exit 127; " +
                "JAVA_BIN=$(readlink -f \"$JAVA_BIN\" 2>/dev/null || printf '%s' \"$JAVA_BIN\"); " +
                "SBK_HOME=$(dirname \"$(dirname \"$JAVA_BIN\")\"); " +
                "\"$JAVA_BIN\" -version; printf '\\nSBK_JAVA_HOME=%s\\n' \"$SBK_HOME\"";
    }

    /**
     * Build a command that checks Java in a specific remote home directory.
     *
     * @param javaHome expected remote Java home
     * @return POSIX-shell probe command
     */
    static String homeProbeCommand(String javaHome) {
        final String quotedHome = RemoteSbkDeployment.shellQuote(javaHome);
        final String quotedJava = RemoteSbkDeployment.shellQuote(javaHome + "/bin/java");
        return "if [ -x " + quotedJava + " ]; then " + quotedJava +
                " -version; printf '\\nSBK_JAVA_HOME=%s\\n' " + quotedHome + "; else exit 127; fi";
    }

    /**
     * Determine whether a probe found the requested Java major version.
     *
     * @param response remote response
     * @param expectedMajor expected Java major version
     * @return true when the probe succeeded and its Java major version matches
     */
    static boolean hasExpectedVersion(SshResponse response, int expectedMajor) {
        if (response == null || response.returnCode != 0) {
            return false;
        }
        final int stdoutMajor = parseMajorVersion(response.stdOutputStream.toString());
        final int stderrMajor = parseMajorVersion(response.errOutputStream.toString());
        return stdoutMajor == expectedMajor || stderrMajor == expectedMajor;
    }

    /**
     * Extract the Java home printed by a discovery command.
     *
     * @param response remote response
     * @return discovered Java home, or null when absent
     */
    static String javaHome(SshResponse response) {
        if (response == null) {
            return null;
        }
        final Matcher matcher = JAVA_HOME_PATTERN.matcher(response.stdOutputStream.toString());
        return matcher.find() ? matcher.group(1).trim() : null;
    }

    /**
     * Parse a Java major version from either {@code java -version} output or a version property.
     *
     * @param text version text
     * @return Java major version, or -1 when it cannot be parsed
     */
    static int parseMajorVersion(String text) {
        if (text == null || text.isBlank()) {
            return -1;
        }
        Matcher matcher = QUOTED_VERSION_PATTERN.matcher(text);
        if (!matcher.find()) {
            matcher = SIMPLE_VERSION_PATTERN.matcher(text.trim());
            if (!matcher.find()) {
                return -1;
            }
        }
        final int first = Integer.parseInt(matcher.group(1));
        return first == 1 && matcher.group(2) != null ? Integer.parseInt(matcher.group(2)) : first;
    }

    /**
     * Build the environment exports used to launch remote SBK.
     *
     * @param javaHome selected remote Java home
     * @return POSIX-shell export prefix
     */
    static String environmentPrefix(String javaHome) {
        return "export SBK_JAVA_HOME=" + RemoteSbkDeployment.shellQuote(javaHome) +
                "; export PATH=\"$SBK_JAVA_HOME/bin:$PATH\"; ";
    }
}
