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

import static org.junit.jupiter.api.Assertions.assertInstanceOf;

/** Verifies that each full-copy setting selects its isolated artifact policy. */
final class RuntimeCopyPolicyTest {

    @Test
    void selectsMinimalPolicyByDefaultValue() {
        assertInstanceOf(MinimalRuntimeCopyPolicy.class, RuntimeCopyPolicy.select(false));
    }

    @Test
    void selectsFullPolicyWhenRequested() {
        assertInstanceOf(FullRuntimeCopyPolicy.class, RuntimeCopyPolicy.select(true));
    }
}
