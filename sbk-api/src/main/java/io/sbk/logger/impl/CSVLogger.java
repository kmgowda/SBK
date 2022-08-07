/**
 * Copyright (c) KMG. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.sbk.logger.impl;

import io.perl.data.Bytes;
import io.sbk.action.Action;
import io.sbk.config.Config;
import io.sbk.params.InputOptions;
import io.sbk.params.ParsedOptions;
import io.sbk.system.Printer;
import io.time.Time;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Paths;

/**
 * Class for recoding/printing results to a CSV file called `out.csv`.
 */
public class CSVLogger extends SystemLogger {
    /**
     * <code>DISABLE_STRING = "no"</code>.
     */
    final static public String DISABLE_STRING = "no";

    /**
     * <code>REGULAR_PRINT = "Regular"</code>.
     */
    final static public String REGULAR_PRINT = "Regular";

    /**
     * <code>TOTAL_PRINT = "Total"</code>.
     */
    final static public String TOTAL_PRINT = "Total";
    /**
     * <code>csvFile = null</code>.
     */
    protected String csvFile;
    /**
     * <code>csvEnable = false</code>.
     */
    protected boolean csvEnable;
    /**
     * <code>csvWriter</code>.
     */
    protected PrintWriter csvWriter;
    /**
     * <code>csvRowCounter = 0</code>.
     */
    private long csvRowCounter;

    /**
     * calls its super class SystemLogger.
     */
    public CSVLogger() {
        super();
    }

    @Override
    public void addArgs(final InputOptions params) throws IllegalArgumentException {
        super.addArgs(params);
        params.addOption("csvfile", true,
                """
                        CSV file to record results;
                        'no' disables this option, default: no""");
        csvEnable = false;
        csvFile = DISABLE_STRING;
        csvRowCounter = 0;
    }

    @Override
    public void parseArgs(final ParsedOptions params) throws IllegalArgumentException {
        super.parseArgs(params);
        csvFile = params.getOptionValue("csvfile", DISABLE_STRING);
        csvEnable = csvFile.compareToIgnoreCase(DISABLE_STRING) != 0;
    }

    /**
     * final public method to Open CSV.
     *
     * @throws IOException If an exception occurred.
     */
    final public void openCSV() throws IOException {
        final StringBuilder headerBuilder = new StringBuilder("ID,Header,Type,Connections,MaxConnections");
        headerBuilder.append(",Storage,Action,LatencyTimeUnit");
        headerBuilder.append(",Writers,Readers,MaxWriters,MaxReaders");
        headerBuilder.append(",ReportSeconds,MB,Records,Records/Sec,MB/Sec");
        headerBuilder.append(",AvgLatency,MaxLatency,InvalidLatencies,LowerDiscard,HigherDiscard,SLC1,SLC2");
        for (String percentileName : percentileNames) {
            headerBuilder.append(",Percentile_");
            headerBuilder.append(percentileName);
        }
        csvWriter = new PrintWriter(Files.newBufferedWriter(Paths.get(csvFile)));
        csvWriter.println(headerBuilder);
    }


    @Override
    public void open(final ParsedOptions params, final String storageName, Action action, Time time) throws IOException {
        super.open(params, storageName, action, time);
        if (csvEnable) {
            openCSV();
            Printer.log.info("SBK CSV Logger Started");
        }
    }

    /**
     * final public method to write CSV.
     *
     * @param header            String
     * @param type              String
     * @param connections           long
     * @param maxConnections        long
     * @param seconds               long
     * @param bytes                 long
     * @param records               long
     * @param recsPerSec            double
     * @param mbPerSec              double
     * @param avgLatency            double
     * @param maxLatency            long
     * @param invalid               long
     * @param lowerDiscard          long
     * @param higherDiscard         long
     * @param slc1                  long
     * @param slc2                  long
     * @param percentileValues      NotNull long[]
     */
    final public void writeToCSV(String header, String type, long connections, long maxConnections,
                                 long seconds, long bytes, long records, double recsPerSec,
                                 double mbPerSec, double avgLatency, long maxLatency, long invalid, long lowerDiscard, long higherDiscard,
                                 long slc1, long slc2, @NotNull long[] percentileValues) {
        final double mBytes = (bytes * 1.0) / Bytes.BYTES_PER_MB;
        StringBuilder data = new StringBuilder(
                String.format("%16d,%s,%s,%s,%s"
                                + ",%s,%s,%s"
                                + ",%5d,%5d,%5d,%5d"
                                + ",%8d,%11.1f,%16d,%11.1f,%8.2f,%8.1f,%7d"
                                + ",%8d,%8d,%8d,%2d,%2d",
                        ++csvRowCounter, header, type, connections, maxConnections,
                        storageName, action.name(), timeUnitFullText,
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
    public void print(int writers, int maxWriters, int readers, int maxReaders,
                      long writeRequestBytes, double writeRequestsMbPerSec, long writesRequests,
                      double writeRequestsPerSec, long readRequestBytes, double readRequestsMBPerSec,
                      long readRequests, double readRequestsPerSec, double seconds, long bytes,
                      long records, double recsPerSec, double mbPerSec,
                      double avgLatency, long minLatency, long maxLatency, long invalid, long lowerDiscard,
                      long higherDiscard, long slc1, long slc2, long[] percentileValues) {
        super.print(writers, maxWriters, readers, maxReaders, writeRequestBytes, writeRequestsMbPerSec, writesRequests,
                writeRequestsPerSec, readRequestBytes, readRequestsMBPerSec, readRequests, readRequestsPerSec,
                seconds, bytes, records, recsPerSec, mbPerSec, avgLatency, minLatency, maxLatency, invalid, lowerDiscard,
                higherDiscard, slc1, slc2, percentileValues);
        if (csvEnable) {
            writeToCSV(Config.NAME, REGULAR_PRINT, 0, 0,
                    (long) seconds, bytes, records, recsPerSec, mbPerSec, avgLatency, maxLatency, invalid,
                    lowerDiscard, higherDiscard, slc1, slc2, percentileValues);
        }
    }

    @Override
    public void printTotal(int writers, int maxWriters, int readers, int maxReaders,
                    long writeRequestBytes, double writeRequestsMbPerSec, long writeRequests,
                    double writeRequestsPerSec, long readRequestBytes, double readRequestsMBPerSec,
                    long readRequests, double readRequestsPerSec, double seconds, long bytes,
                    long records, double recsPerSec, double mbPerSec,
                    double avgLatency, long minLatency, long maxLatency, long invalid, long lowerDiscard,
                    long higherDiscard, long slc1, long slc2, long[] percentileValues) {
        super.printTotal(writers, maxWriters, readers, maxReaders, writeRequestBytes, writeRequestsMbPerSec,
                writeRequests, writeRequestsPerSec, readRequestBytes, readRequestsMBPerSec, readRequests, readRequestsPerSec,
                seconds, bytes, records, recsPerSec, mbPerSec, avgLatency, minLatency, maxLatency, invalid, lowerDiscard,
                higherDiscard, slc1, slc2, percentileValues);
        if (csvEnable) {
            writeToCSV(Config.NAME, TOTAL_PRINT, 0, 0,
                    (long) seconds, bytes, records, recsPerSec, mbPerSec, avgLatency, maxLatency, invalid,
                    lowerDiscard, higherDiscard, slc1, slc2, percentileValues);
            csvWriter.flush();
        }
    }

    @Override
    public void close(final ParsedOptions params) throws IOException {
        super.close(params);
        if (csvEnable) {
            csvWriter.close();
            Printer.log.info("SBK CSV Logger Shutdown");
        }
    }
}
