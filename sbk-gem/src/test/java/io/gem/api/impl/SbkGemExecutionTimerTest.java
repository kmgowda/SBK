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

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Verifies whole-invocation SBK-GEM timing independently of benchmark execution.
 */
final class SbkGemExecutionTimerTest {
    @Test
    void reportsOffsetTimestampsAndMonotonicElapsedTime() {
        final ZoneId zone = ZoneId.of("Asia/Kolkata");
        final MutableClock clock = new MutableClock(Instant.parse("2026-08-27T04:30:00Z"), zone);
        final AtomicLong ticker = new AtomicLong(TimeUnit.SECONDS.toNanos(10));
        final SbkGemExecutionTimer timer = new SbkGemExecutionTimer(clock, ticker::get);

        clock.setInstant(Instant.parse("2026-08-27T04:31:05.123Z"));
        ticker.addAndGet(TimeUnit.SECONDS.toNanos(65) + TimeUnit.MILLISECONDS.toNanos(123));
        final SbkGemExecutionTimer.ExecutionSummary summary = timer.finish();

        assertEquals("2026-08-27T10:00:00.000+05:30", timer.startTime());
        assertEquals("2026-08-27T10:01:05.123+05:30", summary.endTime());
        assertEquals("00h 01m 05.123s", summary.totalTime());
    }

    @Test
    void formatsDurationsLongerThanOneDay() {
        final long elapsed = TimeUnit.DAYS.toNanos(2) + TimeUnit.HOURS.toNanos(3)
                + TimeUnit.MINUTES.toNanos(4) + TimeUnit.SECONDS.toNanos(5)
                + TimeUnit.MILLISECONDS.toNanos(6);

        assertEquals("2d 03h 04m 05.006s", SbkGemExecutionTimer.formatDuration(elapsed));
    }

    @Test
    void clampsNegativeTickerMovementToZero() {
        assertEquals("00h 00m 00.000s", SbkGemExecutionTimer.formatDuration(-1L));
    }

    private static final class MutableClock extends Clock {
        private Instant instant;
        private final ZoneId zone;

        private MutableClock(final Instant instant, final ZoneId zone) {
            this.instant = instant;
            this.zone = zone;
        }

        private void setInstant(final Instant value) {
            instant = value;
        }

        @Override
        public ZoneId getZone() {
            return zone;
        }

        @Override
        public Clock withZone(final ZoneId value) {
            return new MutableClock(instant, value);
        }

        @Override
        public Instant instant() {
            return instant;
        }
    }
}
