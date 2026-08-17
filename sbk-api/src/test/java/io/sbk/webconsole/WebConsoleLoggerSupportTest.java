/**
 * Copyright (c) KMG. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 */
package io.sbk.webconsole;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/** Tests Local Web Console metadata defaults shared by the logger adapters. */
final class WebConsoleLoggerSupportTest {

    @Test
    void generatesApplicationAndStorageBoardNamesWhenNoNameIsConfigured() {
        assertEquals("SBK File", WebConsoleLoggerSupport.resolveBoardName("", "SBK", "File"));
        assertEquals("SBM MinIO", WebConsoleLoggerSupport.resolveBoardName(null, "SBM", "MinIO"));
        assertEquals("SBK-GEM Kafka", WebConsoleLoggerSupport.resolveBoardName("   ", "SBK-GEM", "Kafka"));
    }

    @Test
    void preservesExplicitBoardName() {
        assertEquals("Nightly MinIO", WebConsoleLoggerSupport.resolveBoardName(
                "Nightly MinIO", "SBK", "MinIO"));
    }
}
