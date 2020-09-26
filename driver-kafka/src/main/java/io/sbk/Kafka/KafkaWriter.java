/**
 * Copyright (c) KMG. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 */
package io.sbk.Kafka;
import io.sbk.api.DataType;
import io.sbk.api.Parameters;
import io.sbk.api.Status;
import io.sbk.api.Writer;
import io.sbk.api.RecordTime;

import java.io.IOException;
import java.util.Properties;
import java.util.concurrent.CompletableFuture;

import lombok.Synchronized;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;

/**
 * Class for Kafka writer/producer.
 */
public class KafkaWriter implements Writer<byte[]> {
    final private KafkaProducer<byte[], byte[]> producer;
    final private String topicName;

    public KafkaWriter(int id, Parameters params, String topicName, Properties producerProps) throws IOException {
        this.topicName = topicName;
        this.producer = new KafkaProducer<>(producerProps);
    }

    @Override
    public void recordWrite(DataType<byte[]> dType, byte[] data, int size, Status status, RecordTime record, int id) {
        final long time = System.currentTimeMillis();
        status.startTime = time;
        status.bytes = size;
        status.records = 1;
        producer.send(new ProducerRecord<>(topicName, data), (metadata, exception) -> {
            final long endTime = System.currentTimeMillis();
            record.send(id, time, endTime, size, 1);
        });
    }

    private CompletableFuture writeAsyncFuture(byte[] data) {
        CompletableFuture<Void> retFuture = new CompletableFuture();
        producer.send(new ProducerRecord<>(topicName, data), (metadata, exception) -> {
                if (exception == null) {
                    retFuture.complete(null);
                } else {
                    retFuture.completeExceptionally(exception);
                }
        });
        return retFuture;
    }


    @Override
    public CompletableFuture writeAsync(byte[] data) throws IOException {
        producer.send(new ProducerRecord<>(topicName, data));
        return null;
    }

    @Override
    public void sync() {
        producer.flush();
    }

    @Override
    @Synchronized
    public void close() {
        producer.close();
    }
}