/**
 * Copyright (c) KMG. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.sbk.api;

import com.google.common.reflect.TypeToken;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.sbk.data.DataType;
import io.sbk.data.impl.ByteArray;
import io.sbk.params.InputOptions;
import io.sbk.params.ParameterOptions;

import java.io.IOException;
import java.lang.reflect.Type;
import java.util.Objects;

/**
 * Interface for Benchmarking.
 */
public interface Storage<T> {

    /**
     * Add the driver specific command line arguments.
     *
     * @param params Parameters object to be extended.
     * @throws IllegalArgumentException If an exception occurred.
     */
    void addArgs(final InputOptions params) throws IllegalArgumentException;

    /**
     * Parse the driver specific command line arguments.
     *
     * @param params Parameters object to be parsed for driver specific parameters/arguments.
     * @throws IllegalArgumentException If an exception occurred.
     */
    void parseArgs(final ParameterOptions params) throws IllegalArgumentException;

    /**
     * Open the storage device / client to perform the benchmarking.
     *
     * @param params Parameters object enclosing all commandline arguments,
     *               see {@link ParameterOptions} to get the basic benchmarking parameters.
     * @throws IOException If an exception occurred.
     */
    void openStorage(final ParameterOptions params) throws IOException;

    /**
     * Close the Storage device / client.
     *
     * @param params Parameters object enclosing all commandline arguments,
     *               see {@link ParameterOptions} to get the basic benchmarking parameters.
     * @throws IOException If an exception occurred.
     */
    void closeStorage(final ParameterOptions params) throws IOException;

    /**
     * Create a Single Data Writer / Producer.
     *
     * @param id     Writer id
     * @param params Parameters object enclosing all commandline arguments,
     *               see {@link ParameterOptions} to get the basic benchmarking parameters.
     * @return Writer return the Writer , null in case of failure
     * @throws IOException If an exception occurred.
     */
    DataWriter<T> createWriter(final int id, final ParameterOptions params) throws IOException;

    /**
     * Create a Single Reader / Consumer.
     *
     * @param id     Reader id
     * @param params Parameters object enclosing all commandline arguments,
     *               see {@link ParameterOptions} to get the basic benchmarking parameters.
     * @return Reader return the Reader , null in case of failure
     * @throws IOException If an exception occurred.
     */
    DataReader<T> createReader(final int id, final ParameterOptions params) throws IOException;


    /**
     * Default implementation to create a payload or data to write/read.
     * Default data type is byte[].
     * If your Benchmark data type is other than byte[] then you need to implement your own Data class.
     * If the data type of your Benchmark, Reader and Writer classes  is byte[] (Byte Array),
     * then use this default implementation as it is.
     *
     * @return Data Data interface.
     * @throws IllegalArgumentException if data type is other than byte[]
     */
    @SuppressFBWarnings("NP_NULL_ON_SOME_PATH_FROM_RETURN_VALUE")
    @SuppressWarnings("unchecked")
    default DataType<T> getDataType() throws IllegalArgumentException {
        final TypeToken<T> typeToken = new TypeToken<T>(getClass()) {
        };
        final Type type = Objects.requireNonNull(typeToken.getComponentType()).getType();
        if (type.getTypeName().equals("byte")) {
            return (DataType<T>) new ByteArray();
        } else {
            throw new IllegalArgumentException(
                    "The data type is your class which implements Benchmark interface is not byte[]" +
                    ", Override/Implement the 'dataType' method");
        }
    }

}
