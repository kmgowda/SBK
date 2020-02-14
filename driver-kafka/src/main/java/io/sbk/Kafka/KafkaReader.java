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

import io.sbk.api.Parameters;
import io.sbk.api.Reader;

import java.io.IOException;
import java.time.Duration;
import java.util.Properties;
import java.util.Arrays;

import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;

/**
 * Class for Kafka reader/consumer.
 */
public class KafkaReader implements Reader<byte[]> {
    final private KafkaConsumer<byte[], byte[]> consumer;
    final private Duration timeoutDuration;

    public KafkaReader(int id, Parameters params, String topicName, Properties consumerProps) throws IOException {
        this.consumer = new KafkaConsumer<>(consumerProps);
        this.consumer.subscribe(Arrays.asList(topicName));
        this.timeoutDuration = Duration.ofMillis(params.getTimeout());
    }

    @Override
    public byte[] read() {
        final ConsumerRecords<byte[], byte[]> records = consumer.poll(timeoutDuration);
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