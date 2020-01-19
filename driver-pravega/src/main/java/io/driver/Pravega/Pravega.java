/**
 * Copyright (c) 2020 KMG. All Rights Reserved..
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 */


package io.driver.Pravega;

import io.dsb.api.Benchmark;
import io.dsb.api.Parameters;
import io.dsb.api.QuadConsumer;
import io.dsb.api.Writer;
import io.dsb.api.Reader;

import java.io.IOException;
import java.net.URI;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

import io.pravega.client.ClientConfig;
import io.pravega.client.ClientFactory;
import io.pravega.client.stream.ReaderGroup;
import io.pravega.client.stream.impl.ControllerImpl;
import io.pravega.client.stream.impl.ControllerImplConfig;
import io.pravega.client.stream.impl.ClientFactoryImpl;


/**
 * Abstract class for Benchmarking.
 */
public class Pravega extends Benchmark {
    static final String DEFAULT_SCOPE = "Scope";
    private String scopeName;
    private String streamName;
    private String rdGrpName;
    private String controllerUri;
    private int segmentCount;
    private boolean recreate;
    private PravegaStreamHandler streamHandle;
    private ClientFactory factory;
    private ReaderGroup readerGroup;

    @Override
    public void addArgs(final Parameters params) {
        params.addOption("scope", true, "Scope name");
        params.addOption("stream", true, "Stream name");
        params.addOption("controller", true, "Controller URI");
        params.addOption("segments", true, "Number of segments");
        params.addOption("recreate", true,
                "If the stream is already existing, delete and recreate the same");
    }

    @Override
    public void parseArgs(final Parameters params) throws IllegalArgumentException {
        scopeName = params.getOptionValue("scope", DEFAULT_SCOPE);
        streamName =  params.getOptionValue("stream", null);
        controllerUri = params.getOptionValue("controller", null);
        segmentCount = Integer.parseInt(params.getOptionValue("segments", "1"));
        if (params.hasOption("recreate")) {
            recreate = Boolean.parseBoolean(params.getOptionValue("recreate"));
        } else {
            recreate = params.writersCount > 0 && params.readersCount > 0;
        }
        if (controllerUri == null) {
            throw new IllegalArgumentException("Error: Must specify Controller IP address");
        }

        if (streamName == null) {
            throw new IllegalArgumentException("Error: Must specify stream Name");
        }
        if (recreate) {
            rdGrpName = streamName + params.startTime;
        } else {
            rdGrpName = streamName + "RdGrp";
        }

    }

    @Override
    public void openStorage(final Parameters params) throws IOException {
        try {
            final ScheduledExecutorService bgExecutor = Executors.newScheduledThreadPool(10);
            final ControllerImpl controller = new ControllerImpl(ControllerImplConfig.builder()
                    .clientConfig(ClientConfig.builder()
                            .controllerURI(new URI(controllerUri)).build())
                    .maxBackoffMillis(5000).build(),
                    bgExecutor);

            streamHandle = new PravegaStreamHandler(scopeName, streamName, rdGrpName, controllerUri,
                    segmentCount, params.timeout, controller,
                    bgExecutor);

            if (params.writersCount > 0 && !streamHandle.create()) {
                if (recreate) {
                    streamHandle.recreate();
                } else {
                    streamHandle.scale();
                }
            }
            if (params.readersCount > 0) {
                readerGroup = streamHandle.createReaderGroup(!params.writeAndRead);
            } else {
                readerGroup = null;
            }

            factory = new ClientFactoryImpl(scopeName, controller);
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
    public Writer createWriter(final int id, QuadConsumer recordTime, final Parameters params) {
        try {
            return new PravegaWriter(id, recordTime, params, streamName, factory);
        } catch (IOException ex) {
            ex.printStackTrace();
            return null;
        }

    }

    @Override
    public Reader createReader(final int id, QuadConsumer recordTime, final Parameters params) {
        try {
            return new PravegaReader(id, recordTime, params, streamName, rdGrpName, factory);
        } catch (IOException ex) {
            ex.printStackTrace();
            return null;
        }
    }
}
