/**
 * Copyright (c) KMG. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.perl.api.impl;

import io.perl.api.PeriodicRecorder;
import io.perl.config.PerlConfig;
import io.perl.logger.impl.DefaultLogger;
import io.perl.logger.impl.ResultsLogger;
import io.time.MilliSeconds;
import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;

/** Verifies startup specialization of PerL's array periodic recorders. */
public class PerlBuilderRecorderTest {

    /**
     * Selects the callback-free array recorder for the default logger.
     *
     * @throws IOException if the PerL configuration cannot be loaded
     */
    @Test
    public void selectsCallbackFreeArrayRecorder() throws IOException {
        final PeriodicRecorder recorder = PerlBuilder.buildPeriodicLogger(
                new MilliSeconds(), PerlConfig.build(), new DefaultLogger());

        assertInstanceOf(ArrayWindowPeriodicRecorder.class, recorder);
    }

    /**
     * Selects the callback-preserving array recorder for custom loggers.
     *
     * @throws IOException if the PerL configuration cannot be loaded
     */
    @Test
    public void selectsCallbackArrayRecorder() throws IOException {
        final ResultsLogger logger = new ResultsLogger() { };
        final PeriodicRecorder recorder = PerlBuilder.buildPeriodicLogger(
                new MilliSeconds(), PerlConfig.build(), logger);

        assertInstanceOf(ArrayWindowLatencyPeriodicRecorder.class, recorder);
    }
}
