/**
 * Copyright (c) KMG. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.sbk.logger;

import io.perl.logger.PerformanceLogger;
import io.sbk.action.Action;
import io.perl.exception.ExceptionHandler;
import io.sbk.options.InputOptions;
import io.sbk.options.ParsedOptions;
import io.time.Time;


import java.io.IOException;

/**
 * Interface for recoding/printing results.
 */
public interface Logger extends PerformanceLogger {

    /**
     * Add the Metric type specific command line arguments.
     *
     * @param params InputOptions object to be extended.
     * @throws IllegalArgumentException If an exception occurred.
     */
    void addArgs(final InputOptions params) throws IllegalArgumentException;

    /**
     * Parse the Metric specific command line arguments.
     *
     * @param params InputOptions object to be parsed for driver specific parameters/arguments.
     * @throws IllegalArgumentException If an exception occurred.
     */
    void parseArgs(final ParsedOptions params) throws IllegalArgumentException;


    /**
     * Open the Logger.
     *
     * @param params      InputOptions object to be parsed for driver specific parameters/arguments.
     * @param storageName The Name of the storage.
     * @param action      action to print
     * @param time        time interface
     * @throws IOException If an exception occurred.
     */
    void open(final ParsedOptions params, final String storageName, final Action action, Time time)
            throws IOException;

    /**
     * Close the Logger.
     *
     * @param params InputOptions object to be parsed for driver specific parameters/arguments.
     * @throws IOException If an exception occurred.
     */
    void close(final ParsedOptions params) throws IOException;

    /**
     * Default implementation for setting exception handler.
     * if the logger encounters any exception, it can report to SBK.
     *
     * @param handler Exception handler
     */
    default void setExceptionHandler(ExceptionHandler handler) {

    }

}
