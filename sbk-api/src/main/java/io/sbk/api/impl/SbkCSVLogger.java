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

import io.sbk.api.InputOptions;

import java.io.FileWriter;

/**
 * Class for recoding/printing results to a CSV file called `out.csv`.
 */
public class SbkCSVLogger extends SystemLogger {
    private String fileName;
    private boolean writeToStdout;
    private PrintWriter printWriter;

    public SbkCSVLogger() {
        super();
    }

    @Override
    public void addArgs(final InputOptions params) throws IllegalArgumentException {
        super.addArgs(params);
        params.addOption("csvfile", true, "CSV file name to append the metrics to, e.g.: myOutput.csv; defalt: out.csv");
        params.addOption("stdout", false, "Write the metrics to stdout along with the csv file.");
    }

    @Override
    public void parseArgs(final InputOptions params) throws IllegalArgumentException {
        super.parseArgs(params);
        fileName = params.getOptionValue("csvfile", "out.csv");
        if (params.hasOption("stdout")) {
            writeToStdout = true;
        } else {
            writeToStdout = false;
        }

        // Writing the header to the csv file.
        String header = "_Prefix,_Writers,_Readers,_maxWriters,_maxReaders,_Bytes,_Records,_RecsPerSec,_mbPerSec,_AvgLatency,_maxLatency,_InvalidLatencies,_lowerDiscard,_higherDiscard,_10thPercentile,_25thPercentile,_50thPercentile,_75thPercentile,_90thPercentile,_95thPercentile,_99thPercentile,_99.9thPercentile,_99.99thPercentile\n";
        try {
            printWriter = new PrintWriter(new FileWriter(fileName, true));
            printWriter.print(header);
        } catch (Exception e) {
            e.getStackTrace();
        }
    }

    private void writeToCSV(String prefix, long bytes, long records, double recsPerSec, double mbPerSec,
                       double avgLatency, long maxLatency, long invalid, long lowerDiscard, long higherDiscard,
                       long[] percentileValues) {
        String data = String.format("%s,%5d,%5d,%5d,%5d,%d,%11d,%9.1f,%8.2f,%8.1f,%7d,%8d,%8d,%8d", prefix, writers.get(), readers.get(), maxWriters.get(), maxReaders.get(), bytes, records, recsPerSec, mbPerSec, avgLatency, maxLatency, invalid, lowerDiscard, higherDiscard);
        for (int i = 0; i < Math.min(percentiles.length, percentileValues.length); ++i) {
            data += String.format(", %7d", percentileValues[i]);
        }
        try {
            printWriter.println(data);
        } catch (Exception e) {
            e.getStackTrace();
        }
    }

    @Override
    public void print(long bytes, long records, double recsPerSec, double mbPerSec, double avgLatency,
                      long maxLatency, long invalid, long lowerDiscard, long higherDiscard, long[] percentileValues) {
        if (writeToStdout) {
            super.print(bytes, records, recsPerSec, mbPerSec, avgLatency, maxLatency, invalid, lowerDiscard, higherDiscard, percentileValues);
        }
        writeToCSV(prefix, bytes, records, recsPerSec, mbPerSec, avgLatency, maxLatency, invalid, lowerDiscard,
                higherDiscard, percentileValues);
    }

    @Override
    public void printTotal(long bytes, long records, double recsPerSec, double mbPerSec, double avgLatency,
                      long maxLatency, long invalid, long lowerDiscard, long higherDiscard, long[] percentilesValues) {
        if (writeToStdout) {
            super.printTotal(bytes, records, recsPerSec, mbPerSec, avgLatency, maxLatency, invalid, lowerDiscard, higherDiscard, percentilesValues);
        }
        writeToCSV("Total : " + prefix, bytes, records, recsPerSec, mbPerSec, avgLatency, maxLatency,
                invalid, lowerDiscard, higherDiscard, percentilesValues);
        printWriter.close();
    }
}
