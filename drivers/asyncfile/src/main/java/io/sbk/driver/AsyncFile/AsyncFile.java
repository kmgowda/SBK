/**
 * Copyright (c) KMG. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 */
package io.sbk.driver.AsyncFile;

import io.sbk.api.DataReader;
import io.sbk.api.DataWriter;
import io.sbk.params.ParameterOptions;
import io.sbk.api.Storage;
import io.sbk.data.DataType;
import io.sbk.data.impl.NioByteBuffer;
import io.sbk.params.InputOptions;

import java.io.IOException;
import java.nio.ByteBuffer;

/**
 * Class for Asynchronous File System Benchmarking.
 */
public class AsyncFile implements Storage<ByteBuffer> {
    private String fileName;

    @Override
    public void addArgs(final InputOptions params) throws IllegalArgumentException {
        params.addOption("file", true, "File name");
    }

    @Override
    public void parseArgs(final ParameterOptions params) throws IllegalArgumentException {
        fileName = params.getOptionValue("file", null);

        if (fileName == null) {
            throw new IllegalArgumentException("Error: Must specify file Name");
        }
        if (params.getWritersCount() > 1) {
            throw new IllegalArgumentException("Writers should be only 1 for File writing");
        }
        if (params.getReadersCount() > 0 && params.getWritersCount() > 0) {
            throw new IllegalArgumentException("Specify either Writer or readers ; both are not allowed");
        }
    }

    @Override
    public void openStorage(final ParameterOptions params) throws IOException {

    }

    @Override
    public void closeStorage(final ParameterOptions params) throws IOException {

    }

    @Override
    public DataWriter<ByteBuffer> createWriter(final int id, final ParameterOptions params) {
        try {
            return new AsyncFileWriter(id, params, fileName);
        } catch (IOException ex) {
            ex.printStackTrace();
            return null;
        }
    }

    @Override
    public DataReader<ByteBuffer> createReader(final int id, final ParameterOptions params) {
        try {
            return new AsyncFileReader(id, params, fileName);
        } catch (IOException ex) {
            ex.printStackTrace();
            return null;
        }
    }

    @Override
    public DataType<ByteBuffer> getDataType() {
        return new NioByteBuffer();
    }
}



