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

import io.perf.core.Benchmark;
import io.perf.core.Parameters;
import io.perf.core.QuadConsumer;
import io.perf.core.Writer;
import io.perf.core.Reader;

import io.perf.drivers.Kafka.KafkaReader;
import io.perf.drivers.Kafka.KafkaWriter;

import java.io.IOException;
import java.util.Locale;
import java.util.Properties;

import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.requests.IsolationLevel;
import org.apache.kafka.common.serialization.ByteArraySerializer;
import org.apache.kafka.common.serialization.ByteArrayDeserializer;

/**
 * Abstract class for Benchmarking.
 */
public class Kafka extends Benchmark {
    private String topicName;
    private String brokerUri;
    private Properties producerConfig;
    private Properties consumerConfig;

    @Override
    public void addArgs(final Parameters params) {
        params.addOption("topic", true, "Topic name");
        params.addOption("broker", true, "Broker URI");
    }

    @Override
    public void parseArgs(final Parameters params) throws IllegalArgumentException {
        topicName =  params.getOptionValue("topic", null);
        brokerUri = params.getOptionValue("broker", null);
        if (brokerUri == null) {
            throw new IllegalArgumentException("Error: Must specify Broker IP address");
        }

        if (topicName == null) {
            throw new IllegalArgumentException("Error: Must specify Topic Name");
        }
    }

    private Properties createProducerConfig(Parameters params) {
        if (params.writersCount < 1) {
            return null;
        }
        final Properties props = new Properties();
        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, brokerUri);
        props.put(ProducerConfig.ACKS_CONFIG, "all");
        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, ByteArraySerializer.class.getName());
        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, ByteArraySerializer.class.getName());
        // Enabling the producer IDEMPOTENCE is must to compare between Kafka and Pravega
        props.put(ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG, true);
        return props;
    }

    private Properties createConsumerConfig(Parameters params) {
        if (params.readersCount < 1) {
            return null;
        }
        final Properties props = new Properties();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, brokerUri);
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, ByteArrayDeserializer.class.getName());
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, ByteArrayDeserializer.class.getName());
        props.put(ConsumerConfig.MAX_POLL_RECORDS_CONFIG, 1);
        // Enabling the consumer to READ_COMMITTED is must to compare between Kafka and Pravega
        props.put(ConsumerConfig.ISOLATION_LEVEL_CONFIG, IsolationLevel.READ_COMMITTED.name().toLowerCase(Locale.ROOT));
        if (params.writeAndRead) {
            props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "latest");
            props.put(ConsumerConfig.GROUP_ID_CONFIG, topicName);
        } else {
            props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
            props.put(ConsumerConfig.GROUP_ID_CONFIG, Long.toString(params.startTime));
        }
        return props;
    }

    @Override
    public void openStorage(final Parameters params) throws  IOException {
        producerConfig = createProducerConfig(params);
        consumerConfig = createConsumerConfig(params);
    }

    @Override
    public void closeStorage(final Parameters params) throws IOException {

    }

    @Override
    public Writer createWriter(final int id, QuadConsumer recordTime , final Parameters params) {
        try {
            return new KafkaWriter(id, recordTime, params, topicName, producerConfig);
        } catch (IOException ex) {
            ex.printStackTrace();
            return null;
        }
    }

    @Override
    public Reader createReader(final int id, QuadConsumer recordTime, final Parameters params) {
        try {
            return new KafkaReader(id, recordTime, params, topicName, consumerConfig);
        } catch (IOException ex) {
            ex.printStackTrace();
            return null;
        }
    }
}
