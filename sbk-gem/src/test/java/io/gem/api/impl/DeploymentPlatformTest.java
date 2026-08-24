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

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/** Tests Java-agent operating-system normalization. */
final class DeploymentPlatformTest {
    @Test
    void normalizesAgentOperatingSystemValues() {
        assertEquals(new DeploymentPlatform("linux"), DeploymentPlatform.fromOperatingSystem("Linux"));
        assertEquals(new DeploymentPlatform("macos"), DeploymentPlatform.fromOperatingSystem("Darwin"));
        assertThrows(IllegalArgumentException.class,
                () -> DeploymentPlatform.fromOperatingSystem("Windows"));
    }
}
