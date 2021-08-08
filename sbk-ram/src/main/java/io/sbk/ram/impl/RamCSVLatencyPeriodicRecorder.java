/**
 * Copyright (c) KMG. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 */

package io.sbk.ram.impl;

import io.sbk.grpc.LatenciesRecord;
import io.sbk.logger.SetRW;

import io.sbk.perl.LatencyRecordWindow;
import io.sbk.system.Printer;
import io.sbk.perl.ReportLatencies;
import io.sbk.perl.Print;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVPrinter;
import org.apache.commons.csv.CSVRecord;

import javax.annotation.concurrent.NotThreadSafe;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Iterator;


/**
 *  class for Performance statistics.
 */
@NotThreadSafe
public class RamCSVLatencyPeriodicRecorder extends RamHashMapLatencyPeriodicRecorder {
    final private String csvFile;
    private CSVPrinter csvPrinter;

    public RamCSVLatencyPeriodicRecorder(LatencyRecordWindow window, int maxHashMapSizeMB, Print logger,
                                         Print loggerTotal, ReportLatencies reportLatencies, SetRW setRW,
                                         String fileName) {
        super(window, maxHashMapSizeMB, logger, loggerTotal, reportLatencies, setRW);
        csvFile = fileName;
        csvPrinter = null;
    }


    /**
     * Record the latency.
     *
     * @param currentTime current time.
     * @param record Record Latencies
     */
    public void record(long currentTime, LatenciesRecord record) {
        addLatenciesRecord(record);
        if (window.isOverflow()) {
            flush(currentTime);
        }
    }

    /**
     * print the periodic Latency Results.
     *
     * @param currentTime current time.
     */
    public void stopWindow(long currentTime) {
        flush(currentTime);
        if (isOverflow()) {
            if (hashMapBytesCount > maxHashMapSizeBytes) {
                if (csvPrinter == null) {
                    deleteFile(csvFile);
                    try {
                        csvPrinter = new CSVPrinter(Files.newBufferedWriter(Paths.get(csvFile)), CSVFormat.DEFAULT
                                .withHeader(" Latency (" + time.getTimeUnit().name() + ")", "Records"));
                    } catch (IOException ex) {
                        ex.printStackTrace();
                        hashMapBytesCount = 0;
                        return;
                    }
                }
                Iterator<Long> keys =  latencies.keySet().stream().iterator();
                while (keys.hasNext()) {
                    final long key  = keys.next();
                    try {
                        csvPrinter.printRecord(key, latencies.get(key));
                    } catch (IOException ex) {
                        ex.printStackTrace();
                        hashMapBytesCount = 0;
                        return;
                    }
                    latencies.remove(key);
                }
                hashMapBytesCount = 0;
            }
        }
    }


    private void deleteFile(String fileName) {
        Path fileToDeletePath = Paths.get(fileName);
        try {
            Files.delete(fileToDeletePath);
        } catch (IOException ex) {
            //
        }
    }

    private void readCSV() {
        try {
            CSVParser csvParser = new CSVParser(Files.newBufferedReader(Paths.get(csvFile)), CSVFormat.DEFAULT
                    .withFirstRecordAsHeader().withIgnoreHeaderCase().withTrim());
            hashMapBytesCount = 0;
            for (CSVRecord csvEntry : csvParser) {
                reportLatency(Long.parseLong(csvEntry.get(0)), Long.parseLong(csvEntry.get(1)));
            }
            csvParser.close();
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }

    /**
     * print the Final Latency Results.
     *
     * @param endTime current time.
     */
    @Override
    public void stop(long endTime) {
        if (window.totalRecords > 0) {
           flush(endTime);
        }

        if (csvPrinter != null) {
            Printer.log.info("Reading CSV file :" +csvFile +" ...");
            try {
                csvPrinter.close();
            } catch (IOException ex) {
                ex.printStackTrace();
            }
            readCSV();
            deleteFile(csvFile);
            Printer.log.info("Deleted CSV file :" +csvFile);
        }
        print(endTime, loggerTotal, null);
    }

}
