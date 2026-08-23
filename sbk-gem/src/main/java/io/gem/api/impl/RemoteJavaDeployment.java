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

import io.gem.api.RemoteExitCode;
import io.gem.api.SshResponse;
import io.sbk.config.ExitCode;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Builds remote Java discovery commands and interprets their responses.
 */
final class RemoteJavaDeployment {
    private static final int LEGACY_JAVA_MAJOR = 1;
    private static final int VERSION_MAJOR_GROUP = 1;
    private static final int VERSION_MINOR_GROUP = 2;
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
        return "JAVA_BIN=$(command -v java) || exit " + RemoteExitCode.COMMAND_NOT_FOUND + "; " +
                "if command -v realpath >/dev/null 2>&1; then " +
                "JAVA_BIN=$(realpath \"$JAVA_BIN\") || exit " + RemoteExitCode.COMMAND_NOT_FOUND + "; " +
                "elif readlink -f \"$JAVA_BIN\" >/dev/null 2>&1; then " +
                "JAVA_BIN=$(readlink -f \"$JAVA_BIN\") || exit " + RemoteExitCode.COMMAND_NOT_FOUND + "; " +
                "else while [ -L \"$JAVA_BIN\" ]; do " +
                "JAVA_LINK=$(readlink \"$JAVA_BIN\") || exit " + RemoteExitCode.COMMAND_NOT_FOUND + "; " +
                "case \"$JAVA_LINK\" in /*) JAVA_BIN=$JAVA_LINK ;; " +
                "*) JAVA_BIN=$(dirname \"$JAVA_BIN\")/$JAVA_LINK ;; esac; " +
                "JAVA_DIR=$(CDPATH= cd -P \"$(dirname \"$JAVA_BIN\")\" && pwd) || exit " +
                RemoteExitCode.COMMAND_NOT_FOUND + "; " +
                "JAVA_BIN=$JAVA_DIR/$(basename \"$JAVA_BIN\"); done; fi; " +
                "JAVA_DIR=$(CDPATH= cd -P \"$(dirname \"$JAVA_BIN\")\" && pwd) || exit " +
                RemoteExitCode.COMMAND_NOT_FOUND + "; " +
                "JAVA_BIN=$JAVA_DIR/$(basename \"$JAVA_BIN\"); " +
                "SBK_HOME=$(dirname \"$(dirname \"$JAVA_BIN\")\"); " +
                "test -x \"$SBK_HOME/bin/javac\" || exit " + RemoteExitCode.COMMAND_NOT_FOUND + "; " +
                "\"$JAVA_BIN\" -version; \"$SBK_HOME/bin/javac\" -version; " +
                "printf '\\nSBK_JAVA_HOME=%s\\n' \"$SBK_HOME\"";
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
        final String quotedJavac = RemoteSbkDeployment.shellQuote(javaHome + "/bin/javac");
        return "if [ -x " + quotedJava + " ] && [ -x " + quotedJavac + " ]; then " + quotedJava
                + " -version; " + quotedJavac + " -version; printf '\\nSBK_JAVA_HOME=%s\\n' "
                + quotedHome + "; else exit "
                + RemoteExitCode.COMMAND_NOT_FOUND + "; fi";
    }

    /**
     * Resolve the remote Java destination used by SBK-GEM.
     *
     * <p>An explicitly configured Java home takes precedence. Otherwise, the
     * managed Java directory is placed beside the remote SBK working
     * directory so a runtime copied during an earlier execution can be reused.
     *
     * @param connectionDir remote SBK working directory
     * @param configuredJavaHome explicitly configured remote Java home, or null
     * @param expectedMajor requested Java major version
     * @return remote Java home to probe before copying and to use as the copy destination
     * @throws IllegalArgumentException when the remote connection directory is not absolute
     */
    static String destinationJavaHome(String connectionDir, String configuredJavaHome, int expectedMajor) {
        if (connectionDir == null || !connectionDir.startsWith("/")) {
            throw new IllegalArgumentException("Remote connection directory must be absolute: " + connectionDir);
        }
        final String absoluteConnectionDir = normalizeAbsoluteRemotePath(connectionDir);
        if (configuredJavaHome != null) {
            return normalizeAbsoluteRemotePath(configuredJavaHome.startsWith("/") ? configuredJavaHome :
                    absoluteConnectionDir + "/" + configuredJavaHome);
        }
        final int separator = absoluteConnectionDir.lastIndexOf('/');
        final String parent = separator == 0 ? "/" : absoluteConnectionDir.substring(0, separator);
        return normalizeAbsoluteRemotePath(parent + "/sbk-java-" + expectedMajor);
    }

    /**
     * Determine whether a probe found the requested Java major version.
     *
     * @param response remote response
     * @param expectedMajor expected Java major version
     * @return true when the probe succeeded and its Java major version matches
     */
    static boolean hasExpectedVersion(SshResponse response, int expectedMajor) {
        if (response == null || response.returnCode != ExitCode.SUCCESS) {
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
        if (!matcher.find()) {
            return null;
        }
        final String javaHome = matcher.group(1).trim();
        return javaHome.startsWith("/") ? normalizeAbsoluteRemotePath(javaHome) : null;
    }

    private static String normalizeAbsoluteRemotePath(String path) {
        final Deque<String> segments = new ArrayDeque<>();
        for (String segment : path.split("/")) {
            if (segment.isEmpty() || ".".equals(segment)) {
                continue;
            }
            if ("..".equals(segment)) {
                segments.pollLast();
            } else {
                segments.addLast(segment);
            }
        }
        return "/" + String.join("/", segments);
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
        final int first = Integer.parseInt(matcher.group(VERSION_MAJOR_GROUP));
        return first == LEGACY_JAVA_MAJOR && matcher.group(VERSION_MINOR_GROUP) != null
                ? Integer.parseInt(matcher.group(VERSION_MINOR_GROUP)) : first;
    }

    /**
     * Build the environment exports used to launch remote SBK.
     *
     * @param javaHome selected remote Java home
     * @return POSIX-shell export prefix
     * @throws IllegalArgumentException if the selected Java home is empty
     */
    static String environmentPrefix(String javaHome) {
        if (javaHome == null || javaHome.isBlank()) {
            throw new IllegalArgumentException("Remote SBK_JAVA_HOME must not be empty");
        }
        return "export SBK_JAVA_HOME=" + RemoteSbkDeployment.shellQuote(javaHome) +
                "; export PATH=\"$SBK_JAVA_HOME/bin:$PATH\"; ";
    }

    /**
     * Build a remote SBK launch command using the Java home resolved for one
     * specific node. The exported value is inherited by the SBK launcher, which
     * selects {@code $SBK_JAVA_HOME/bin/java} before {@code JAVA_HOME} or PATH.
     *
     * @param javaHome Java home resolved or installed on the target node
     * @param sbkCommand complete SBK command to execute on that node
     * @return POSIX-shell command exporting the node's Java home before SBK starts
     * @throws IllegalArgumentException if the Java home or SBK command is empty
     */
    static String launchCommand(String javaHome, String sbkCommand) {
        if (sbkCommand == null || sbkCommand.isBlank()) {
            throw new IllegalArgumentException("Remote SBK command must not be empty");
        }
        return environmentPrefix(javaHome) + sbkCommand;
    }
}
