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

import io.micrometer.core.instrument.Meter;
import io.sbk.action.Action;
import io.sbk.config.Config;
import io.sbk.logger.MetricsConfig;
import io.time.MilliSeconds;
import io.time.TimeUnit;
import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

/** Verifies the stable common tags exported by direct SBK metrics. */
final class SbkPrometheusServerTest {

    @Test
    void identifiesDirectSbkAsTheMetricsComponent() throws IOException {
        final MetricsConfig config = metricsConfig();
        final SbkPrometheusServer server = new SbkPrometheusServer(Config.NAME, Action.Reading.name(),
                "ConcurrentQ", new double[]{50.0}, new MilliSeconds(), config);
        try {
            assertFalse(server.registry.getMeters().isEmpty());
            for (Meter meter : server.registry.getMeters()) {
                assertEquals(Config.NAME, meter.getId().getTag(SbkPrometheusServer.COMPONENT_TAG));
                assertEquals("ConcurrentQ", meter.getId().getTag(Config.CLASS_OPTION));
                assertEquals(Action.Reading.name(), meter.getId().getTag("action"));
            }
        } finally {
            server.stop();
        }
    }

    private static MetricsConfig metricsConfig() {
        final MetricsConfig config = new MetricsConfig();
        config.port = 0;
        config.context = "/metrics";
        config.latencyTimeUnit = TimeUnit.ms;
        return config;
    }
}
