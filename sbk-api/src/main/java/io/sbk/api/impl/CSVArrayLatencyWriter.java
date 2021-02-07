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

import io.sbk.api.Action;
import io.sbk.api.Print;
import io.sbk.api.Time;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVPrinter;
import org.apache.commons.csv.CSVRecord;

import javax.annotation.concurrent.NotThreadSafe;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

@NotThreadSafe
public class CSVArrayLatencyWriter extends HashMapLatencyRecorder {
    final private String csvFile;
    final private CSVPrinter csvPrinter;

    CSVArrayLatencyWriter(long baseLatency, long latencyThreshold, long totalLatencyMax, long totalRecordsMax, long bytesMax,
                          double[] percentiles, Time time, Action action, String csvFile) throws IOException {
        super(baseLatency, latencyThreshold, totalLatencyMax, totalRecordsMax, bytesMax, percentiles, time);
        this.csvFile = csvFile;
        csvPrinter = new CSVPrinter(Files.newBufferedWriter(Paths.get(csvFile)), CSVFormat.DEFAULT
                .withHeader("Start Time (" + time.getTimeUnit().toString() + ")", "data size (bytes)", "Records", action.name()+" Latency (" + time.getTimeUnit().name() + ")"));
    }

    private void readCSV() {
        try {
            CSVParser csvParser = new CSVParser(Files.newBufferedReader(Paths.get(csvFile)), CSVFormat.DEFAULT
                    .withFirstRecordAsHeader().withIgnoreHeaderCase().withTrim());
            reset(startTime);
            for (CSVRecord csvEntry : csvParser) {
                super.record(Long.parseLong(csvEntry.get(0)), Integer.parseInt(csvEntry.get(1)),
                        Integer.parseInt(csvEntry.get(2)), Long.parseLong(csvEntry.get(3)));
            }
            csvParser.close();
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
    }

    @Override
    public void record(long startTime, int bytes, int events, long latency) {
        try {
            if (record(bytes, events, latency)) {
                csvPrinter.printRecord(startTime, bytes, events, latency);
            }
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
    }

    @Override
    public void print(long endTime, Print logger) {
        try {
            csvPrinter.close();
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
        readCSV();
        super.print(endTime, logger);
    }
}
