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

import java.io.IOException;
import java.io.PrintWriter;

import io.sbk.api.Action;
import io.sbk.api.InputOptions;
import io.sbk.perl.PerlConfig;
import io.sbk.perl.Time;

import java.io.FileWriter;

/**
 * Class for recoding/printing results to a CSV file called `out.csv`.
 */
public class SbkCSVLogger extends SystemLogger {
    final static public String DISABLE_STRING = "no";
    public String csvFile;
    public boolean csvEnable;
    public PrintWriter csvWriter;


    public SbkCSVLogger() {
        super();
    }

    @Override
    public void addArgs(final InputOptions params) throws IllegalArgumentException {
        super.addArgs(params);
        params.addOption("csvfile", true, "CSV file to record results" +
                "; 'no' disables this option, default: no");
        csvEnable = false;
        csvFile = DISABLE_STRING;
    }

    @Override
    public void parseArgs(final InputOptions params) throws IllegalArgumentException {
        super.parseArgs(params);
        csvFile = params.getOptionValue("csvfile", DISABLE_STRING);
        if (csvFile.compareToIgnoreCase(DISABLE_STRING) == 0) {
            csvEnable = false;
        } else {
            csvEnable = true;
        }
    }

    public void openCSV() throws IOException {
        final StringBuilder headerBuilder =
                new StringBuilder("Action,LatencyTimeUnit,Writers,Readers,MaxWriters,MaxReaders");
        headerBuilder.append(",MB,Records,Records/Sec,MB/Sec");
        headerBuilder.append(",AvgLatency,MaxLatency,InvalidLatencies,LowerDiscard,HigherDiscard");
        for (String percentileName : percentileNames) {
            headerBuilder.append(",Percentile_");
            headerBuilder.append(percentileName);
        }
        csvWriter = new PrintWriter(new FileWriter(csvFile, false));
        csvWriter.println(headerBuilder);
    }


    @Override
    public void open(final InputOptions params, final String storageName, Action action, Time time) throws  IOException {
        super.open(params, storageName, action, time);
        if (csvEnable) {
            openCSV();
        }
    }

    public void writeToCSV(String prefix, long bytes, long records, double recsPerSec, double mbPerSec,
                       double avgLatency, long maxLatency, long invalid, long lowerDiscard, long higherDiscard,
                       long[] percentileValues) {
        final double mBytes = (bytes * 1.0) / PerlConfig.BYTES_PER_MB;
        StringBuilder data = new StringBuilder(
                String.format("%s,%s,%5d,%5d,%5d,%5d,%11.1f,%11d,%9.1f,%8.2f,%8.1f,%7d,%8d,%8d,%8d", prefix,
                        timeUnitText, writers.get(), readers.get(), maxWriters.get(), maxReaders.get(),
                        mBytes, records, recsPerSec, mbPerSec, avgLatency, maxLatency,
                        invalid, lowerDiscard, higherDiscard)
        );

        for (int i = 0; i < Math.min(percentiles.length, percentileValues.length); ++i) {
            data.append(String.format(", %7d", percentileValues[i]));
        }
        csvWriter.println(data);
    }

    @Override
    public void print(long bytes, long records, double recsPerSec, double mbPerSec, double avgLatency,
                      long maxLatency, long invalid, long lowerDiscard, long higherDiscard, long[] percentileValues) {
        super.print(bytes, records, recsPerSec, mbPerSec, avgLatency, maxLatency, invalid,
                lowerDiscard, higherDiscard, percentileValues);
        if (csvEnable) {
            writeToCSV(prefix, bytes, records, recsPerSec, mbPerSec, avgLatency, maxLatency, invalid, lowerDiscard,
                    higherDiscard, percentileValues);
        }
    }

    @Override
    public void close(final InputOptions params) throws IOException {
        super.close(params);
        if (csvEnable) {
            csvWriter.close();
        }
    }
}
