/**
 * Copyright (c) KMG. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 */

package io.sbk.perl.impl;

import io.sbk.config.PerlConfig;
import io.sbk.perl.LatencyPercentiles;
import io.sbk.perl.LatencyRecord;
import io.sbk.perl.LatencyRecordWindow;
import io.sbk.perl.LatencyRecorder;
import io.sbk.perl.ReportLatencies;
import io.sbk.system.Printer;
import io.sbk.time.Time;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVPrinter;
import org.apache.commons.csv.CSVRecord;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

final public class CSVExtendedLatencyRecorder extends LatencyRecordWindow {
    final private LatencyRecordWindow latencyBuffer;
    final private CSVLatencyReporter csvReporter;

    public CSVExtendedLatencyRecorder(long lowLatency, long highLatency, long totalLatencyMax, long totalRecordsMax,
                                      long bytesMax, double[] percentilesFractions, Time time,
                                      LatencyRecordWindow latencyBuffer, int csvFileSizeGB, String fileName) {
        super(lowLatency, highLatency, totalLatencyMax, totalRecordsMax, bytesMax, percentilesFractions, time);
        this.latencyBuffer = latencyBuffer;
        this.csvReporter = new CSVLatencyReporter(this, csvFileSizeGB, fileName);
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
            this.maxCsvSizeBytes = csvFileSizeGB * PerlConfig.BYTES_PER_GB;
            this.incBytes = PerlConfig.LATENCY_VALUE_SIZE_BYTES * 2;
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
                    Printer.log.info("Creating CSV file: " +csvFile +" ...");
                    csvPrinter = new CSVPrinter(Files.newBufferedWriter(Paths.get(csvFile)), CSVFormat.DEFAULT
                            .withHeader(" Latency", "Records"));
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
            Printer.log.info("Deleting CSV file: " +csvFile +" ...");
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
            Printer.log.info("Reading CSV file: " +csvFile +" ...");
            reportLatencies.reportLatencyRecord(recorder);
            try {
                CSVParser csvParser = new CSVParser(Files.newBufferedReader(Paths.get(csvFile)), CSVFormat.DEFAULT
                        .withFirstRecordAsHeader().withIgnoreHeaderCase().withTrim());
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
    public void recordLatency(long startTime, int bytes, int events, long latency) {
        latencyBuffer.recordLatency(startTime, bytes, events, latency);
        checkBufferFull();
    }

    @Override
    public void copyPercentiles(LatencyPercentiles percentiles, ReportLatencies reportLatencies) {
        if (this.totalRecords > 0) {
            csvReporter.readCSV(latencyBuffer);
        }
        super.reset();
        super.update(latencyBuffer);
        latencyBuffer.copyPercentiles(percentiles, reportLatencies);
    }

    @Override
    final public boolean isFull() {
       return csvReporter.isFull() || super.isOverflow();
    }

    @Override
    final public long getMaxMemoryBytes() {
       return csvReporter.getMaxMemoryBytes();
    }

}
