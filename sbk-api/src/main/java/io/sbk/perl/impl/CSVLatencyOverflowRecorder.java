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

public class CSVLatencyOverflowRecorder extends LatencyRecordWindow {
    final LatencyRecordWindow latencyBuffer;
    final CSVLatencyReporter csvReporter;

    public CSVLatencyOverflowRecorder(long lowLatency, long highLatency, long totalLatencyMax, long totalRecordsMax,
                                      long bytesMax, double[] percentilesFractions, Time time, LatencyRecordWindow latencyBuffer,
                                      String fileName) {
        super(lowLatency, highLatency, totalLatencyMax, totalRecordsMax, bytesMax, percentilesFractions, time);
        this.latencyBuffer = latencyBuffer;
        this.csvReporter = new CSVLatencyReporter(this, fileName);
    }


    private static class CSVLatencyReporter implements ReportLatencies {
        final LatencyRecorder recorder;
        final private String csvFile;
        private CSVPrinter csvPrinter;

        public CSVLatencyReporter(LatencyRecorder recorder, String fileName) {
            this.recorder = recorder;
            this.csvFile = fileName;
            this.csvPrinter = null;
        }

        @Override
        public void reportLatencyRecord(LatencyRecord record) {
            recorder.updateRecord(record);
        }

        @Override
        public void reportLatency(long latency, long count) {
            if (csvPrinter == null) {
                deleteCSVFile();
                try {
                    csvPrinter = new CSVPrinter(Files.newBufferedWriter(Paths.get(csvFile)), CSVFormat.DEFAULT
                            .withHeader(" Latency", "Records"));
                } catch (IOException ex) {
                    ex.printStackTrace();
                }
            }
            if (csvPrinter != null) {
                try {
                    csvPrinter.printRecord(latency, count);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

        private void deleteCSVFile() {
            Path fileToDeletePath = Paths.get(csvFile);
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
            Printer.log.info("Reading CSV file :" +csvFile +" ...");
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


    }

   private void checkOverflow() {
        if (latencyBuffer.isOverflow()) {
            latencyBuffer.copyPercentiles(percentiles, csvReporter);
        }
   }

    @Override
    public void reportLatencyRecord(LatencyRecord record) {
        latencyBuffer.reportLatencyRecord(record);
        checkOverflow();
    }

    @Override
    public void reportLatency(long latency, long count) {
        latencyBuffer.reportLatency(latency, count);
        checkOverflow();
    }

    @Override
    public void recordLatency(long startTime, int bytes, int events, long latency) {
        latencyBuffer.recordLatency(startTime, bytes, events, latency);
        checkOverflow();
    }

    @Override
    public void copyPercentiles(LatencyPercentiles percentiles, ReportLatencies reportLatencies) {
        csvReporter.readCSV(latencyBuffer);
        latencyBuffer.copyPercentiles(percentiles, reportLatencies);
    }


}
