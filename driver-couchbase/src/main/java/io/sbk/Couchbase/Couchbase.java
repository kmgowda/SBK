/**
 * Copyright (c) KMG. All Rights Reserved..
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 */
package io.sbk.Couchbase;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.javaprop.JavaPropsFactory;
import io.sbk.api.DataReader;
import io.sbk.api.DataWriter;
import io.sbk.api.ParameterOptions;
import io.sbk.api.Storage;
import io.sbk.data.DataType;
import io.sbk.data.impl.ByteArray;

import java.io.IOException;
import java.util.Objects;

/**
 * Class for Couchbase storage driver.
 *
 * Incase if your data type in other than byte[] (Byte Array)
 * then change the datatype and getDataType.
 */
public class Couchbase implements Storage<byte[]> {
    private final static String CONFIGFILE = "Couchbase.properties";
    private CouchbaseConfig config;

    @Override
    public void addArgs(final ParameterOptions params) throws IllegalArgumentException {
        final ObjectMapper mapper = new ObjectMapper(new JavaPropsFactory())
                .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        try {
            config = mapper.readValue(
                    Objects.requireNonNull(Couchbase.class.getClassLoader().getResourceAsStream(CONFIGFILE)),
                    CouchbaseConfig.class);
        } catch (Exception ex) {
            ex.printStackTrace();
            throw new IllegalArgumentException(ex);
        }

        // change and uncomment the below code as per your driver specific parameters
        // params.addOption("param", true, "Couchbase parameter, default param: " + config.param);
        throw new IllegalArgumentException("The Couchbase Driver not defined");
    }

    @Override
    public void parseArgs(final ParameterOptions params) throws IllegalArgumentException {
        // change and uncommnet the below code as per your driver specific parameters
        // config.param = params.getOptionValue("param", config.param);
        throw new IllegalArgumentException("The Couchbase Driver not defined");
    }

    @Override
    public void openStorage(final ParameterOptions params) throws IOException {
        throw new IOException("The Couchbase Driver not defined");
    }

    @Override
    public void closeStorage(final ParameterOptions params) throws IOException {
        throw new IOException("The Couchbase Driver not defined");
    }

    @Override
    public DataWriter<byte[]> createWriter(final int id, final ParameterOptions params) {
        return new CouchbaseWriter(id, params, config);
    }

    @Override
    public DataReader<byte[]> createReader(final int id, final ParameterOptions params) {
        return new CouchbaseReader(id, params, config);
    }

    @Override
    public DataType<byte[]> getDataType() {
        return new ByteArray();
    }
}
