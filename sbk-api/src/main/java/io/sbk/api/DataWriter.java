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
 * Interface for Basic Data Writers.
 */
public interface DataWriter<T> {


    /**
     * Close the  Writer.
     * @throws IOException If an exception occurred.
     */
    void close() throws IOException;

    /**
     * writer benchmarking by writing given number of records.
     *
     * @param writer Writer Descriptor
     * @param dType  Data Type interface
     * @param data  data to write
     * @param size  size of the data
     * @param time  time interface
     * @throws IOException If an exception occurred.
     */
    void RecordsWriter(Worker writer, DataType<T> dType, T data, int size, Time time) throws IOException;

    /**
     * Writer benchmarking by writing given number of records and data should synced is invoked after writing given set of records.
     *
     * @param writer Writer Descriptor
     * @param dType  Data Type interface
     * @param data  data to write
     * @param size  size of the data
     * @param time  time interface
     * @param rController Rate Controller
     * @throws IOException If an exception occurred.
     */
    void RecordsWriterSync(Worker writer, DataType<T> dType, T data, int size, Time time, RateController rController) throws IOException;

    /**
     * Writer benchmarking by continuously writing data records for specific time duration.
     * sync is invoked after writing records for given time.
     *
     * @param writer Writer Descriptor
     * @param dType  Data Type interface
     * @param data  data to write
     * @param size  size of the data
     * @param time  time interface
     * @throws IOException If an exception occurred.
     */
    void RecordsWriterTime(Worker writer, DataType<T> dType, T data, int size, Time time) throws IOException;

    /**
     * writer benchmarking by continuously writing data records for specific time duration.
     * sync is invoked after writing given set of records.
     *
     * @param writer Writer Descriptor
     * @param dType  Data Type interface
     * @param data  data to write
     * @param size  size of the data
     * @param time  time interface
     * @param rController Rate Controller
     * @throws IOException If an exception occurred.
     */
    void RecordsWriterTimeSync(Worker writer, DataType<T> dType, T data, int size,
                                       Time time, RateController rController) throws IOException;

    /**
     * Write given number of records. No Writer Benchmarking is performed.
     *
     * @param writer Writer Descriptor
     * @param dType  Data Type interface
     * @param data  data to write
     * @param size  size of the data
     * @param time  time interface
     * @param rController Rate Controller
     * @throws IOException If an exception occurred.
     */
    void RecordsWriterRW(Worker writer, DataType<T> dType, T data, int size, Time time, RateController rController) throws IOException;

    /**
     * Writing data records for specific time duration. No Writer Benchmarking is performed.
     *
     * @param writer Writer Descriptor
     * @param dType  Data Type interface
     * @param data  data to write
     * @param size  size of the data
     * @param time  time interface
     * @param rController Rate Controller
     * @throws IOException If an exception occurred.
     */
    void RecordsWriterTimeRW(Worker writer, DataType<T> dType, T data, int size,
                                     Time time, RateController rController) throws IOException;
}
