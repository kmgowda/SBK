/**
 * Copyright (c) KMG. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 */

package io.sbk.logger.impl;

import java.io.IOException;
import java.io.PrintWriter;

import io.sbk.action.Action;
import io.sbk.config.Config;
import io.sbk.options.InputOptions;
import io.sbk.config.PerlConfig;
import io.sbk.time.Time;

import java.io.FileWriter;

/**
 * Class for recoding/printing results to a CSV file called `out.csv`.
 */
public class SbkCSVLogger extends SystemLogger {
    final static public String DISABLE_STRING = "no";
    final static public String REGULAR_PRINT = "Regular";
    final static public String TOTAL_PRINT = "Total";
    public String csvFile;
    public boolean csvEnable;
    public PrintWriter csvWriter;
    private long csvRowCounter;


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
        csvRowCounter = 0;
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

    final public void openCSV() throws IOException {
        final StringBuilder headerBuilder = new StringBuilder("ID,Header,Type,Connections,MaxConnections");
        headerBuilder.append(",Storage,Action,LatencyTimeUnit");
        headerBuilder.append(",Writers,Readers,MaxWriters,MaxReaders");
        headerBuilder.append(",ReportSeconds,MB,Records,Records/Sec,MB/Sec");
        headerBuilder.append(",AvgLatency,MaxLatency,InvalidLatencies,LowerDiscard,HigherDiscard,SLC1%,SLC2%");
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

    final public void writeToCSV(String header, String type, long connections, long maxConnections,
                           long seconds, long bytes, long records, double recsPerSec,
                           double mbPerSec, double avgLatency, long maxLatency, long invalid, long lowerDiscard, long higherDiscard,
                           int slc1, int slc2, long[] percentileValues) {
        final double mBytes = (bytes * 1.0) / PerlConfig.BYTES_PER_MB;
        StringBuilder data = new StringBuilder(
                String.format("%16d,%s,%s,%s,%s"
                        + ",%s,%s,%s"
                        + ",%5d,%5d,%5d,%5d"
                        + ",%8d,%11.1f,%16d,%11.1f,%8.2f,%8.1f,%7d"
                        + ",%8d,%8d,%8d,%2d,%2d",
                        ++csvRowCounter, header, type, connections, maxConnections,
                        storageName, action.name(), timeUnitText,
                        writers.get(), readers.get(), maxWriters.get(), maxReaders.get(),
                        seconds, mBytes, records, recsPerSec, mbPerSec, avgLatency, maxLatency,
                        invalid, lowerDiscard, higherDiscard, slc1, slc2)
        );

        for (int i = 0; i < Math.min(percentiles.length, percentileValues.length); ++i) {
            data.append(String.format(",%7d", percentileValues[i]));
        }
        csvWriter.println(data);
    }

    @Override
    public void print(double seconds, long bytes, long records, double recsPerSec, double mbPerSec, double avgLatency,
                      long maxLatency, long invalid, long lowerDiscard, long higherDiscard,
                      int slc1, int slc2, long[] percentileValues) {
        super.print(seconds, bytes, records, recsPerSec, mbPerSec, avgLatency, maxLatency, invalid,
                lowerDiscard, higherDiscard, slc1, slc2, percentileValues);
        if (csvEnable) {
            writeToCSV(Config.NAME, REGULAR_PRINT, 0, 0,
                    (long) seconds, bytes, records, recsPerSec, mbPerSec, avgLatency, maxLatency, invalid,
                    lowerDiscard, higherDiscard, slc1, slc2, percentileValues);
        }
    }

    @Override
    public void printTotal(double seconds, long bytes, long records, double recsPerSec, double mbPerSec,
                           double avgLatency, long maxLatency, long invalid, long lowerDiscard, long higherDiscard,
                           int slc1, int slc2, long[] percentileValues) {
        super.printTotal( seconds, bytes, records, recsPerSec, mbPerSec, avgLatency, maxLatency,
                invalid, lowerDiscard, higherDiscard, slc1, slc2, percentileValues);
        if (csvEnable) {
            writeToCSV(Config.NAME, TOTAL_PRINT, 0, 0,
                    (long) seconds, bytes, records, recsPerSec, mbPerSec, avgLatency, maxLatency, invalid,
                    lowerDiscard, higherDiscard, slc1, slc2, percentileValues);
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
