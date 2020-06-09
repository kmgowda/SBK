/**
 * Copyright (c) KMG. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 */
package io.sbk.FileStream;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.javaprop.JavaPropsFactory;
import io.sbk.api.Storage;
import io.sbk.api.Parameters;
import io.sbk.api.Writer;
import io.sbk.api.Reader;

import java.io.IOException;
import java.util.Objects;

/**
 * Class for File System Benchmarking.
 */
public class FileStream implements Storage<byte[]> {
    private final static String CONFIGFILE = "filestream.properties";
    private FileStreamConfig config;

    @Override
    public void addArgs(final Parameters params) throws IllegalArgumentException {
        final ObjectMapper mapper = new ObjectMapper(new JavaPropsFactory())
                .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        try {
            config = mapper.readValue(Objects.requireNonNull(FileStream.class.getClassLoader().getResourceAsStream(CONFIGFILE)),
                    FileStreamConfig.class);
        } catch (Exception ex) {
            ex.printStackTrace();
            throw new IllegalArgumentException(ex);
        }

        params.addOption("file", true, "File name, default file name : "+config.fileName);
        params.addOption("append", true, "Append writes, default : "+config.isAppend);
        params.addOption("recreate", true,
                "if file exists, delete and create it for write operation, default : "+config.reCreate);
        params.addOption("buffer", true, "Buffered Write/Read , default: "+config.isBuffered);

    }

    @Override
    public void parseArgs(final Parameters params) throws IllegalArgumentException {
        if (params.getWritersCount() > 1) {
            throw new IllegalArgumentException("Writers should be only 1 for File writing");
        }
        if (params.getReadersCount() > 0 && params.getWritersCount() > 0) {
            throw new IllegalArgumentException("Specify either Writer or readers ; both are not allowed");
        }
        config.fileName =  params.getOptionValue("file", config.fileName);
        config.isAppend =  Boolean.parseBoolean(params.getOptionValue("append", String.valueOf(config.isAppend)));
        config.reCreate =  Boolean.parseBoolean(params.getOptionValue("recreate", String.valueOf(config.reCreate)));
        config.isBuffered =  Boolean.parseBoolean(params.getOptionValue("buffer", String.valueOf(config.isBuffered)));
    }

    @Override
    public void openStorage(final Parameters params) throws  IOException {
        if (config.reCreate && params.getWritersCount() > 0) {
            java.io.File file = new java.io.File(config.fileName);
            file.delete();
        }
    }

    @Override
    public void closeStorage(final Parameters params) throws IOException {

    }

    @Override
    public Writer<byte[]> createWriter(final int id, final Parameters params) {
        try {
            if (config.isBuffered) {
                return new FileBufferedWriter(id, params, config);
            } else {
                return new FileStreamWriter(id, params, config);
            }
        } catch (IOException ex) {
            ex.printStackTrace();
            return null;
        }
    }

    @Override
    public Reader<byte[]> createReader(final int id, final Parameters params) {
        try {
            if (config.isBuffered) {
                return new FileBufferedReader(id, params, config);
            } else {
                return new FileStreamReader(id, params, config);
            }
        } catch (IOException ex) {
            ex.printStackTrace();
            return null;
        }
    }
}
