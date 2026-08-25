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

import io.gem.agent.RemoteAgentProtocol;
import io.gem.api.SshResponse;
import io.sbk.config.ExitCode;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Builds and parses remote Java-agent requests without remote shell programs. */
final class RemoteAgent {
    private static final Pattern JAVA_HOME = Pattern.compile("(?m)^SBK_JAVA_HOME=(.+)$");
    private static final Pattern OPERATING_SYSTEM = Pattern.compile("(?m)^SBK_OS=(\\S+)$");

    private RemoteAgent() {
    }

    static String command(String javaExecutable, String agentJar) {
        return quote(javaExecutable) + " -jar " + quote(agentJar);
    }

    static byte[] probe(int javaVersion) throws IOException {
        return RemoteAgentProtocol.encode("probe", List.of(Integer.toString(javaVersion)));
    }

    static byte[] activate(String archive, String archiveDigest, String contentDigest, String staging,
                           String destination, String operatingSystem) throws IOException {
        return RemoteAgentProtocol.encode("activate", List.of(archive, archiveDigest, contentDigest, staging,
                destination, operatingSystem));
    }

    static byte[] verify(String destination, String contentDigest, String version, String operatingSystem)
            throws IOException {
        return RemoteAgentProtocol.encode("verify", List.of(destination, contentDigest, version, operatingSystem));
    }

    static byte[] run(String destination, String version, List<String> jvmArgs, List<String> sbkArgs)
            throws IOException {
        final List<String> values = new ArrayList<>(3 + jvmArgs.size() + sbkArgs.size());
        values.add(destination);
        values.add(version);
        values.add(Integer.toString(jvmArgs.size()));
        values.addAll(jvmArgs);
        values.addAll(sbkArgs);
        return RemoteAgentProtocol.encode("run", values);
    }

    static boolean archiveDigestMismatch(SshResponse response) {
        return response != null && response.returnCode != ExitCode.SUCCESS
                && response.errOutputStream.toString().contains(RemoteAgentProtocol.ARCHIVE_DIGEST_MISMATCH);
    }

    static boolean successful(SshResponse response) {
        return response != null && response.returnCode == ExitCode.SUCCESS;
    }

    static String javaHome(SshResponse response) {
        return match(JAVA_HOME, response);
    }

    static DeploymentPlatform platform(SshResponse response) {
        final String value = match(OPERATING_SYSTEM, response);
        return value == null ? null : DeploymentPlatform.fromOperatingSystem(value);
    }

    private static String match(Pattern pattern, SshResponse response) {
        if (!successful(response)) {
            return null;
        }
        final Matcher matcher = pattern.matcher(response.stdOutputStream.toString());
        return matcher.find() ? matcher.group(1).trim() : null;
    }

    private static String quote(String value) {
        if (value == null || value.isBlank() || value.indexOf('\n') >= 0 || value.indexOf('\r') >= 0
                || value.indexOf('\0') >= 0) {
            throw new IllegalArgumentException("Invalid remote executable path");
        }
        return "'" + value.replace("'", "'\\''") + "'";
    }
}
