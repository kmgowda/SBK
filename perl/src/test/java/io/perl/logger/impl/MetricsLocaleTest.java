/**
 * Copyright (c) KMG. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 */
package io.perl.logger.impl;

import io.micrometer.core.instrument.composite.CompositeMeterRegistry;
import io.time.NanoSeconds;
import io.time.TimeUnit;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.ResourceLock;

import java.util.Locale;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;

/**
 * Verifies that percentile metric names are independent of the process default locale.
 */
final class MetricsLocaleTest {
    @Test
    @ResourceLock("default-locale")
    void commaDecimalLocaleUsesStablePercentileMetricNames() {
        final Locale originalLocale = Locale.getDefault();
        try {
            Locale.setDefault(Locale.GERMANY);
            final PrintMetrics metrics = new PrintMetrics("SBK", new double[]{92.5, 99.99},
                    new NanoSeconds(), TimeUnit.ns, new CompositeMeterRegistry());
            try {
                assertArrayEquals(new String[]{"SBK_ns_92.5", "SBK_ns_99.99"},
                        metrics.percentileLatencyNames);
                assertArrayEquals(new String[]{"SBK_Count_92.5", "SBK_Count_99.99"},
                        metrics.percentileLatencyCountNames);
            } finally {
                metrics.close();
            }
        } finally {
            Locale.setDefault(originalLocale);
        }
    }
}
