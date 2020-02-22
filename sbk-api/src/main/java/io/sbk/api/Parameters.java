/**
 * Copyright (c) KMG. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 */
package io.sbk.api;

import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

/**
 * class for Command Line Parameters.
 */
public interface Parameters {
    int MAXTIME = 60 * 60 * 24;
    int TIMEOUT = 1000;

    /**
     * Add the driver specific command line arguments.
     * @param name Name of the parameter to add.
     * @param hasArg flag signalling if an argument is required after this option.
     * @param description Self-documenting description.
     * @return Options return the added options
     */
    Options addOption(String name, boolean hasArg, String description);

    /**
     * Parse the driver specific command line arguments.
     * @param name Name of the parameter to add.
     * @param description Self-documenting description.
     * @return Options return the added options
     */
    Options addOption(String name, String description);

    /**
     * Returns whether the named Option is a member of this Parameters.
     * @param name name of the parameter option
     * @return  true if the named Option is a member of this Options
     */
    boolean hasOption(String name);

    /**
     * Retrieve the Option matching the parameter name specified.
     * @param name Name of the parameter.
     * @return  parameter value
     */
    String getOptionValue(String name);

    /**
     * Retrieve the Option matching the parameter name specified.
     * @param name Name of the parameter.
     * @param defaultValue default value if the parameter not found
     * @return   parameter value
     */
    String getOptionValue(String name, String defaultValue);

    /**
     * Get the execution time in seconds.
     * @return   seconds to run
     */
    int getSecondsToRun();

    /**
     * Check if the both read and writes are requested.
     * @return   True if both Writers and readers are supplied; False otherwise.
     */
    boolean isWriteAndRead();

    /**
     * Check metrics enabled.
     * @return  True by default; False is user explicitly specified.
     */
    boolean isMetricsEnabled();

    /**
     * Get the prefix name for metrics.
     * @return  Prefix Name.
     */
    String getPrefixName();

    /**
     * Get the Number of records to read/writer.
     * @return   number of records.
     */
    int getRecordsCount();

    /**
     * Start time of SBK.
     * @return   starting time of SBK in milliseconds.
     */
    long getStartTime();

    /**
     * Size of the record/event to read or write.
     * @return   size of the record.
     */
    int getRecordSize();

    /**
     * Number of records/events to write per single flush.
     * @return   number of records per flush.
     */
    int getRecordsPerFlush();

    /**
     * Number of records/events to write per Second.
     * @return   number of records per seconds.
     */
    int getRecordsPerSec();

    /**
     * Number of Writers/Producers.
     * @return   Number of Writers.
     */
    int getWritersCount();

    /**
     * Number of Readers/Consumers.
     * @return   Number of Readers.
     */
    int getReadersCount();

    /**
     * CSV file to store the write and read benchmarking data.
     * @return   Name of the CSV file.
     */
    String getCsvFile();

    /**
     * Time out for data to read.
     * @return  time-out in milliseconds.
     */
    int getTimeout();

    /**
     * Print the -help output.
     */
    void printHelp();

    /**
     * Print the -version output.
     */
    void printVersion();

    /**
     * Parse the command line arguments.
     * @param args list of command line arguments.
     * @throws IllegalArgumentException If an exception occurred.
     * @throws ParseException If an exception occurred.
     */
    void parseArgs(String[] args) throws ParseException, IllegalArgumentException;
}
