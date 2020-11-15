/**
 * Copyright (c) KMG. All Rights Reserved..
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 */

package io.sbk.Pravega;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.javaprop.JavaPropsFactory;
import io.sbk.api.Storage;
import io.sbk.api.Parameters;
import io.sbk.api.Writer;
import io.sbk.api.Reader;

import java.io.IOException;
import java.net.URI;
import java.util.Objects;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

import io.pravega.client.ClientConfig;
import io.pravega.client.stream.ReaderGroup;
import io.pravega.client.stream.impl.ControllerImpl;
import io.pravega.client.stream.impl.ControllerImplConfig;
import io.pravega.client.EventStreamClientFactory;

/**
 * Class for Pravega benchmarking.
 */
public class Pravega implements Storage<byte[]> {
    private final static String CONFIGFILE = "pravega.properties";
    private PravegaConfig config;
    private PravegaStreamHandler streamHandle;
    private EventStreamClientFactory factory;
    private ReaderGroup readerGroup;
    private String rdGrpName;


    @Override
    public void addArgs(final Parameters params) throws IllegalArgumentException {
        final ObjectMapper mapper = new ObjectMapper(new JavaPropsFactory())
                .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        try {
            config = mapper.readValue(Objects.requireNonNull(Pravega.class.getClassLoader().getResourceAsStream(CONFIGFILE)),
                    PravegaConfig.class);
        } catch (Exception ex) {
            ex.printStackTrace();
            throw new IllegalArgumentException(ex);
        }

        params.addOption("scope", true, "Scope name, default :" + config.scopeName);
        params.addOption("stream", true, "Stream name, default :" + config.streamName);
        params.addOption("controller", true, "Controller URI, default :" + config.controllerUri);
        params.addOption("segments", true, "Number of segments, default :" + config.segmentCount);
        params.addOption("recreate", true,
                "If the stream is already existing, delete and recreate the same, default :" + config.recreate);
    }

    @Override
    public void parseArgs(final Parameters params) throws IllegalArgumentException {
        config.scopeName = params.getOptionValue("scope", config.scopeName);
        config.streamName =  params.getOptionValue("stream", config.streamName);
        config.controllerUri = params.getOptionValue("controller", config.controllerUri);
        config.segmentCount = Integer.parseInt(params.getOptionValue("segments", Integer.toString(config.segmentCount)));
        if (params.hasOption("recreate")) {
            config.recreate = Boolean.parseBoolean(params.getOptionValue("recreate"));
        } else {
            config.recreate = params.getWritersCount() > 0 && params.getReadersCount() > 0;
        }

        if (config.recreate) {
            rdGrpName = config.streamName + System.currentTimeMillis();
        } else {
            rdGrpName = config.streamName + "RdGrp";
        }

    }

    @Override
    public void openStorage(final Parameters params) throws IOException {
        try {
            final ScheduledExecutorService bgExecutor = Executors.newScheduledThreadPool(10);
            final ControllerImpl controller = new ControllerImpl(ControllerImplConfig.builder()
                    .clientConfig(ClientConfig.builder()
                            .controllerURI(new URI(config.controllerUri)).build())
                    .maxBackoffMillis(5000).build(),
                    bgExecutor);

            streamHandle = new PravegaStreamHandler(config.scopeName, config.streamName, rdGrpName,
                    config.controllerUri, config.segmentCount, params.getTimeout(), controller,
                    bgExecutor);

            if (params.getWritersCount() > 0 && !streamHandle.create()) {
                if (config.recreate) {
                    streamHandle.recreate();
                } else {
                    streamHandle.scale();
                }
            }
            if (params.getReadersCount() > 0) {
                readerGroup = streamHandle.createReaderGroup(!params.isWriteAndRead());
            } else {
                readerGroup = null;
            }

            factory = EventStreamClientFactory.withScope(config.scopeName, ClientConfig.builder()
                                        .controllerURI(new URI(config.controllerUri)).build());
        } catch (Exception ex) {
             throw new IOException(ex);
        }
    }

    @Override
    public  void closeStorage(final Parameters params) throws IOException {
        if (readerGroup != null) {
            readerGroup.close();
        }
    }

    @Override
    public Writer<byte[]> createWriter(final int id, final Parameters params) {
        try {
            return new PravegaWriter(id, params, config.streamName, factory);
        } catch (IOException ex) {
            ex.printStackTrace();
            return null;
        }

    }

    @Override
    public Reader<byte[]> createReader(final int id, final Parameters params) {
        try {
            return new PravegaReader(id, params, config.streamName, rdGrpName, factory);
        } catch (IOException ex) {
            ex.printStackTrace();
            return null;
        }
    }
}
