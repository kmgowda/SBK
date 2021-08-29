/**
 * Copyright (c) KMG. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.sbk.Pravega;

import io.pravega.client.EventStreamClientFactory;
import io.pravega.client.stream.EventStreamReader;
import io.pravega.client.stream.ReaderConfig;
import io.pravega.client.stream.ReinitializationRequiredException;
import io.pravega.client.stream.impl.ByteArraySerializer;
import io.sbk.api.ParameterOptions;
import io.sbk.api.Reader;

import java.io.IOException;


/**
 * Class for Pravega reader/consumer.
 */
public class PravegaReader implements Reader<byte[]> {
    private final ParameterOptions params;
    private final EventStreamReader<byte[]> reader;

    public PravegaReader(int id, ParameterOptions params, String streamName,
                         String readergrp, EventStreamClientFactory factory) throws IOException {
        final String readerSt = Integer.toString(id);
        this.params = params;
        reader = factory.createReader(readerSt, readergrp,
                new ByteArraySerializer(), ReaderConfig.builder().build());
    }

    @Override
    public byte[] read() throws IOException {
        try {
            return reader.readNextEvent(params.getTimeoutMS()).getEvent();
        } catch (ReinitializationRequiredException e) {
            throw new IOException(e);
        }
    }

    @Override
    public void close() {
        reader.close();
    }
}
