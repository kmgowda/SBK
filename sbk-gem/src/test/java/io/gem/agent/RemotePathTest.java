/**
 * Copyright (c) KMG. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 */
package io.gem.agent;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Verifies remote POSIX paths independently of the controller file-system separator. */
public final class RemotePathTest {

    /** Joins nested remote paths without duplicating the root separator. */
    @Test
    public void joinsRemotePathSegments() {
        assertEquals("/srv/sbk/runtime/bin/java", RemotePath.join("/srv/sbk/", "runtime", "bin/java"));
        assertEquals("/runtime", RemotePath.join(RemotePath.ROOT, "runtime"));
    }

    /** Rejects an absolute child because joining it would discard the remote parent. */
    @Test
    public void rejectsAbsoluteChildPath() {
        assertThrows(IllegalArgumentException.class, () -> RemotePath.join("/srv/sbk", "/runtime"));
    }

    /** Normalizes trailing separators and returns remote parents with root fallback. */
    @Test
    public void normalizesAndFindsRemoteParent() {
        assertEquals("/srv/sbk", RemotePath.normalize(" /srv/sbk/// "));
        assertNull(RemotePath.normalize(" "));
        assertEquals("/srv", RemotePath.parent("/srv/sbk/"));
        assertEquals(RemotePath.ROOT, RemotePath.parent("/srv"));
    }

    /** Detects absolute paths using the supported remote POSIX contract. */
    @Test
    public void detectsAbsoluteRemotePaths() {
        assertTrue(RemotePath.isAbsolute("/srv/sbk"));
        assertFalse(RemotePath.isAbsolute("srv/sbk"));
    }
}
