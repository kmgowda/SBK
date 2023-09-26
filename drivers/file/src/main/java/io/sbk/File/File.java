/**
 * Copyright (c) KMG. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 */
package io.sbk.File;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.javaprop.JavaPropsFactory;
import io.sbk.api.DataReader;
import io.sbk.api.DataWriter;
import io.sbk.params.ParameterOptions;
import io.sbk.api.Storage;
import io.sbk.data.DataType;
import io.sbk.data.impl.NioByteBuffer;
import io.sbk.params.InputOptions;
import io.sbk.system.Printer;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Objects;

/**
 * Class for File System Benchmarking using File Channel.
 */
public class File implements Storage<ByteBuffer> {
    private final static String CONFIGFILE = "file.properties";
    private FileConfig config;
    private DataType<ByteBuffer> dType;

    @Override
    public void addArgs(final InputOptions params) throws IllegalArgumentException {
        final ObjectMapper mapper = new ObjectMapper(new JavaPropsFactory())
                .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        try {
            config = mapper.readValue(
                    Objects.requireNonNull(File.class.getClassLoader().getResourceAsStream(CONFIGFILE)),
                    FileConfig.class);
        } catch (Exception ex) {
            ex.printStackTrace();
            throw new IllegalArgumentException(ex);
        }
        params.addOption("file", true, "File name, default file name : " + config.fileName);
        params.addOption("asyncthreads", true, "Asynchronous File Read mode, Default : " + config.asyncThreads);
    }

    @Override
    public void parseArgs(final ParameterOptions params) throws IllegalArgumentException {
        config.fileName = params.getOptionValue("file", config.fileName);
        config.asyncThreads = Integer.parseInt(params.getOptionValue("asyncthreads", Integer.toString(config.asyncThreads)));
        if (params.getWritersCount() > 1) {
            throw new IllegalArgumentException("Writers should be only 1 for File writing");
        }
        if (params.getReadersCount() > 0 && params.getWritersCount() > 0) {
            throw new IllegalArgumentException("Specify either Writer or readers ; both are not allowed");
        }
    }

    @Override
    public void openStorage(final ParameterOptions params) throws IOException {
        if (config.reCreate && params.getWritersCount() > 0) {
            java.io.File file = new java.io.File(config.fileName);
            file.delete();
        }
    }

    @Override
    public void closeStorage(final ParameterOptions params) throws IOException {

    }

    @Override
    public DataWriter<ByteBuffer> createWriter(final int id, final ParameterOptions params) throws IOException {
        return new FileWriter(id, params, config);
    }

    @Override
    public DataReader<ByteBuffer> createReader(final int id, final ParameterOptions params) throws IOException {
        if (config.asyncThreads > 0) {
            Printer.log.warn("Asynchronous File Reader initiated !");
            return new FileAsyncReader(id, params, dType, config);
        } else {
            Printer.log.info("Synchronous File Reader initiated !");
            return new FileReader(id, params, dType, config);
        }
    }

    @Override
    public DataType<ByteBuffer> getDataType() {
        dType = new NioByteBuffer();
        return dType;
    }
}



