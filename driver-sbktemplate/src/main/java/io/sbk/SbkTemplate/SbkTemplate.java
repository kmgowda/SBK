/**
 * Copyright (c) KMG. All Rights Reserved..
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 */
package io.sbk.SbkTemplate;

import io.sbk.api.DataReader;
import io.sbk.api.DataWriter;
import io.sbk.api.ParameterOptions;
import io.sbk.api.Storage;

import java.io.IOException;

/**
 * Class for SbkTemplate storage driver.
 *
 * Incase if your data type in other than byte[] (Byte Array)
 * then change the datatype and getDataType.
 */
public class SbkTemplate implements Storage<byte[]> {

    @Override
    public void addArgs(final ParameterOptions params) throws IllegalArgumentException {
        throw new IllegalArgumentException("The SbkTemplate Driver not defined");
    }

    @Override
    public void parseArgs(final ParameterOptions params) throws IllegalArgumentException {
        throw new IllegalArgumentException("The SbkTemplate Driver not defined");
    }

    @Override
    public void openStorage(final ParameterOptions params) throws IOException {
        throw new IOException("The SbkTemplate Driver not defined");
    }

    @Override
    public void closeStorage(final ParameterOptions params) throws IOException {
        throw new IOException("The SbkTemplate Driver not defined");
    }

    @Override
    public DataWriter<byte[]> createWriter(final int id, final ParameterOptions params) {
        return new SbkTemplateWriter(id, params);
    }

    @Override
    public DataReader<byte[]> createReader(final int id, final ParameterOptions params) {
        return new SbkTemplateReader(id, params);
    }

    @Override
    public DataType<byte[]> getDataType() {
        return new ByteArray();
    }
}
