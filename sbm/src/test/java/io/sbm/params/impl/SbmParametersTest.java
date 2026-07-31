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

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/** Tests parsing of storage identity used by SBM metrics. */
final class SbmParametersTest {

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
}
