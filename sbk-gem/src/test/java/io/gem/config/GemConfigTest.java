/**
 * Copyright (c) KMG. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 */
package io.gem.config;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

/** Verifies validation of bundled SBK-GEM runtime configuration. */
public final class GemConfigTest {

    /** Ensures diagnostic truncation always retains at least one suffix character. */
    @Test
    public void validatesCompleteDiagnosticTruncationBudget() {
        final GemConfig config = validConfig();
        config.maximumDiagnosticCharacters = config.diagnosticPrefixCharacters
                + GemConfig.DIAGNOSTIC_TRUNCATION_MARKER.length();

        assertThrows(IllegalArgumentException.class, config::validate);

        config.maximumDiagnosticCharacters++;
        assertDoesNotThrow(config::validate);
    }

    /** Ensures an unlaunched reservation spans the bounded lock and SSH control intervals. */
    @Test
    public void validatesRuntimeLeaseReservationWindow() {
        final GemConfig config = validConfig();
        config.runtimeLeaseReservationSeconds = config.runtimeManagementLockTimeoutSeconds
                + config.remoteTimeoutSeconds;

        assertThrows(IllegalArgumentException.class, config::validate);

        config.runtimeLeaseReservationSeconds++;
        assertDoesNotThrow(config::validate);
    }

    /** Ensures runtime bundle progress reporting cannot be disabled accidentally. */
    @Test
    public void validatesRuntimeProgressInterval() {
        final GemConfig config = validConfig();
        config.runtimeProgressIntervalSeconds = 0;

        assertThrows(IllegalArgumentException.class, config::validate);

        config.runtimeProgressIntervalSeconds = 1;
        assertDoesNotThrow(config::validate);
    }

    private static GemConfig validConfig() {
        final GemConfig config = new GemConfig();
        config.remoteTimeoutSeconds = 1;
        config.deploymentTimeoutSeconds = 1;
        config.runtimeProgressIntervalSeconds = 1;
        config.runtimeCacheDirectory = ".sbk/cache/sbk-gem";
        config.runtimeManagementLockTimeoutSeconds = 1;
        config.runtimeManagementLockStaleSeconds = 2;
        config.runtimeLeaseReservationSeconds = 3;
        config.executorThreadReserve = 1;
        config.diagnosticBytes = 1;
        config.maximumAgentResponseBytes = 1;
        config.diagnosticPrefixCharacters = 1;
        config.maximumDiagnosticCharacters = 2
                + GemConfig.DIAGNOSTIC_TRUNCATION_MARKER.length();
        return config;
    }
}
