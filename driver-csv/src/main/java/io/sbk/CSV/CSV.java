/**
 * Copyright (c) KMG. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 */
package io.sbk.CSV;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.javaprop.JavaPropsFactory;
import io.sbk.api.DataReader;
import io.sbk.data.DataType;
import io.sbk.api.DataWriter;
import io.sbk.api.ParameterOptions;
import io.sbk.api.Storage;
import io.sbk.data.impl.StringHandler;

import java.io.IOException;
import java.util.Objects;

/**
 * Class for File System Benchmarking using File Channel.
 */
public class CSV implements Storage<String> {
    private final static String CONFIGFILE = "csv.properties";
    private CSVConfig config;

    @Override
    public void addArgs(final ParameterOptions params) throws IllegalArgumentException {
        final ObjectMapper mapper = new ObjectMapper(new JavaPropsFactory())
                .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        try {
            config = mapper.readValue(
                    Objects.requireNonNull(CSV.class.getClassLoader().getResourceAsStream(CONFIGFILE)),
                    CSVConfig.class);
        } catch (Exception ex) {
            ex.printStackTrace();
            throw new IllegalArgumentException(ex);
        }
        params.addOption("file", true, "File name, default file name : "+config.fileName);
    }

    @Override
    public void parseArgs(final ParameterOptions params) throws IllegalArgumentException {
        config.fileName =  params.getOptionValue("file", config.fileName);
        if (params.getWritersCount() > 1) {
            throw new IllegalArgumentException("Writers should be only 1 for File writing");
        }
        if (params.getReadersCount() > 0 && params.getWritersCount() > 0) {
            throw new IllegalArgumentException("Specify either Writer or readers ; both are not allowed");
        }
    }

    @Override
    public void openStorage(final ParameterOptions params) throws  IOException {
    }

    @Override
    public void closeStorage(final ParameterOptions params) throws IOException {

    }

    @Override
    public DataWriter<String> createWriter(final int id, final ParameterOptions params) {
        try {
            return new CSVWriter(id, params, config);
        } catch (IOException ex) {
            ex.printStackTrace();
            return null;
        }
    }

    @Override
    public DataReader<String> createReader(final int id, final ParameterOptions params) {
        try {
            return new CSVReader(id, params, config);
        } catch (IOException ex) {
            ex.printStackTrace();
            return null;
        }
    }

    @Override
    public DataType<String> getDataType() {
        return new StringHandler();
    }
}



