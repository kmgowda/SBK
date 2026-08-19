/**
 * Copyright (c) KMG. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 */
package io.sbk.logger.impl;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Verifies which SBK loggers may perform work for the PerL individual-latency callback.
 */
final class SystemLoggerLatencyCallbackTest {
    private static final Class<?>[] SYSTEM_LOGGER_FAMILY = {
            SystemLogger.class,
            Sl4jLogger.class,
            CSVLogger.class,
            PrometheusLogger.class,
            WebLogger.class
    };

    @Test
    void systemLoggerFamilyUsesFinalNoOpLatencyCallback() throws NoSuchMethodException {
        final Method callback = latencyCallback(SystemLogger.class);
        assertTrue(Modifier.isFinal(callback.getModifiers()));
        for (Class<?> loggerClass : SYSTEM_LOGGER_FAMILY) {
            assertEquals(SystemLogger.class, latencyCallback(loggerClass).getDeclaringClass(),
                    loggerClass.getSimpleName());
        }
    }

    @Test
    void grpcLoggerRetainsDedicatedLatencyCallback() throws NoSuchMethodException {
        assertEquals(GrpcLogger.class, latencyCallback(GrpcLogger.class).getDeclaringClass());
    }

    private static Method latencyCallback(Class<?> loggerClass) throws NoSuchMethodException {
        return loggerClass.getMethod("recordLatency", long.class, int.class, int.class, long.class);
    }
}
