/**
 * Copyright (c) 2017 Dell Inc., or its subsidiaries. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 */
package io.perf.drivers.kafka;
import io.perf.core.WriterWorker;
import io.perf.core.PerfStats;
import io.perf.core.TriConsumer;
import java.util.Properties;

import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;

/**
 * Class for Kafka writer/producer.
 */
public class KafkaWriterWorker extends WriterWorker {
    final private KafkaProducer<byte[], byte[]> producer;

    public KafkaWriterWorker(int sensorId, int events, int flushEvents,
                      int secondsToRun, boolean isRandomKey, int messageSize,
                      long start, PerfStats stats, String streamName,
                      int eventsPerSec, boolean writeAndRead, Properties producerProps) {

        super(sensorId, events, flushEvents,
                secondsToRun, isRandomKey, messageSize,
                start, stats, streamName, eventsPerSec, writeAndRead);

        this.producer = new KafkaProducer<>(producerProps);
    }

    public long recordWrite(byte[] data, TriConsumer record) {
        final long time = System.currentTimeMillis();
        producer.send(new ProducerRecord<>(streamName, data), (metadata, exception) -> {
            final long endTime = System.currentTimeMillis();
            record.accept(time, endTime, data.length);
        });
        return time;
    }

    @Override
    public void writeData(byte[] data) {
        producer.send(new ProducerRecord<>(streamName, data));
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