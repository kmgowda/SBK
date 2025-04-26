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

import io.perl.data.Bytes;
import io.perl.config.LatencyConfig;
import io.perl.api.LatencyPercentiles;
import io.perl.api.LatencyRecord;
import io.perl.api.LatencyRecordWindow;
import io.perl.api.LatencyRecorder;
import io.perl.system.PerlPrinter;
import io.perl.api.ReportLatencies;
import io.time.Time;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVPrinter;
import org.apache.commons.csv.CSVRecord;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Class CSVExtendedLatencyRecorder.
 */
final public class CSVExtendedLatencyRecorder extends LatencyRecordWindow {
    final private LatencyRecordWindow latencyBuffer;
    final private CSVLatencyReporter csvReporter;

    /**
     * Constructor CSVExtendedLatencyRecorder pass all values to its super class
     * and initialize {@link #latencyBuffer} and {@link #csvReporter}.
     *
     * @param lowLatency            long
     * @param highLatency           long
     * @param totalLatencyMax       long
     * @param totalRecordsMax       long
     * @param bytesMax              long
     * @param percentilesFractions  double[]
     * @param time                  Time
     * @param latencyBuffer         LatencyRecordWindow
     * @param csvFileSizeGB         int
     * @param fileName              String
     */
    public CSVExtendedLatencyRecorder(long lowLatency, long highLatency, long totalLatencyMax, long totalRecordsMax,
                                      long bytesMax, double[] percentilesFractions, Time time,
                                      LatencyRecordWindow latencyBuffer, int csvFileSizeGB, String fileName) {
        super(lowLatency, highLatency, totalLatencyMax, totalRecordsMax, bytesMax, percentilesFractions, time);
        this.latencyBuffer = latencyBuffer;
        this.csvReporter = new CSVLatencyReporter(this, csvFileSizeGB, fileName);
    }

    private void checkBufferFull() {
        if (latencyBuffer.isFull()) {
            latencyBuffer.copyPercentiles(percentiles, csvReporter);
            latencyBuffer.reset();
        }
    }

    @Override
    public void reset(long startTime) {
        super.reset(startTime);
        latencyBuffer.reset(startTime);
        csvReporter.reset();
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
    public void recordLatency(long startTime, int events, int bytes, long latency) {
        latencyBuffer.recordLatency(startTime, events, bytes, latency);
        checkBufferFull();
    }

    @Override
    public void copyPercentiles(LatencyPercentiles percentiles, ReportLatencies reportLatencies) {
        if (this.totalRecords > 0) {
            csvReporter.readCSV(latencyBuffer);
        }

        latencyBuffer.copyPercentiles(percentiles, reportLatencies);
        // Update the current Window values to print
        super.reset();
        super.update(latencyBuffer);
    }

    @Override
    final public boolean isFull() {
        return csvReporter.isFull() || super.isOverflow();
    }

    @Override
    final public long getMaxMemoryBytes() {
        return csvReporter.getMaxMemoryBytes();
    }

    private static class CSVLatencyReporter implements ReportLatencies {
        final LatencyRecorder recorder;
        final private String csvFile;
        final private long maxCsvSizeBytes;
        final private int incBytes;
        private long csvBytesCount;
        private CSVPrinter csvPrinter;

        public CSVLatencyReporter(LatencyRecorder recorder, int csvFileSizeGB, String fileName) {
            this.recorder = recorder;
            this.csvFile = fileName;
            this.maxCsvSizeBytes = csvFileSizeGB * Bytes.BYTES_PER_GB;
            this.incBytes = LatencyConfig.LATENCY_VALUE_SIZE_BYTES * 2;
            this.csvBytesCount = 0;
            this.csvPrinter = null;
        }

        @Override
        public void reportLatencyRecord(LatencyRecord record) {
            recorder.update(record);
        }

        @Override
        public void reportLatency(long latency, long count) {
            if (csvPrinter == null) {
                deleteCSVFile();
                try {
                    PerlPrinter.log.info("Creating CSV file: " + csvFile + " ...");
                    csvPrinter = new CSVPrinter(Files.newBufferedWriter(Paths.get(csvFile)),
                            CSVFormat.DEFAULT.builder().setHeader(" Latency", "Records").get());
                } catch (IOException ex) {
                    ex.printStackTrace();
                }
            }
            if (csvPrinter != null) {
                try {
                    csvPrinter.printRecord(latency, count);
                    csvBytesCount += incBytes;
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

        private void deleteCSVFile() {
            Path fileToDeletePath = Paths.get(csvFile);
            if (!Files.exists(fileToDeletePath)) {
                return;
            }
            PerlPrinter.log.info("Deleting CSV file: " + csvFile + " ...");
            try {
                Files.delete(fileToDeletePath);
            } catch (IOException ex) {
                //
            }
        }

        private void readCSV(ReportLatencies reportLatencies) {
            if (csvPrinter != null) {
                try {
                    csvPrinter.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                csvPrinter = null;
            }
            PerlPrinter.log.info("Reading CSV file: " + csvFile + " ...");
            reportLatencies.reportLatencyRecord(recorder);
            try {
                CSVParser csvParser =  CSVParser.parse(Files.newBufferedReader(Paths.get(csvFile)),
                        CSVFormat.DEFAULT.builder().
                                setSkipHeaderRecord(true).setIgnoreHeaderCase(true).setTrim(true).get());
                for (CSVRecord csvEntry : csvParser) {
                    reportLatencies.reportLatency(Long.parseLong(csvEntry.get(0)), Long.parseLong(csvEntry.get(1)));
                }
                csvParser.close();
            } catch (IOException ex) {
                ex.printStackTrace();
            }
            deleteCSVFile();
        }

        public boolean isFull() {
            return csvBytesCount >= maxCsvSizeBytes;
        }

        public long getMaxMemoryBytes() {
            return maxCsvSizeBytes;
        }

        public void reset() {
            if (csvPrinter != null) {
                try {
                    csvPrinter.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                csvPrinter = null;
            }
            deleteCSVFile();
            csvBytesCount = 0;
        }
    }

}
