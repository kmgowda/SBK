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

import io.time.Time;
import javax.annotation.concurrent.NotThreadSafe;
import java.util.concurrent.locks.LockSupport;
import java.util.function.LongConsumer;

/**
 * Adapts the number of idle parks between wall-clock checks.
 *
 * <p>The first clock check is deliberately made after one park. Subsequent
 * checks use the observed number of completed parks per millisecond instead
 * of assuming that {@link LockSupport#parkNanos(long)} sleeps for exactly the
 * requested duration. This makes the calibration independent of operating
 * system timer granularity, scheduler behavior, CPU speed, and virtual versus
 * platform thread execution.
 *
 * <p>This class is owned exclusively by the single PerL consumer thread.
 */
@NotThreadSafe
public final class ElasticWait {
    private static final double CALIBRATION_WEIGHT = 0.25;
    private static final int BACKOFF_MULTIPLIER = 2;

    private final int windowIntervalMS;
    private final int idleNS;
    private final long maximumCalibrationCount;
    private final LongConsumer idleStrategy;
    private double waitsPerMillisecond;
    private long elasticCount;
    private long idleCount;
    private long previousElapsedIntervalMS;
    private boolean calibrated;

    /**
     * Constructor ElasticWait initialize all values.
     *
     * @param idleNS             requested idle park duration in nanoseconds
     * @param windowIntervalMS   reporting-window duration in milliseconds
     * @param minIntervalMS      maximum bootstrap calibration interval in milliseconds
     * @throws IllegalArgumentException if any argument is zero or negative
     */
    public ElasticWait(int idleNS, int windowIntervalMS, int minIntervalMS) {
        this(idleNS, windowIntervalMS, minIntervalMS, LockSupport::parkNanos);
    }

    ElasticWait(int idleNS, int windowIntervalMS, int minIntervalMS, LongConsumer idleStrategy) {
        if (idleNS <= 0 || windowIntervalMS <= 0 || minIntervalMS <= 0) {
            throw new IllegalArgumentException("ElasticWait intervals must be greater than zero");
        }
        if (idleStrategy == null) {
            throw new IllegalArgumentException("ElasticWait idle strategy must not be null");
        }
        this.windowIntervalMS = windowIntervalMS;
        this.idleNS = idleNS;
        this.idleStrategy = idleStrategy;
        waitsPerMillisecond = (Time.NS_PER_MS * 1.0) / this.idleNS;
        maximumCalibrationCount = waitCount(waitsPerMillisecond, minIntervalMS);
        elasticCount = 1;
        idleCount = 0;
        previousElapsedIntervalMS = 0;
        calibrated = false;
    }

    /**
     * Starts a new reporting window while retaining the measured park rate.
     */
    public void reset() {
        idleCount = 0;
        previousElapsedIntervalMS = 0;
        elasticCount = calibrated ? waitCount(waitsPerMillisecond, windowIntervalMS) : 1;
    }

    /**
     * Parks once and checks whether the next wall-clock sample is due.
     *
     * @return true when the calibrated number of parks has completed
     */
    public boolean waitAndCheck() {
        idleStrategy.accept(idleNS);
        idleCount++;
        return idleCount >= elasticCount;
    }

    /**
     * Starts a fresh idle calibration sample after data was consumed.
     *
     * <p>The established moving-average park rate is retained, but parks from
     * the earlier idle period are discarded so active processing time is not
     * included in the next observed park rate. The elapsed value comes from
     * the timestamp already consumed by the recorder, avoiding another
     * wall-clock read.</p>
     *
     * @param elapsedIntervalMS elapsed milliseconds in the current window
     */
    void startIdle(long elapsedIntervalMS) {
        final long elapsed = Math.max(
                previousElapsedIntervalMS, Math.max(0, elapsedIntervalMS));
        previousElapsedIntervalMS = elapsed;
        idleCount = 0;
        if (calibrated) {
            final long remainingIntervalMS =
                    Math.max(1, windowIntervalMS - elapsed);
            elasticCount = waitCount(
                    waitsPerMillisecond, remainingIntervalMS);
        } else {
            elasticCount = 1;
        }
    }

    /**
     * Updates calibration after a clock check within the current window.
     *
     * <p>The completed batch is always cleared. If the clock has not advanced
     * far enough to measure a rate, the bootstrap batch grows exponentially
     * up to the configured calibration interval. Otherwise the next clock
     * check is scheduled for the remaining part of the reporting window.
     *
     * @param elapsedIntervalMS elapsed milliseconds in the current window
     */
    public void updateElastic(long elapsedIntervalMS) {
        final long elapsed = Math.max(0, elapsedIntervalMS);
        final long elapsedSinceLastCheck = elapsed - previousElapsedIntervalMS;
        if (elapsedSinceLastCheck > 0) {
            updateRate(elapsedSinceLastCheck);
        }
        previousElapsedIntervalMS = elapsed;
        idleCount = 0;
        if (calibrated) {
            final long remainingIntervalMS = Math.max(1, windowIntervalMS - elapsed);
            elasticCount = waitCount(waitsPerMillisecond, remainingIntervalMS);
        } else {
            elasticCount = Math.min(
                    saturatingMultiplyByTwo(elasticCount),
                    maximumCalibrationCount);
        }
    }

    /**
     * Completes calibration for an expired window and starts the next window.
     *
     * @param currentIntervalMS elapsed milliseconds in the expired window
     */
    public void setElastic(long currentIntervalMS) {
        final long elapsedSinceLastCheck = currentIntervalMS - previousElapsedIntervalMS;
        if (elapsedSinceLastCheck > 0) {
            updateRate(elapsedSinceLastCheck);
        }
        reset();
    }

    private void updateRate(long elapsedIntervalMS) {
        if (idleCount <= 0) {
            return;
        }
        final double observedRate = idleCount / (double) elapsedIntervalMS;
        if (observedRate <= 0 || !Double.isFinite(observedRate)) {
            return;
        }
        waitsPerMillisecond = calibrated
                ? waitsPerMillisecond * (1.0 - CALIBRATION_WEIGHT)
                    + observedRate * CALIBRATION_WEIGHT
                : observedRate;
        calibrated = true;
    }

    private static long waitCount(double rate, long intervalMS) {
        final double count = Math.ceil(rate * intervalMS);
        if (!Double.isFinite(count) || count >= Long.MAX_VALUE) {
            return Long.MAX_VALUE;
        }
        return Math.max(1, (long) count);
    }

    private static long saturatingMultiplyByTwo(long value) {
        return value > Long.MAX_VALUE / BACKOFF_MULTIPLIER
                ? Long.MAX_VALUE : value * BACKOFF_MULTIPLIER;
    }
}
