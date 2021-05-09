/**
 * Copyright (c) KMG. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 */

package io.sbk.SeaweedFS;

import io.sbk.api.DataReader;
import io.sbk.api.DataWriter;
import io.sbk.api.Parameters;
import io.sbk.api.Storage;
import seaweedfs.client.FilerGrpcClient;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.javaprop.JavaPropsFactory;

import java.io.IOException;
import java.util.Objects;

public class SeaweedFS implements Storage<byte[]> {
    private final static String CONFIGFILE = "seaweedfs.properties";
    private SeaweedFSConfig config;
    private FilerGrpcClient client;


    @Override
    public void addArgs(final Parameters params) throws IllegalArgumentException {
        final ObjectMapper mapper = new ObjectMapper(new JavaPropsFactory())
                .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        try {
            config = mapper.readValue(
                    Objects.requireNonNull(SeaweedFS.class.getClassLoader().getResourceAsStream(CONFIGFILE)),
                    SeaweedFSConfig.class);
        } catch (Exception ex) {
            ex.printStackTrace();
            throw new IllegalArgumentException(ex);
        }

        params.addOption("host", true, "host, default: " + config.host);
        params.addOption("port", true, "port, default: " + config.port);
        params.addOption("file", true, "File name, default: "+config.file);
        params.addOption("recreate", true,
                "If the file is already existing, delete and recreate the same; only for writer, default: " +
                        config.reCreate);
    }

    @Override
    public void parseArgs(final Parameters params) throws IllegalArgumentException {
        config.host = params.getOptionValue("host", config.host);
        config.port = Integer.parseInt(params.getOptionValue("port", String.valueOf(config.port)));
        config.file = params.getOptionValue("file", config.file);
        config.reCreate = Boolean.parseBoolean(params.getOptionValue("recreate", String.valueOf(config.reCreate)));
        if (params.getReadersCount() > 0 && params.getWritersCount() > 0) {
            throw new IllegalArgumentException("Specify either Writer or readers ; both are not allowed");
        }
    }

    @Override
    public void openStorage(final Parameters params) throws IOException {
        client = new FilerGrpcClient(config.host, config.port);
    }

    @Override
    public void closeStorage(final Parameters params) throws IOException {

    }

    @Override
    public DataWriter<byte[]> createWriter(final int id, final Parameters params) {
        try {
            return new SeaweedFSWriter(id, params, client, config);
        } catch (IOException ex) {
            ex.printStackTrace();
            return null;
        }
    }

    @Override
    public DataReader<byte[]> createReader(final int id, final Parameters params) {
        try {
            return new SeaweedFSReader(id, params, client, config);
        } catch (IOException ex) {
            ex.printStackTrace();
            return null;
        }
    }
}