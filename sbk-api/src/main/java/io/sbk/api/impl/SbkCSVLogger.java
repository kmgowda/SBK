/**
 * Copyright (c) KMG. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 */

package io.sbk.api.impl;

import java.io.PrintWriter;
import java.io.FileWriter;

/**
 * Class for recoding/printing results to a CSV file called `out.csv`.
 */
public class SbkCSVLogger extends SystemLogger {
    public String fileName = "out.csv";

    public SbkCSVLogger() {
        super();
        String header = "Prefix, Writers, Readers, maxWriters, maxReaders, Bytes, Records, RecsPerSec, mbPerSec, AvgLatency, maxLatency, InvalidLatencies, lowerDiscard, higherDiscard, 10thPercentile, 25thPercentile, 50thPercentile, 75thPercentile, 90thPercentile, 95thPercentile, 99thPercentile, 99.9thPercentile, 99.99thPercentile\n";
        try {
            PrintWriter pw = new PrintWriter(fileName);
            pw.print(header);
            pw.close();
        } catch (Exception e) {
            e.getStackTrace();
        }
    }

    private void writeToCSV(String prefix, long bytes, long records, double recsPerSec, double mbPerSec,
                       double avgLatency, long maxLatency, long invalid, long lowerDiscard, long higherDiscard,
                       long[] percentileValues) {
        String data = String.format("%s, %5d, %5d, %5d, %5d, %d, %11d, %9.1f, %8.2f, %8.1f, %7d, %8d, %8d, %8d", prefix, writers.get(), readers.get(), maxWriters.get(), maxReaders.get(), bytes, records, recsPerSec, mbPerSec, avgLatency, maxLatency, invalid, lowerDiscard, higherDiscard);
        for (int i = 0; i < Math.min(percentiles.length, percentileValues.length); ++i) {
            data += String.format(", %7d", percentileValues[i]);
        }
        try {
            PrintWriter pw = new PrintWriter(new FileWriter(fileName, true));
            pw.println(data);
            pw.close();
        } catch (Exception e) {
            e.getStackTrace();
        }
    }

    @Override
    public void print(long bytes, long records, double recsPerSec, double mbPerSec, double avgLatency,
                      long maxLatency, long invalid, long lowerDiscard, long higherDiscard, long[] percentileValues) {
        writeToCSV(prefix, bytes, records, recsPerSec, mbPerSec, avgLatency, maxLatency, invalid, lowerDiscard,
                higherDiscard, percentileValues);
    }

    @Override
    public void printTotal(long bytes, long records, double recsPerSec, double mbPerSec, double avgLatency,
                      long maxLatency, long invalid, long lowerDiscard, long higherDiscard, long[] percentilesValues) {
        writeToCSV("Total : " + prefix, bytes, records, recsPerSec, mbPerSec, avgLatency, maxLatency,
                invalid, lowerDiscard, higherDiscard, percentilesValues);
    }
}
