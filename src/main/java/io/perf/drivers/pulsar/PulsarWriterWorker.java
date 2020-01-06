/**
 * Copyright (c) 2017 Dell Inc., or its subsidiaries. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 */
package io.perf.drivers.pulsar;
import io.perf.core.WriterWorker;
import io.perf.core.PerfStats;
import io.perf.core.TriConsumer;

import java.io.IOException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import org.apache.pulsar.client.api.Producer;
import org.apache.pulsar.client.api.PulsarClient;
import org.apache.pulsar.client.api.PulsarClientException;
/**
 * Class for Pulsar writer/producer.
 */
public class PulsarWriterWorker extends WriterWorker {
    final private Producer<byte[]> producer;

    public PulsarWriterWorker(int sensorId, int events, int flushEvents,
                      int secondsToRun, boolean isRandomKey, int messageSize,
                      long start, PerfStats stats, String streamName, int timeout,
                      int eventsPerSec, boolean writeAndRead, PulsarClient client) throws IOException{

        super(sensorId, events, flushEvents,
                secondsToRun, isRandomKey, messageSize,
                start, stats, streamName, timeout, eventsPerSec, writeAndRead);
        try {
            this.producer = client.newProducer().enableBatching(true)
                    .topic(streamName).sendTimeout(timeout, TimeUnit.SECONDS)
                    .blockIfQueueFull(true).create();
        } catch (PulsarClientException ex){
            throw new IOException(ex);
        }
    }

    public long recordWrite(byte[] data, TriConsumer record) {
        CompletableFuture ret;
        final long time = System.currentTimeMillis();
        ret = producer.sendAsync(data);
        ret.thenAccept(d -> {
            final long endTime = System.currentTimeMillis();
            record.accept(time, endTime, data.length);
        });
        return time;
    }

    @Override
    public void writeData(byte[] data) {
        producer.sendAsync(data);
    }


    @Override
    public void flush() throws IOException {
        try {
            producer.flush();
        } catch (PulsarClientException ex){
            throw new IOException(ex);
        }
    }

    @Override
    public synchronized void close() throws IOException {
        try {
            producer.close();
        } catch (PulsarClientException ex){
            throw new IOException(ex);
        }
    }
}