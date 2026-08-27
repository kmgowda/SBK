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

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.concurrent.TimeUnit;
import java.util.function.LongSupplier;

/**
 * Measures and formats the complete SBK-GEM invocation lifetime.
 *
 * <p>Wall-clock instants are retained for operator-facing start and end times,
 * while a monotonic ticker supplies the elapsed duration.</p>
 */
final class SbkGemExecutionTimer {
    private static final String TIMESTAMP_PATTERN = "uuuu-MM-dd'T'HH:mm:ss.SSSXXX";
    private static final DateTimeFormatter TIMESTAMP_FORMAT =
            DateTimeFormatter.ofPattern(TIMESTAMP_PATTERN, Locale.ROOT);
    private static final long MILLIS_PER_SECOND = TimeUnit.SECONDS.toMillis(1);
    private static final long SECONDS_PER_MINUTE = TimeUnit.MINUTES.toSeconds(1);
    private static final long MINUTES_PER_HOUR = TimeUnit.HOURS.toMinutes(1);
    private static final long HOURS_PER_DAY = TimeUnit.DAYS.toHours(1);

    private final Clock clock;
    private final LongSupplier monotonicTicker;
    private final Instant startedAt;
    private final long startedNanos;

    /**
     * Create a timer with controllable clocks for deterministic verification.
     *
     * @param clock wall clock
     * @param monotonicTicker monotonic nanosecond ticker
     */
    SbkGemExecutionTimer(final Clock clock, final LongSupplier monotonicTicker) {
        this.clock = clock;
        this.monotonicTicker = monotonicTicker;
        startedAt = clock.instant();
        startedNanos = monotonicTicker.getAsLong();
    }

    /**
     * Start measuring an invocation with the system clocks.
     *
     * @return a newly started timer
     */
    static SbkGemExecutionTimer start() {
        return new SbkGemExecutionTimer(Clock.systemDefaultZone(), System::nanoTime);
    }

    /**
     * Format the invocation start time with its UTC offset.
     *
     * @return formatted start time
     */
    String startTime() {
        return formatTimestamp(startedAt, clock.getZone());
    }

    /**
     * Finish measuring the invocation.
     *
     * @return immutable end-time and elapsed-time summary
     */
    ExecutionSummary finish() {
        final Instant finishedAt = clock.instant();
        final long elapsedNanos = Math.max(0L, monotonicTicker.getAsLong() - startedNanos);
        return new ExecutionSummary(formatTimestamp(finishedAt, clock.getZone()), formatDuration(elapsedNanos));
    }

    private static String formatTimestamp(final Instant timestamp, final ZoneId zone) {
        return TIMESTAMP_FORMAT.format(timestamp.atZone(zone));
    }

    static String formatDuration(final long elapsedNanos) {
        final long elapsedMillis = TimeUnit.NANOSECONDS.toMillis(Math.max(0L, elapsedNanos));
        final long totalSeconds = elapsedMillis / MILLIS_PER_SECOND;
        final long days = totalSeconds / TimeUnit.DAYS.toSeconds(1);
        final long hours = totalSeconds / TimeUnit.HOURS.toSeconds(1) % HOURS_PER_DAY;
        final long minutes = totalSeconds / SECONDS_PER_MINUTE % MINUTES_PER_HOUR;
        final long seconds = totalSeconds % SECONDS_PER_MINUTE;
        final long milliseconds = elapsedMillis % MILLIS_PER_SECOND;
        final String dayPrefix = days == 0L ? "" : days + "d ";
        return String.format(Locale.ROOT, "%s%02dh %02dm %02d.%03ds",
                dayPrefix, hours, minutes, seconds, milliseconds);
    }

    /**
     * Completed execution timing values.
     *
     * @param endTime formatted wall-clock end time
     * @param totalTime formatted monotonic elapsed duration
     */
    record ExecutionSummary(String endTime, String totalTime) {
    }
}
