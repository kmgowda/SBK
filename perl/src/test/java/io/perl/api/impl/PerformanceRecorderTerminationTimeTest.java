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

import io.perl.api.Channel;
import io.perl.api.PeriodicRecorder;
import io.perl.api.PerlChannel;
import io.perl.api.TimeStamp;
import io.time.MilliSeconds;
import org.junit.jupiter.api.Test;

import java.util.ArrayDeque;
import java.util.Deque;

import static org.junit.jupiter.api.Assertions.assertEquals;

/** Verifies that lifecycle markers do not extend benchmark measurement time. */
final class PerformanceRecorderTerminationTimeTest {
    private static final long MEASUREMENT_START = 1_000L;
    private static final long MEASUREMENT_END = 2_000L;
    private static final long LATE_END_MARKER = 5_000L;
    private static final int REPORTING_INTERVAL_MS = 10_000;
    private static final int IDLE_TIMEOUT_SECONDS = 20;

    @Test
    void elasticWaitUsesLastMeasurementTimeForTotal() {
        final CapturingPeriodicRecorder periodicRecorder = new CapturingPeriodicRecorder();
        final Channel channel = measurementThenLateEndMarker();
        final PerformanceRecorderElasticWait recorder = new PerformanceRecorderElasticWait(
                periodicRecorder, new Channel[]{channel}, new MilliSeconds(),
                REPORTING_INTERVAL_MS, 1, IDLE_TIMEOUT_SECONDS);

        recorder.run(0, 0);

        assertEquals(MEASUREMENT_END, periodicRecorder.stopTime);
    }

    @Test
    void idleSleepUsesLastMeasurementTimeForTotal() {
        final CapturingPeriodicRecorder periodicRecorder = new CapturingPeriodicRecorder();
        final Channel channel = measurementThenLateEndMarker();
        final PerformanceRecorderIdleSleep recorder = new PerformanceRecorderIdleSleep(
                periodicRecorder, new Channel[]{channel}, new MilliSeconds(),
                REPORTING_INTERVAL_MS, 1, IDLE_TIMEOUT_SECONDS);

        recorder.run(0, 0);

        assertEquals(MEASUREMENT_END, periodicRecorder.stopTime);
    }

    private static Channel measurementThenLateEndMarker() {
        return new SequenceChannel(
                new TimeStamp(MEASUREMENT_START, MEASUREMENT_END, 1, 1),
                new TimeStamp(LATE_END_MARKER));
    }

    private static final class SequenceChannel implements Channel {
        private final Deque<TimeStamp> timestamps = new ArrayDeque<>();

        private SequenceChannel(TimeStamp... timestamps) {
            for (TimeStamp timestamp : timestamps) {
                this.timestamps.addLast(timestamp);
            }
        }

        @Override
        public TimeStamp receive(int timeout) {
            return timestamps.pollFirst();
        }

        @Override
        public void sendEndTime(long endTime) {
            timestamps.addLast(new TimeStamp(endTime));
        }

        @Override
        public boolean isEmpty() {
            return timestamps.isEmpty();
        }

        @Override
        public void clear() {
            timestamps.clear();
        }

        @Override
        public PerlChannel getPerlChannel() {
            throw new UnsupportedOperationException("producer access is not used by this test");
        }
    }

    private static final class CapturingPeriodicRecorder implements PeriodicRecorder {
        private long stopTime = Long.MIN_VALUE;

        @Override
        public void record(long startTime, long endTime, int events, int bytes) {
            // The test only verifies the measurement-time boundary.
        }

        @Override
        public void startWindow(long startTime) {
            // No window storage is needed for this boundary test.
        }

        @Override
        public long elapsedMilliSecondsWindow(long currentTime) {
            return 0;
        }

        @Override
        public void stopWindow(long stopTime) {
            // No periodic window is emitted by this boundary test.
        }

        @Override
        public void start(long startTime) {
            // No total storage is needed for this boundary test.
        }

        @Override
        public void stop(long endTime) {
            stopTime = endTime;
        }
    }
}
