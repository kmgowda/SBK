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

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/** Verifies validation of bundled SBK-GEM runtime configuration. */
public final class GemConfigTest {

    /** Ensures the bundled deployment configuration owns the default SCP buffer. */
    @Test
    public void loadsSshCopyBufferDefault() throws IOException {
        assertEquals(GemConfig.DEFAULT_SSH_COPY_BUFFER_BYTES, GemConfig.load().sshCopyBufferBytes);
    }

    /** Ensures the bundled configuration enables bounded automatic transfer sizing. */
    @Test
    public void loadsAutomaticTransferExecutorDefaults() throws IOException {
        final GemConfig config = GemConfig.load();

        assertEquals(0, config.transferExecutorThreads);
        assertEquals(4, config.transferExecutorMinimumThreads);
        assertEquals(64, config.transferExecutorMaximumThreads);
        assertEquals(8, config.transferTargetWaves);
    }

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

    /** Ensures bounded orchestration pools accept auto sizing or a fixed in-range override. */
    @Test
    public void validatesOrchestrationExecutorSizes() {
        final GemConfig config = validConfig();
        config.controlExecutorThreads = 0;

        assertThrows(IllegalArgumentException.class, config::validate);

        config.controlExecutorThreads = 1;
        config.transferExecutorThreads = -1;
        assertThrows(IllegalArgumentException.class, config::validate);

        config.transferExecutorThreads = 0;
        assertDoesNotThrow(config::validate);

        config.transferExecutorThreads = config.transferExecutorMinimumThreads - 1;
        assertThrows(IllegalArgumentException.class, config::validate);

        config.transferExecutorThreads = config.transferExecutorMaximumThreads + 1;
        assertThrows(IllegalArgumentException.class, config::validate);

        config.transferExecutorThreads = config.transferExecutorMaximumThreads;
        assertDoesNotThrow(config::validate);
    }

    /** Ensures automatic transfer sizing has valid bounds and a positive wave target. */
    @Test
    public void validatesAutomaticTransferExecutorConfiguration() {
        final GemConfig config = validConfig();
        config.transferExecutorMinimumThreads = 0;
        assertThrows(IllegalArgumentException.class, config::validate);

        config.transferExecutorMinimumThreads = 4;
        config.transferExecutorMaximumThreads = 3;
        assertThrows(IllegalArgumentException.class, config::validate);

        config.transferExecutorMaximumThreads = 64;
        config.transferTargetWaves = 0;
        assertThrows(IllegalArgumentException.class, config::validate);

        config.transferTargetWaves = 8;
        assertDoesNotThrow(config::validate);
    }

    /** Ensures each SCP stream has a non-empty read buffer. */
    @Test
    public void validatesSshCopyBufferSize() {
        final GemConfig config = validConfig();
        config.sshCopyBufferBytes = 0;

        assertThrows(IllegalArgumentException.class, config::validate);

        config.sshCopyBufferBytes = 1;
        assertDoesNotThrow(config::validate);
    }

    /** Ensures registration and driver operation timeouts are positive and safely convertible. */
    @Test
    public void validatesRegistrationAndDriverTimeouts() {
        final GemConfig config = validConfig();
        config.sbmRegistrationTimeoutSeconds = 0;
        assertThrows(IllegalArgumentException.class, config::validate);

        config.sbmRegistrationTimeoutSeconds = 1;
        config.timeoutSeconds = 0;
        assertThrows(IllegalArgumentException.class, config::validate);

        config.timeoutSeconds = Integer.MAX_VALUE;
        assertThrows(IllegalArgumentException.class, config::validate);

        config.timeoutSeconds = 1;
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
        config.sbmRegistrationTimeoutSeconds = 1;
        config.timeoutSeconds = 1;
        config.controlExecutorThreads = 1;
        config.transferExecutorThreads = 0;
        config.transferExecutorMinimumThreads = 4;
        config.transferExecutorMaximumThreads = 64;
        config.transferTargetWaves = 8;
        config.sshCopyBufferBytes = 1;
        config.diagnosticBytes = 1;
        config.maximumAgentResponseBytes = 1;
        config.diagnosticPrefixCharacters = 1;
        config.maximumDiagnosticCharacters = 2
                + GemConfig.DIAGNOSTIC_TRUNCATION_MARKER.length();
        return config;
    }
}
