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
import io.sbk.api.impl.ByteArray;
import java.io.IOException;
import com.google.common.reflect.TypeToken;
import java.lang.reflect.Type;

/**
 * Interface for Benchmarking.
 */
public interface Benchmark<T> {

    /**
     * Add the driver specific command line arguments.
     * @param params Parameters object to be extended.
     */
    void addArgs(final Parameters params);

    /**
     * Parse the driver specific command line arguments.
     * @param params Parameters object to be parsed for driver specific parameters/arguments.
     * @throws IllegalArgumentException If an exception occurred.
     */
    void parseArgs(final Parameters params) throws IllegalArgumentException;

    /**
     * Open the storage device / client to perform the benchmarking.
     * @param params Parameters object enclosing all commandline arguments,
     *              see {@link io.sbk.api.Parameters} to get the basic benchmarking parameters.
     * @throws IOException If an exception occurred.
     */
    void openStorage(final Parameters params) throws IOException;

    /**
     * Close the Storage device / client.
     * @param params Parameters object enclosing all commandline arguments,
     *              see {@link io.sbk.api.Parameters} to get the basic benchmarking parameters.
     * @throws IOException If an exception occurred.
     */
    void closeStorage(final Parameters params) throws IOException;

    /**
     * Create a Single Writer / Producer.
     * @param id Writer id
     * @param params Parameters object enclosing all commandline arguments,
     *              see {@link io.sbk.api.Parameters} to get the basic benchmarking parameters.
     * @return Writer return the Writer , null in case of failure
     */
    Writer<T> createWriter(final int id, final Parameters params);

    /**
     * Create a Single Reader / Consumer.
     * @param id Reader id
     * @param params Parameters object enclosing all commandline arguments,
     *              see {@link io.sbk.api.Parameters} to get the basic benchmarking parameters.
     * @return Reader return the Reader , null in case of failure
     */
    Reader<T> createReader(final int id, final Parameters params);

    /**
     * Default implementation to create a payload or data to write/read.
     * Default data type is byte[].
     * If your Benchmark type <T> is other than byte[] then you need to implement your own Data class.
     * If the data type of your Benchmark, Reader and Writer classes  is byte[] (Byte Array),
     * then use this default implementation as it is.
     * @return Data Data interface, null in case of failure
     * @throws IllegalArgumentException if data type is other than byte[]
     */
    default DataType getDataType() throws IllegalArgumentException {
        final TypeToken<T> typeToken = new TypeToken<T>(getClass()) { };
        final Type type = typeToken.getComponentType().getType();
        if (type.getTypeName().equals("byte")) {
            return new ByteArray();
        } else {
            throw new IllegalArgumentException("The data type is your class which implements Benchmark interface is not byte[]"+
                    ", Override/Implement the 'dataType' method");
        }
    }
}
