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
import io.sbk.api.Config;
import io.sbk.api.PeriodicLatencyRecorder;
import io.sbk.api.Print;
import io.sbk.api.Time;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Random;

public class SbkLatencyRecorder implements PeriodicLatencyRecorder {
    final private double[] percentiles;
    final private Print windowLogger;
    final private Print totalWindowLogger;
    final private  LatencyWindow window;
    final private  LatencyWindow totalWindow;
    final private String csvFile;
    final private boolean deletecsvFile;

    public SbkLatencyRecorder(long lowLatency, long highLatency, double[] percentiles, Time time,
                              Action action, Print logger, Print loggerTotal, String outFile) throws IOException {
        this.windowLogger = logger;
        this.totalWindowLogger = loggerTotal;
        this.percentiles = new double[percentiles.length];
        for (int i = 0; i < percentiles.length; i++) {
            this.percentiles[i] = percentiles[i] / 100.0;
        }
        final long memSizeMB = ((highLatency - lowLatency) * Config.LATENCY_VALUE_SIZE_BYTES) / (1024 * 1024);
        if (memSizeMB < Config.MAX_LATENCY_MEMORY_MB) {
            SbkLogger.log.info("Window Latency Store: Array");
            window = new ArrayLatencyRecorder(lowLatency, highLatency, Config.LONG_MAX, Config.LONG_MAX, Config.LONG_MAX,
                    this.percentiles, time);
            deletecsvFile = false;
            if (outFile == null || outFile.equalsIgnoreCase("no")) {
                csvFile = null;
                SbkLogger.log.info("Total Latency Store: HashMap");
                totalWindow = new HashMapLatencyRecorder(lowLatency, highLatency, Config.LONG_MAX, Config.LONG_MAX, Config.LONG_MAX,
                        this.percentiles, time);
            } else {
                csvFile = outFile;
                deleteFile(csvFile);
                SbkLogger.log.info("Total Latency Store CSV file:" + csvFile);
                totalWindow = new CSVHashMapLatencyRecorder(lowLatency, highLatency, Config.LONG_MAX, Config.LONG_MAX, Config.LONG_MAX,
                        this.percentiles, time, action, csvFile);
            }
        } else {
            SbkLogger.log.info("Window Latency Store: HashMap");
            window = new HashMapLatencyRecorder(lowLatency, highLatency, Config.LONG_MAX, Config.LONG_MAX, Config.LONG_MAX,
                    this.percentiles, time);

            if (outFile == null) {
                csvFile = Config.NAME.toUpperCase() + "-" + action.name() + "-" + String.format("%04d", new Random().nextInt(10000)) + ".csv";
                deletecsvFile = true;
            } else {
                csvFile = outFile;
                deletecsvFile = false;
            }
            if (csvFile.equalsIgnoreCase("no")) {
                SbkLogger.log.info("Total Latency Store: HashMap");
                totalWindow = new HashMapLatencyRecorder(lowLatency, highLatency, Config.LONG_MAX, Config.LONG_MAX, Config.LONG_MAX,
                        this.percentiles, time);
            } else {
                deleteFile(csvFile);
                SbkLogger.log.info("Total Latency Store CSV file: " + csvFile);
                totalWindow = new CSVHashMapLatencyRecorder(lowLatency, highLatency, Config.LONG_MAX, Config.LONG_MAX, Config.LONG_MAX,
                        this.percentiles, time, action, csvFile);
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

    /**
     * Start the window.
     *
     * @param startTime starting time.
     */
    public void start(long startTime) {
        window.reset(startTime);
        totalWindow.reset(startTime);
    }

    /**
     * Reset the window.
     *
     * @param startTime starting time.
     */
    public void resetWindow(long startTime) {
        window.reset(startTime);
    }

    /**
     * is Overflow condition for this recorder.
     *
     * @return isOverflow condition occurred or not
     */
    public boolean isOverflow() {
        return totalWindow.isOverflow();
    }

    /**
     * Get the current time duration of this window.
     *
     * @param currentTime current time.
     * @return elapsed Time in Milliseconds from the startTime.
     */
    public long elapsedMilliSeconds(long currentTime) {
        return window.elapsedMilliSeconds(currentTime);
    }


    /**
     * Record the latency.
     *
     * @param startTime start time of the event.
     * @param bytes number of bytes
     * @param events number of events (records)
     * @param latency latency value
     */
    public void record(long startTime, int bytes, int events, long latency) {
        window.record(startTime, bytes, events, latency);
        totalWindow.record(startTime, bytes, events, latency);
    }


    /**
     * print the periodic Latency Results.
     *
     * @param currentTime current time.
     */
    public void print(long currentTime) {
        window.print(currentTime, windowLogger, null);
    }

    /**
     * print the Final Latency Results.
     *
     * @param endTime current time.
     */
    public void printTotal(long endTime) {
        window.printPendingData(endTime, windowLogger, null);
        totalWindow.print(endTime, totalWindowLogger, null);
        if (csvFile != null && deletecsvFile) {
            deleteFile(csvFile);
            SbkLogger.log.info("CSV File: "+csvFile+" Deleted");
        }
    }

}
