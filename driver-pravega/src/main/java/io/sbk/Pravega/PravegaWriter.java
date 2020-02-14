/**
 * Copyright (c) KMG. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 */
package io.sbk.Pravega;

import io.sbk.api.Parameters;
import io.sbk.api.QuadConsumer;
import io.sbk.api.Writer;

import java.io.IOException;
import java.util.concurrent.CompletableFuture;

import io.pravega.client.stream.EventStreamWriter;
import io.pravega.client.stream.impl.ByteArraySerializer;
import io.pravega.client.stream.EventWriterConfig;
import io.pravega.client.EventStreamClientFactory;

/**
 * Class for Pravega writer/producer.
 */
public class PravegaWriter implements Writer<byte[]> {
    final EventStreamWriter<byte[]> producer;

    public PravegaWriter(int id, Parameters params, String streamName, EventStreamClientFactory factory) throws IOException {
        this.producer = factory.createEventWriter(streamName,
                new ByteArraySerializer(),
                EventWriterConfig.builder().build());
    }

    /**
     * Writes the data and benchmark.
     *
     * @param data   data to write
     * @param size   size of the data
     * @param record to call for benchmarking
     * @return time return the data sent time
     */
    @Override
    public long recordWrite(byte[] data, int size, QuadConsumer record) throws IOException {
        CompletableFuture ret;
        final long time = System.currentTimeMillis();
        ret = writeAsync(data);
        ret.thenAccept(d -> {
            final long endTime = System.currentTimeMillis();
            record.accept(time, endTime, size, 1);
        });
        return time;
    }


    @Override
    public CompletableFuture writeAsync(byte[] data) throws IOException {
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
