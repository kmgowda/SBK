/**
 * Copyright (c) KMG. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 */
package io.sbm.params.impl;

import io.sbk.action.Action;
import io.sbm.config.SbmConfig;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Tests parsing of storage identity used by SBM metrics. */
final class SbmParametersTest {

    @Test
    void loadsIndependentValidatedConfigurations() {
        final SbmConfig shared = SbmConfig.get();
        final SbmConfig first = SbmConfig.load();
        final SbmConfig second = SbmConfig.load();
        final int configuredMaximum = shared.maxConnections;

        assertNotSame(shared, first);
        assertNotSame(first, second);
        first.maxConnections = configuredMaximum + 1;

        assertEquals(configuredMaximum, shared.maxConnections);
        assertEquals(configuredMaximum, second.maxConnections);
    }

    @Test
    void usesConfiguredDefaultAction() throws Exception {
        final SbmParameters parameters = new SbmParameters("test", 9717, 1, 0, null);

        parameters.parseArgs(new String[]{"-class", "File"});

        assertEquals("r", SbmConfig.get().defaultAction);
        assertEquals(Action.Reading, parameters.getAction());
    }

    @Test
    void preservesCanonicalStorageClassName() throws Exception {
        final SbmParameters parameters = new SbmParameters("test", 9717, 1, 0, null);

        parameters.parseArgs(new String[]{"-class", "FdbRecord", "-action", "r"});

        assertEquals("FdbRecord", parameters.getStorageName());
    }

    @Test
    void doesNotRewriteStorageClassAcronyms() throws Exception {
        final SbmParameters parameters = new SbmParameters("test", 9717, 1, 0, null);

        parameters.parseArgs(new String[]{"-class", "MinIO", "-action", "w"});

        assertEquals("MinIO", parameters.getStorageName());
    }

    @Test
    void parsesIdleTimeoutAndPreservesItsDefault() throws Exception {
        final SbmParameters defaults = new SbmParameters("test", 9717, 1, 0, null);
        defaults.parseArgs(new String[]{"-class", "File"});
        assertEquals(600, defaults.getIdleTimeoutSeconds());
        assertFalse(defaults.isFixedRecordMode());

        final SbmParameters override = new SbmParameters("test", 9717, 1, 0, null);
        override.parseArgs(new String[]{"-class", "File", "-records", "100", "-idletimeoutseconds", "23"});
        assertEquals(23, override.getIdleTimeoutSeconds());
        assertTrue(override.isFixedRecordMode());

        final SbmParameters invalid = new SbmParameters("test", 9717, 1, 0, null);
        assertThrows(IllegalArgumentException.class,
                () -> invalid.parseArgs(new String[]{"-class", "File", "-records", "0"}));
    }
}
