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
import io.sbk.config.ExitCode;

import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Operating-system and processor architecture of an SBK-GEM deployment.
 *
 * <p>SBK-GEM intentionally deploys one native Java runtime to a homogeneous
 * cluster. It therefore rejects a remote operating system or architecture
 * which differs from the controller before transferring the runtime bundle.
 *
 * @param operatingSystem normalized operating system
 * @param architecture normalized processor architecture
 */
record DeploymentPlatform(String operatingSystem, String architecture) {
    private static final Pattern OPERATING_SYSTEM_PATTERN = Pattern.compile("(?m)^SBK_OS=(\\S+)$");
    private static final Pattern ARCHITECTURE_PATTERN = Pattern.compile("(?m)^SBK_ARCH=(\\S+)$");

    /**
     * Detect the controller platform.
     *
     * @return normalized controller platform
     * @throws IllegalArgumentException when the controller platform is unsupported
     */
    static DeploymentPlatform local() {
        return new DeploymentPlatform(normalizeOperatingSystem(System.getProperty("os.name")),
                normalizeArchitecture(System.getProperty("os.arch")));
    }

    /**
     * Build the remote platform and deployment-tool preflight command.
     *
     * @return POSIX-shell command
     */
    static String probeCommand() {
        return "command -v tar >/dev/null 2>&1 || { printf '%s\\n' 'tar command is required' >&2; exit 127; }; "
                + "if command -v sha256sum >/dev/null 2>&1; then SBK_SHA256=sha256sum; "
                + "elif command -v shasum >/dev/null 2>&1; then SBK_SHA256=shasum; "
                + "else printf '%s\\n' 'sha256sum or shasum is required' >&2; exit 127; fi; "
                + "printf 'SBK_OS=%s\\nSBK_ARCH=%s\\nSBK_SHA256=%s\\n' "
                + "\"$(uname -s)\" \"$(uname -m)\" \"$SBK_SHA256\"";
    }

    /**
     * Parse a successful remote platform probe.
     *
     * @param response SSH response
     * @return normalized remote platform, or {@code null} for an invalid probe
     */
    static DeploymentPlatform fromProbe(SshResponse response) {
        if (response == null || response.returnCode != ExitCode.SUCCESS) {
            return null;
        }
        final String output = response.stdOutputStream.toString();
        final Matcher osMatcher = OPERATING_SYSTEM_PATTERN.matcher(output);
        final Matcher archMatcher = ARCHITECTURE_PATTERN.matcher(output);
        if (!osMatcher.find() || !archMatcher.find()) {
            return null;
        }
        try {
            return new DeploymentPlatform(normalizeOperatingSystem(osMatcher.group(1)),
                    normalizeArchitecture(archMatcher.group(1)));
        } catch (IllegalArgumentException ignored) {
            return null;
        }
    }

    /**
     * Return the stable platform identifier used in runtime bundle names.
     *
     * @return operating-system and architecture identifier
     */
    String id() {
        return operatingSystem + "-" + architecture;
    }

    private static String normalizeOperatingSystem(String value) {
        if (value == null) {
            throw new IllegalArgumentException("Missing operating-system name");
        }
        final String normalized = value.trim().toLowerCase(Locale.ROOT);
        if (normalized.equals("linux")) {
            return "linux";
        }
        if (normalized.equals("darwin") || normalized.startsWith("mac")) {
            return "macos";
        }
        throw new IllegalArgumentException("Unsupported SBK-GEM operating system: " + value);
    }

    private static String normalizeArchitecture(String value) {
        if (value == null) {
            throw new IllegalArgumentException("Missing processor architecture");
        }
        return switch (value.trim().toLowerCase(Locale.ROOT)) {
            case "amd64", "x86_64" -> "amd64";
            case "aarch64", "arm64" -> "arm64";
            default -> throw new IllegalArgumentException("Unsupported SBK-GEM processor architecture: " + value);
        };
    }
}
