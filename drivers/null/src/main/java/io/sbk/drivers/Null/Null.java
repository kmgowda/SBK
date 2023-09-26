/**
 * Copyright (c) KMG. All Rights Reserved..
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 */
package io.sbk.drivers.Null;

import io.sbk.api.DataReader;
import io.sbk.api.DataWriter;
import io.sbk.params.ParameterOptions;
import io.sbk.api.Storage;
import io.sbk.params.InputOptions;

import java.io.IOException;

/**
 * Class for Null Storage driver.
 */
public class Null implements Storage<byte[]> {
    private long n;
    private int timeoutMS;

    public Null() {
        n = 0;
        timeoutMS = Integer.MAX_VALUE;
    }

    @Override
    public void addArgs(final InputOptions params) throws IllegalArgumentException {
        params.addOption("n", true, "iteration loop max value for writers, default value: " + n);
        params.addOption("timeout", true, "max timeout in milli seconds per writer/reader , default : "+ timeoutMS);
    }

    @Override
    public void parseArgs(final ParameterOptions params) throws IllegalArgumentException {
        n = Long.parseLong(params.getOptionValue("n", "0"));
        timeoutMS = Integer.parseInt(params.getOptionValue("timeout", String.valueOf(timeoutMS)));
    }

    @Override
    public void openStorage(final ParameterOptions params) throws IOException {
    }

    @Override
    public void closeStorage(final ParameterOptions params) throws IOException {
    }

    @Override
    public DataWriter<byte[]> createWriter(final int id, final ParameterOptions params) {
        return new NullWriter(timeoutMS, n);
    }

    @Override
    public DataReader<byte[]> createReader(final int id, final ParameterOptions params) {
        return new NullReader(timeoutMS);
    }
}