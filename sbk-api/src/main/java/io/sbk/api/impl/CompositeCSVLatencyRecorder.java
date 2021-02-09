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

import io.sbk.api.Config;
import io.sbk.api.Print;
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
import java.util.Random;

@NotThreadSafe
public class CompositeCSVLatencyRecorder extends CompositeHashMapLatencyRecorder {
    final private long maxHeapBytes;
    final private int incBytes;
    final private String csvFile;
    private long heapBytesCnt;
    private CSVPrinter csvPrinter;

    CompositeCSVLatencyRecorder(LatencyWindow window, Print logger, Print loggerTotal, long maxHeapBytes) {
        super(window, logger, loggerTotal);
        this.maxHeapBytes = maxHeapBytes;
        this.incBytes = Config.LATENCY_VALUE_SIZE_BYTES * 2;
        this.heapBytesCnt = 0;
        csvFile = Config.NAME.toUpperCase() + "-" + String.format("%06d", new Random().nextInt(1000000)) + ".csv";
        csvPrinter = null;
    }

    @Override
    public void copyLatency(long latency, long events) {
        Long val = latencies.get(latency);
        if (val == null) {
            val = 0L;
        } else {
            heapBytesCnt += incBytes;
        }
        latencies.put(latency, val + events);
    }


    /**
     * print the periodic Latency Results.
     *
     * @param currentTime current time.
     */
    public void print(long currentTime) {
        super.print(currentTime);

        if (heapBytesCnt > maxHeapBytes) {
            if (csvPrinter == null) {
                deleteFile(csvFile);
                try {
                    csvPrinter = new CSVPrinter(Files.newBufferedWriter(Paths.get(csvFile)), CSVFormat.DEFAULT
                            .withHeader(" Latency (" + time.getTimeUnit().name() + ")", "Records"));
                } catch (IOException ex) {
                    ex.printStackTrace();
                    heapBytesCnt = 0;
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
                    heapBytesCnt = 0;
                    return;
                }
                latencies.remove(key);
            }
            heapBytesCnt = 0;
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
            for (CSVRecord csvEntry : csvParser) {
                copyLatency(Long.parseLong(csvEntry.get(0)), Long.parseLong(csvEntry.get(1)));
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
    public void printTotal(long endTime) {
        window.printPendingData(endTime, windowLogger, this);
        if (csvPrinter != null) {
            SbkLogger.log.info("Reading CSV file :" +csvFile +" ...");
            try {
                csvPrinter.close();
            } catch (IOException ex) {
                ex.printStackTrace();
            }
            readCSV();
            deleteFile(csvFile);
            SbkLogger.log.info("Deleted CSV file :" +csvFile);
        }
        print(endTime, loggerTotal, null);
    }

}
