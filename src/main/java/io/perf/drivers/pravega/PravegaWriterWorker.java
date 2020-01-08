/**
 * Copyright (c) 2017 Dell Inc., or its subsidiaries. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 */

package io.perf.drivers.pravega;
import io.perf.core.WriterWorker;
import io.perf.core.PerfStats;
import io.perf.core.TriConsumer;
import java.util.concurrent.CompletableFuture;

import io.pravega.client.stream.EventStreamWriter;
import io.pravega.client.ClientFactory;
import io.pravega.client.stream.impl.ByteArraySerializer;
import io.pravega.client.stream.EventWriterConfig;

/**
 * Class for Pravega writer/producer.
 */
public class PravegaWriterWorker extends WriterWorker {
    final EventStreamWriter<byte[]> producer;

    public PravegaWriterWorker(int sensorId, int events, int EventsPerFlush, int secondsToRun,
                        boolean isRandomKey, int messageSize, long start,
                        PerfStats stats, String streamName, int timeout, int eventsPerSec,
                        boolean writeAndRead, ClientFactory factory) {

        super(sensorId, events, EventsPerFlush,
                secondsToRun, isRandomKey, messageSize, start,
                stats, timeout, eventsPerSec, writeAndRead);

        this.producer = factory.createEventWriter(streamName,
                new ByteArraySerializer(),
                EventWriterConfig.builder().build());
    }

    @Override
    public long recordWrite(byte[] data, TriConsumer record) {
        CompletableFuture ret;
        final long time = System.currentTimeMillis();
        ret = producer.writeEvent(data);
        ret.thenAccept(d -> {
            final long endTime = System.currentTimeMillis();
            record.accept(time, endTime, data.length);
        });
        return time;
    }

    @Override
    public void writeData(byte[] data) {
        producer.writeEvent(data);
    }

    @Override
    public void flush() {
        producer.flush();
    }

    @Override
    public synchronized void close() {
        producer.close();
    }
}