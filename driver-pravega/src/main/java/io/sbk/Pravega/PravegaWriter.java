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
import io.pravega.client.stream.EventStreamWriter;
import io.pravega.client.stream.EventWriterConfig;
import io.pravega.client.stream.impl.ByteArraySerializer;
import io.sbk.api.ParameterOptions;
import io.sbk.api.Status;
import io.sbk.api.Writer;
import io.sbk.data.DataType;
import io.perl.SendChannel;
import io.time.Time;

import java.io.IOException;
import java.util.concurrent.CompletableFuture;

/**
 * Class for Pravega writer/producer.
 */
public class PravegaWriter implements Writer<byte[]> {
    final EventStreamWriter<byte[]> producer;

    public PravegaWriter(int id, ParameterOptions params, String streamName, EventStreamClientFactory factory) throws IOException {
        this.producer = factory.createEventWriter(streamName,
                new ByteArraySerializer(),
                EventWriterConfig.builder().build());
    }

    /**
     * Writes the data and benchmark.
     *
     * @param dType  Data Type interface
     * @param data   data to write
     * @param size   size of the data
     * @param status Write status to return
     * @param record to call for benchmarking
     * @param id     for record benchmarking
     */
    @Override
    public void recordWrite(DataType<byte[]> dType, byte[] data, int size, Time time,
                            Status status, SendChannel record, int id) throws IOException {
        CompletableFuture<Void> ret;
        final long ctime = time.getCurrentTime();
        status.startTime = ctime;
        status.records = 1;
        status.bytes = size;
        ret = writeAsync(data);
        ret.thenAccept(d -> {
            final long endTime = time.getCurrentTime();
            record.send(id, ctime, endTime, size, 1);
        });
    }


    @Override
    public CompletableFuture<Void> writeAsync(byte[] data) throws IOException {
        return producer.writeEvent(data);
    }

    @Override
    public void sync() throws IOException {
        producer.flush();
    }

    @Override
    public synchronized void close() throws IOException {
        producer.close();
    }
}
