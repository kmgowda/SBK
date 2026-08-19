/**
 * Copyright (c) KMG. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 */
package io.sbm.logger.impl;

import io.sbk.logger.impl.SystemLogger;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Verifies that SBM loggers retain the final no-op individual-latency callback.
 */
final class SbmLoggerLatencyCallbackTest {

    @Test
    void sbmLoggersUseSystemLoggerLatencyCallback() throws NoSuchMethodException {
        assertEquals(SystemLogger.class, latencyCallback(SbmPrometheusLogger.class).getDeclaringClass());
        assertEquals(SystemLogger.class, latencyCallback(SbmWebLogger.class).getDeclaringClass());
    }

    private static Method latencyCallback(Class<?> loggerClass) throws NoSuchMethodException {
        return loggerClass.getMethod("recordLatency", long.class, int.class, int.class, long.class);
    }
}
