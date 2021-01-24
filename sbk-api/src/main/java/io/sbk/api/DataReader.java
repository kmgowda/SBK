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

import java.io.EOFException;
import java.io.IOException;

/**
 * Interface for Basic Data Readers.
 */
public interface DataReader<T> {

    /**
     * Close the  Reader.
     * @throws IOException If an exception occurred.
     */
    void close() throws IOException;

    /**
     * Benchmarking reader by reading given number of records.
     *
     * @param reader  Reader Descriptor
     * @param dType  dataType
     * @param time  time interface
     * @throws EOFException If the End of the file occurred.
     * @throws IOException If an exception occurred.
     */
    void RecordsReader(Worker reader, DataType<T> dType, Time time) throws EOFException, IOException;

    /**
     * Benchmarking reader by reading given number of records.
     * used while another writer is writing the data.
     *
     * @param reader      Reader Descriptor
     * @param dType     dataType
     * @param time  time interface
     * @throws EOFException If the End of the file occurred.
     * @throws IOException If an exception occurred.
     */
    void RecordsReaderRW(Worker reader, DataType<T> dType, Time time) throws EOFException, IOException;

    /**
     * Benchmarking reader by reading events/records for specific time duration.
     *
     * @param reader  Reader Descriptor
     * @param dType  dataType
     * @param time  time interface
     * @throws EOFException If the End of the file occurred.
     * @throws IOException If an exception occurred.
     */
    void RecordsTimeReader(Worker reader, DataType<T> dType, Time time) throws EOFException, IOException;

    /**
     * Benchmarking reader by reading events/records for specific time duration.
     * used while another writer is writing the data.
     *
     * @param reader  Reader Descriptor
     * @param dType  dataType
     * @param time  time interface
     * @throws EOFException If the End of the file occurred.
     * @throws IOException If an exception occurred.
     */
    void RecordsTimeReaderRW(Worker reader, DataType<T> dType, Time time) throws EOFException, IOException;


    /**
     * Benchmarking reader by reading given number of records with Rate controlled.
     *
     * @param reader  Reader Descriptor
     * @param dType  dataType
     * @param time  time interface
     * @param rController Rate Controller
     * @throws EOFException If the End of the file occurred.
     * @throws IOException If an exception occurred.
     */
    void RecordsReaderRateControl(Worker reader, DataType<T> dType, Time time, RateController rController) throws EOFException, IOException;

    /**
     * Benchmarking reader by reading given number of records with Rate controlled.
     * used while another writer is writing the data.
     *
     * @param reader      Reader Descriptor
     * @param dType     dataType
     * @param time  time interface
     * @param rController Rate Controller
     * @throws EOFException If the End of the file occurred.
     * @throws IOException If an exception occurred.
     */
    void RecordsReaderRWRateControl(Worker reader, DataType<T> dType, Time time, RateController rController) throws EOFException, IOException;

    /**
     * Benchmarking reader by reading events/records for specific time duration with Rate controlled.
     *
     * @param reader  Reader Descriptor
     * @param dType  dataType
     * @param time  time interface
     * @param rController Rate Controller
     * @throws EOFException If the End of the file occurred.
     * @throws IOException If an exception occurred.
     */
    void RecordsTimeReaderRateControl(Worker reader, DataType<T> dType, Time time, RateController rController) throws EOFException, IOException;

    /**
     * Benchmarking reader by reading events/records for specific time duration with Rate controlled.
     * used while another writer is writing the data.
     *
     * @param reader  Reader Descriptor
     * @param dType  dataType
     * @param time  time interface
     * @param rController Rate Controller
     * @throws EOFException If the End of the file occurred.
     * @throws IOException If an exception occurred.
     */
    void RecordsTimeReaderRWRateControl(Worker reader, DataType<T> dType, Time time, RateController rController) throws EOFException, IOException;
}

