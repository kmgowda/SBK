/**
 * Copyright (c) 2020 KMG. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 */

package io.perf.drivers.Pravega;

import io.perf.core.Parameters;
import io.perf.core.Reader;

import io.perf.core.TriConsumer;
import io.pravega.client.stream.EventStreamReader;
import io.pravega.client.ClientFactory;
import io.pravega.client.stream.impl.ByteArraySerializer;
import io.pravega.client.stream.ReaderConfig;
import io.pravega.client.stream.ReinitializationRequiredException;

import java.io.IOException;

/**
 * Class for Pravega reader/consumer.
 */
public class PravegaReader extends Reader {
    private final EventStreamReader<byte[]> reader;

    public PravegaReader(int readerId, TriConsumer recordTime, Parameters params,
                         String streamName, String readergrp, ClientFactory factory) throws IOException {
        super(readerId, recordTime, params);

        final String readerSt = Integer.toString(readerId);
        reader = factory.createReader(
                readerSt, readergrp, new ByteArraySerializer(), ReaderConfig.builder().build());
    }

    @Override
    public byte[] read() throws IOException {
        try {
            return reader.readNextEvent(params.timeout).getEvent();
        } catch (ReinitializationRequiredException e) {
            throw new IOException(e);
        }
    }

    @Override
    public void close() {
        reader.close();
    }
}
