/**
 * Copyright (c) 2020 KMG. All Rights Reserved..
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 */
package io.perf.drivers.Kafka;

import io.perf.core.Parameters;
import io.perf.core.Reader;
import io.perf.core.TriConsumer;

import java.io.IOException;
import java.util.Properties;
import java.util.Arrays;

import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;

/**
 * Class for Kafka reader/consumer.
 */
public class KafkaReader extends Reader {
    final private KafkaConsumer<byte[], byte[]> consumer;

    public KafkaReader(int readerId, TriConsumer recordTime, Parameters params,
                       String topicName, Properties consumerProps) throws IOException {
        super(readerId, recordTime, params);

        this.consumer = new KafkaConsumer<>(consumerProps);
        this.consumer.subscribe(Arrays.asList(topicName));
    }

    @Override
    public byte[] read() {
        final ConsumerRecords<byte[], byte[]> records = consumer.poll(params.timeout);
        if (records.isEmpty()) {
            return null;
        }
        return records.iterator().next().value();
    }

    @Override
    public void close() {
        consumer.close();
    }
}