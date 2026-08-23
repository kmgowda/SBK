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

/**
 * Builds POSIX-shell commands for content-addressed SBK runtime deployments.
 */
final class RemoteRuntimeDeployment {
    private RemoteRuntimeDeployment() {
    }

    /**
     * Build a probe which accepts only the exact immutable runtime content.
     *
     * @param deploymentDirectory absolute remote runtime directory
     * @param expectedContentDigest expected runtime content digest
     * @param javaHome absolute Java home used by the deployment
     * @param sbkCommand absolute SBK launcher path
     * @param expectedSbkVersion expected SBK version
     * @param expectedJavaVersion expected Java major version
     * @return remote probe command
     */
    static String probeCommand(String deploymentDirectory, String expectedContentDigest,
                               String javaHome, String sbkCommand, String expectedSbkVersion,
                               int expectedJavaVersion) {
        final String digestFile = deploymentDirectory + "/" + SbkRuntimeBundle.REMOTE_DIGEST_FILE;
        final String javaVersionPattern = "version \\\"?" + expectedJavaVersion + "([.]|\\\")";
        final String javacVersionPattern = "^javac " + expectedJavaVersion + "([.]|$)";
        return "test -f " + quote(digestFile)
                + " && test \"$(cat " + quote(digestFile) + ")\" = " + quote(expectedContentDigest)
                + " && test -x " + quote(javaHome + "/bin/java")
                + " && test -x " + quote(javaHome + "/bin/javac")
                + " && java_version=$(" + quote(javaHome + "/bin/java") + " -version 2>&1)"
                + " && printf '%s\\n' \"$java_version\" | grep -Eq " + quote(javaVersionPattern)
                + " && javac_version=$(" + quote(javaHome + "/bin/javac") + " -version 2>&1)"
                + " && printf '%s\\n' \"$javac_version\" | grep -Eq " + quote(javacVersionPattern)
                + " && test -x " + quote(sbkCommand)
                + " && sbk_version=$(" + RemoteJavaDeployment.environmentPrefix(javaHome)
                + quote(sbkCommand) + " -version 2>&1)"
                + " && printf '%s\\n' \"$sbk_version\" | grep -Fq "
                + quote("SBK Version: " + expectedSbkVersion)
                + " && printf 'SBK_RUNTIME_CONTENT=%s\\nSBK_JAVA_MAJOR=%s\\nSBK_VERSION=%s\\n' "
                + quote(expectedContentDigest) + " " + expectedJavaVersion + " " + quote(expectedSbkVersion);
    }

    /**
     * Build an archive verification, extraction, full-file verification, and
     * atomic activation command.
     *
     * @param archivePath uploaded archive path
     * @param expectedArchiveDigest expected archive SHA-256
     * @param expectedContentDigest expected extracted content SHA-256
     * @param stagingDirectory unique staging directory
     * @param deploymentDirectory final content-addressed directory
     * @param expectedOperatingSystem normalized expected operating system
     * @param expectedArchitecture normalized expected architecture
     * @param replaceInvalid whether an invalid exact content-addressed destination may be replaced
     * @return remote activation command
     */
    static String activateCommand(String archivePath, String expectedArchiveDigest,
                                  String expectedContentDigest, String stagingDirectory,
                                  String deploymentDirectory, String expectedOperatingSystem,
                                  String expectedArchitecture, boolean replaceInvalid) {
        final String extractedRoot = stagingDirectory + "/" + SbkRuntimeBundle.ARCHIVE_ROOT;
        final String descriptor = extractedRoot + "/" + SbkRuntimeBundle.DESCRIPTOR_FILE;
        final String checksums = extractedRoot + "/" + SbkRuntimeBundle.CHECKSUM_FILE;
        final String marker = deploymentDirectory + "/" + SbkRuntimeBundle.REMOTE_DIGEST_FILE;
        return "set -eu; archive=" + quote(archivePath) + "; staging=" + quote(stagingDirectory)
                + "; final_dir=" + quote(deploymentDirectory) + "; "
                + "cleanup() { rm -f \"$archive\"; rm -rf \"$staging\"; }; "
                + "trap cleanup EXIT HUP INT TERM; "
                + "if command -v sha256sum >/dev/null 2>&1; then actual=$(sha256sum \"$archive\"); "
                + "else actual=$(shasum -a 256 \"$archive\"); fi; actual=${actual%% *}; "
                + "test \"$actual\" = " + quote(expectedArchiveDigest)
                + " || { printf 'runtime archive SHA-256 mismatch: expected %s, found %s\\n' "
                + quote(expectedArchiveDigest) + " \"$actual\" >&2; exit 65; }; "
                + "rm -rf \"$staging\"; mkdir -p \"$staging\"; tar -xzf \"$archive\" -C \"$staging\"; "
                + "test -f " + quote(descriptor) + "; test -f " + quote(checksums) + "; "
                + "grep -Fqx " + quote("content.sha256=" + expectedContentDigest) + " " + quote(descriptor)
                + "; grep -Fqx " + quote("platform.os=" + expectedOperatingSystem) + " " + quote(descriptor)
                + "; grep -Fqx " + quote("platform.arch=" + expectedArchitecture) + " " + quote(descriptor)
                + "; cd " + quote(extractedRoot) + "; "
                + "if command -v sha256sum >/dev/null 2>&1; then sha256sum -c "
                + quote(SbkRuntimeBundle.CHECKSUM_FILE) + "; else shasum -a 256 -c "
                + quote(SbkRuntimeBundle.CHECKSUM_FILE) + "; fi; cd /; "
                + "if test -e \"$final_dir\"; then "
                + "if test -f " + quote(marker) + " && test \"$(cat " + quote(marker) + ")\" = "
                + quote(expectedContentDigest) + "; then :; "
                + (replaceInvalid ? "else rm -rf \"$final_dir\"; mv " + quote(extractedRoot)
                        + " \"$final_dir\"; fi; "
                        : "else printf 'existing immutable runtime is invalid: %s\\n' \"$final_dir\" >&2; "
                        + "exit 73; fi; ")
                + "else mv " + quote(extractedRoot) + " \"$final_dir\"; fi; printf '%s\\n' "
                + quote(expectedContentDigest) + " > " + quote(marker);
    }

    private static String quote(String value) {
        return RemoteSbkDeployment.shellQuote(value);
    }
}
