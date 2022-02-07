/**
 * Copyright (c) KMG. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.perl.logger;

import io.perl.api.Bytes;
import org.jetbrains.annotations.NotNull;

public interface ResultsFormat {

    default void appendResults(@NotNull StringBuilder out, String timeUnitName, String[] percentileNames,
                               long seconds, long bytes, long records, double recsPerSec, double mbPerSec,
                               double avgLatency, long maxLatency, long invalid, long lowerDiscard,
                               long higherDiscard, long slc1, long slc2, @NotNull long[] percentileValues) {
        final double mBytes = (bytes * 1.0) / Bytes.BYTES_PER_MB;
        out.append(String.format("%8d seconds, %11.1f MB, %16d records, %11.1f records/sec, %8.2f MB/sec"
                        + ", %8.1f %s avg latency, %7d %s max latency;"
                        + " %8d invalid latencies; Discarded Latencies:%8d lower, %8d higher;"
                        + " SLC-1: %3d, SLC-2: %3d;",
                seconds, mBytes, records, recsPerSec, mbPerSec, avgLatency, timeUnitName, maxLatency,
                timeUnitName, invalid, lowerDiscard, higherDiscard, slc1, slc2));
        out.append(" Latency Percentiles: ");

        for (int i = 0; i < Math.min(percentileNames.length, percentileValues.length); i++) {
            if (i == 0) {
                out.append(String.format("%7d %s %sth", percentileValues[i], timeUnitName, percentileNames[i]));
            } else {
                out.append(String.format(", %7d %s %sth", percentileValues[i], timeUnitName, percentileNames[i]));
            }
        }
    }
}
