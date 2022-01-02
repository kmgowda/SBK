/**
 * Copyright (c) KMG. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 */
package io.sbk.perl.impl;

import io.sbk.config.PerlConfig;
import io.sbk.perl.LatencyPercentiles;
import io.sbk.perl.LatencyRecord;
import io.sbk.perl.LatencyRecordWindow;
import io.sbk.perl.LatencyRecorder;
import io.sbk.perl.ReportLatencies;
import io.sbk.system.Printer;
import io.time.Time;
import org.HdrHistogram.Histogram;
import org.HdrHistogram.HistogramIterationValue;

import java.util.Iterator;

public final class HdrExtendedLatencyRecorder extends LatencyRecordWindow {
    final private LatencyRecordWindow latencyBuffer;
    final private HdrLatencyReporter hdrReporter;

    public HdrExtendedLatencyRecorder(long lowLatency, long highLatency, long totalLatencyMax, long totalRecordsMax,
                                      long bytesMax, double[] percentilesFractions, Time time,
                                      LatencyRecordWindow latencyBuffer) {
        super(lowLatency, highLatency, totalLatencyMax, totalRecordsMax, bytesMax, percentilesFractions, time);
        this.latencyBuffer = latencyBuffer;
        this.hdrReporter = new HdrLatencyReporter(this, highLatency, PerlConfig.HDR_SIGNIFICANT_DIGITS);
    }

    private void checkBufferFull() {
        if (latencyBuffer.isFull()) {
            latencyBuffer.copyPercentiles(percentiles, hdrReporter);
            latencyBuffer.reset();
        }
    }

    @Override
    public void reset(long startTime) {
        super.reset(startTime);
        latencyBuffer.reset(startTime);
        hdrReporter.reset();
    }

    @Override
    public void reportLatencyRecord(LatencyRecord record) {
        latencyBuffer.reportLatencyRecord(record);
        checkBufferFull();
    }

    @Override
    public void reportLatency(long latency, long count) {
        latencyBuffer.reportLatency(latency, count);
        checkBufferFull();
    }

    @Override
    public void recordLatency(long startTime, int bytes, int events, long latency) {
        latencyBuffer.recordLatency(startTime, bytes, events, latency);
        checkBufferFull();
    }

    @Override
    public void copyPercentiles(LatencyPercentiles percentiles, ReportLatencies reportLatencies) {
        if (this.totalRecords > 0) {
            Printer.log.info("Percentiles are from HdrHistogram!");
            latencyBuffer.copyPercentiles(percentiles, hdrReporter);
            hdrReporter.copyPercentiles(percentiles, reportLatencies);
        } else {
            // Update the current Window values to print
            super.reset();
            super.update(latencyBuffer);
            latencyBuffer.copyPercentiles(percentiles, reportLatencies);
        }
    }

    @Override
    final public boolean isFull() {
        return super.isOverflow();
    }

    @Override
    final public long getMaxMemoryBytes() {
        return hdrReporter.getMaxMemoryBytes();
    }

    private static class HdrLatencyReporter implements ReportLatencies {
        final private LatencyRecorder recorder;
        final private Histogram histogram;

        public HdrLatencyReporter(LatencyRecorder recorder, long range, int sigDigits) {
            this.recorder = recorder;
            this.histogram = new Histogram(range, sigDigits);
        }

        @Override
        public void reportLatencyRecord(LatencyRecord record) {
            recorder.update(record);
        }

        @Override
        public void reportLatency(long latency, long count) {
            this.histogram.recordValueWithCount(latency, count);
        }

        public void copyPercentiles(LatencyPercentiles percentiles, ReportLatencies reportLatencies) {
            final Iterator<HistogramIterationValue> hValues = this.histogram.recordedValues().iterator();

            if (reportLatencies != null) {
                while (hValues.hasNext()) {
                    final HistogramIterationValue val = hValues.next();
                    final long latency = val.getValueIteratedTo();
                    final long count = val.getCountAtValueIteratedTo();
                    reportLatencies.reportLatency(latency, count);
                }
                reportLatencies.reportLatencyRecord(this.recorder);
            }

            percentiles.reset(this.recorder.getValidLatencyRecords());
            for (int i = 0; i < percentiles.fractions.length; i++) {
                percentiles.latencies[i] = this.histogram.getValueAtPercentile(percentiles.fractions[i] * 100.0);
            }
            percentiles.medianLatency = this.histogram.getValueAtPercentile(50);
        }

        public long getMaxMemoryBytes() {
            return histogram.getEstimatedFootprintInBytes();
        }

        public void reset() {
            this.histogram.reset();
        }
    }
}