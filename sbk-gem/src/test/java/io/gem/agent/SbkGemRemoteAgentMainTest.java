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

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Tests remote Java compatibility with the controller minimum. */
final class SbkGemRemoteAgentMainTest {
    @Test
    void acceptsMatchingOrNewerJavaAndRejectsOlderJava() {
        assertTrue(SbkGemRemoteAgentMain.isJavaCompatible(25, 25));
        assertTrue(SbkGemRemoteAgentMain.isJavaCompatible(26, 25));
        assertFalse(SbkGemRemoteAgentMain.isJavaCompatible(24, 25));
    }
}
