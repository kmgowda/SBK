/**
 * Copyright (c) KMG. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 */
package io.sbk.api.impl;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

/** Tests rate-controller shutdown behavior. */
final class SbkRateControllerTest {

    @Test
    void preservesInterruptWhenRateControlSleepIsCancelled() {
        final SbkRateController controller = new SbkRateController();
        controller.start(1);
        Thread.currentThread().interrupt();

        try {
            controller.control(1, 1.0);
            assertTrue(Thread.currentThread().isInterrupted());
        } finally {
            Thread.interrupted();
        }
    }
}
