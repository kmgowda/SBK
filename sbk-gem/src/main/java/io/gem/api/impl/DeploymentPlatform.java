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

import java.util.Locale;

/**
 * Operating system of an SBK-GEM deployment.
 *
 * <p>SBK-GEM requires the controller and deployment targets to use the same
 * supported operating system. Processor architecture is intentionally not
 * part of deployment compatibility, which also permits translated runtimes
 * and containers whose reported architecture differs from their host.
 *
 * @param operatingSystem normalized operating system
 */
record DeploymentPlatform(String operatingSystem) {
    /**
     * Detect the controller platform.
     *
     * @return normalized controller platform
     * @throws IllegalArgumentException when the controller platform is unsupported
     */
    static DeploymentPlatform local() {
        return new DeploymentPlatform(normalizeOperatingSystem(System.getProperty("os.name")));
    }

    static DeploymentPlatform fromOperatingSystem(String value) {
        return new DeploymentPlatform(normalizeOperatingSystem(value));
    }

    /**
     * Return the stable platform identifier used in runtime bundle names.
     *
     * @return operating-system identifier
     */
    String id() {
        return operatingSystem;
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

}
