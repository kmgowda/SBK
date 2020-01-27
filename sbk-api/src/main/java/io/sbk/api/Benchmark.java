/**
 * Copyright (c) KMG. All Rights Reserved..
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 */

package io.sbk.api;
import java.io.IOException;

/**
 * Interface for Benchmarking.
 */
public interface  Benchmark {

    /**
     * Add the driver specific command line arguments.
     * @param params Parameters object to be extended.
     */
    void addArgs(final Parameters params);

    /**
     * Add the driver specific command line arguments.
     * @param params Parameters object to be parsed.
     * @throws IllegalArgumentException If an exception occurred.
     */
    void parseArgs(final Parameters params) throws IllegalArgumentException;

    /**
     * Open the storage device / client to perform the benchmarking.
     * @param params configuration parameters.
     * @throws IOException If an exception occurred.
     */
    void openStorage(final Parameters params) throws IOException;

    /**
     * Close the Storage device / client.
     * @param params configuration parameters.
     * @throws IOException If an exception occurred.
     */
    void closeStorage(final Parameters params) throws IOException;

    /**
     * Create a Single Writer.
     * @param id Writer id
     * @param params configuration parameters.
     * @return Writer return the Writer , null in case of failure
     */
    Writer createWriter(final int id, final Parameters params);

    /**
     * Create a Single Reader.
     * @param id Reader id
     * @param params configuration parameters.
     * @return Reader return the Reader , null in case of failure
     */
    Reader createReader(final int id, final Parameters params);
}
