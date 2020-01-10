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
import io.perf.core.TriConsumer;
import io.perf.core.Writer;

import java.io.IOException;
import java.util.concurrent.CompletableFuture;

import io.pravega.client.stream.EventStreamWriter;
import io.pravega.client.ClientFactory;
import io.pravega.client.stream.impl.ByteArraySerializer;
import io.pravega.client.stream.EventWriterConfig;


public class PravegaWriter extends Writer {
    final EventStreamWriter<byte[]> producer;

    public PravegaWriter(int writerID, TriConsumer recordTime, Parameters params,
                        String streamName, ClientFactory factory) throws IOException {
        super(writerID, recordTime, params);
        this.producer = factory.createEventWriter(streamName,
                new ByteArraySerializer(),
                EventWriterConfig.builder().build());
    }

    @Override
    public void write(byte[] data) throws IOException {
        throw new IOException("Synchronous Writes are not implemented for Pravega");
    }

    @Override
    public CompletableFuture writeAsync(byte[] data) {
        return producer.writeEvent(data);
    }


    @Override
    public void flush() throws IOException {
            producer.flush();
    }

    @Override
    public synchronized void close() throws IOException {
            producer.close();
    }
}
