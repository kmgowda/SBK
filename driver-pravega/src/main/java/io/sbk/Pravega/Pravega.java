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

import io.sbk.api.Benchmark;
import io.sbk.api.Parameters;
import io.sbk.api.Writer;
import io.sbk.api.Reader;

import java.io.IOException;
import java.net.URI;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

import io.pravega.client.ClientConfig;
import io.pravega.client.stream.ReaderGroup;
import io.pravega.client.stream.impl.ControllerImpl;
import io.pravega.client.stream.impl.ControllerImplConfig;
import io.pravega.client.EventStreamClientFactory;

/**
 * class for Pravega benchmarking.
 */
public class Pravega implements Benchmark<byte[]> {
    static final String DEFAULT_SCOPE = "Scope";
    private String scopeName;
    private String streamName;
    private String rdGrpName;
    private String controllerUri;
    private int segmentCount;
    private boolean recreate;
    private PravegaStreamHandler streamHandle;
    private EventStreamClientFactory factory;
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
            recreate = params.getWritersCount() > 0 && params.getReadersCount() > 0;
        }
        if (controllerUri == null) {
            throw new IllegalArgumentException("Error: Must specify Controller IP address");
        }

        if (streamName == null) {
            throw new IllegalArgumentException("Error: Must specify stream Name");
        }
        if (recreate) {
            rdGrpName = streamName + params.getStartTime();
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
                    segmentCount, params.getTimeout(), controller,
                    bgExecutor);

            if (params.getWritersCount() > 0 && !streamHandle.create()) {
                if (recreate) {
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

            factory = EventStreamClientFactory.withScope(scopeName, ClientConfig.builder()
                                        .controllerURI(new URI(controllerUri)).build());
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
    public Writer createWriter(final int id, final Parameters params) {
        try {
            return new PravegaWriter(id, params, streamName, factory);
        } catch (IOException ex) {
            ex.printStackTrace();
            return null;
        }

    }

    @Override
    public Reader createReader(final int id, final Parameters params) {
        try {
            return new PravegaReader(id, params, streamName, rdGrpName, factory);
        } catch (IOException ex) {
            ex.printStackTrace();
            return null;
        }
    }
}
