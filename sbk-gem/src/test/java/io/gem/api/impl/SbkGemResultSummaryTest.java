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

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

/** Verifies the aligned, multiline distributed benchmark result summary. */
final class SbkGemResultSummaryTest {
    @Test
    void formatsSuccessfulDistributedResultAsAlignedLines() {
        assertEquals(List.of(
                "SBK-GEM Distributed Benchmark Status : SUCCESS",
                "SBK-GEM Expected Nodes               : 4",
                "SBK-GEM Successful Nodes             : 4",
                "SBK-GEM Failed Nodes                 : 0",
                "SBK-GEM Maximum SBM Registrations    : 4/4"),
                SbkGem.distributedResultSummary("SUCCESS", 4, 4, 0, 4));
    }

    @Test
    void identifiesUnavailableSbmRegistrationCount() {
        assertEquals("SBK-GEM Maximum SBM Registrations    : unavailable",
                SbkGem.distributedResultSummary("FAILED", 2, 0, 2, -1).get(4));
    }
}
