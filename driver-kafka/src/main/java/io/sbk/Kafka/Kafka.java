/**
 * Copyright (c) KMG. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 */
package io.sbk.Kafka;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.javaprop.JavaPropsFactory;
import io.sbk.api.DataReader;
import io.sbk.api.DataWriter;
import io.sbk.api.ParameterOptions;
import io.sbk.api.Storage;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.IsolationLevel;
import org.apache.kafka.common.serialization.ByteArrayDeserializer;
import org.apache.kafka.common.serialization.ByteArraySerializer;

import java.io.IOException;
import java.util.Locale;
import java.util.Objects;
import java.util.Properties;

/**
 * Class for Kafka Benchmarking.
 */
public class Kafka implements Storage<byte[]> {
    private final static String CONFIGFILE = "kafka.properties";
    private KafkaConfig config;
    private Properties producerConfig;
    private Properties consumerConfig;
    private KafkaTopicHandler topicHandler;


    public void addArgs(final ParameterOptions params, String configFile) throws IllegalArgumentException {
        final ObjectMapper mapper = new ObjectMapper(new JavaPropsFactory())
                .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        try {
            config = mapper.readValue(Objects.requireNonNull(Kafka.class.getClassLoader().getResourceAsStream(configFile)),
                    KafkaConfig.class);
        } catch (Exception ex) {
            ex.printStackTrace();
            throw new IllegalArgumentException(ex);
        }
        params.addOption("broker", true, "Broker URI, default: " + config.brokerUri);
        params.addOption("topic", true, "Topic name, default: " + config.topicName);
        params.addOption("partitions", true, "partitions, default: " + config.partitions);
        params.addOption("replica", true, "Replication factor, default: " + config.replica);
        params.addOption("sync", true, "Minimum in-sync Replicas, default: " + config.sync);
        params.addOption("create", true,
                "Create (recreate) the topic, valid only for writers, default: " + config.create);
    }

    @Override
    public void addArgs(final ParameterOptions params) throws IllegalArgumentException {
        addArgs(params, CONFIGFILE);
    }

    @Override
    public void parseArgs(final ParameterOptions params) throws IllegalArgumentException {
        config.topicName = params.getOptionValue("topic", config.topicName);
        config.brokerUri = params.getOptionValue("broker", config.brokerUri);
        config.partitions = Integer.parseInt(params.getOptionValue("partitions", Integer.toString(config.partitions)));
        config.replica = Short.parseShort(params.getOptionValue("replica", Integer.toString(config.replica)));
        config.sync = Short.parseShort(params.getOptionValue("sync", Integer.toString(config.sync)));
        config.create = Boolean.parseBoolean(params.getOptionValue("create", Boolean.toString(config.create)));
    }

    private Properties createProducerConfig(ParameterOptions params) {
        if (params.getWritersCount() < 1) {
            return null;
        }
        final Properties props = new Properties();
        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, config.brokerUri);
        props.put(ProducerConfig.ACKS_CONFIG, "all");
        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, ByteArraySerializer.class.getName());
        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, ByteArraySerializer.class.getName());
        // Enabling the producer IDEMPOTENCE is must, to compare between Kafka and Pravega.
        props.put(ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG, config.idempotence);
        return props;
    }

    private Properties createConsumerConfig(ParameterOptions params) {
        if (params.getReadersCount() < 1) {
            return null;
        }
        final Properties props = new Properties();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, config.brokerUri);
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, ByteArrayDeserializer.class.getName());
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, ByteArrayDeserializer.class.getName());
        props.put(ConsumerConfig.MAX_POLL_RECORDS_CONFIG, Integer.MAX_VALUE);
        // props.put(ConsumerConfig.MAX_POLL_INTERVAL_MS_CONFIG, params.getTimeout());
        // Enabling the consumer to READ_COMMITTED is must, to compare between Kafka and Pravega.
        props.put(ConsumerConfig.ISOLATION_LEVEL_CONFIG, IsolationLevel.READ_COMMITTED.name().toLowerCase(Locale.ROOT));
        if (params.isWriteAndRead()) {
            props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "latest");
            props.put(ConsumerConfig.GROUP_ID_CONFIG, config.topicName);
        } else {
            props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
            props.put(ConsumerConfig.GROUP_ID_CONFIG, Long.toString(System.currentTimeMillis()));
        }
        return props;
    }

    @Override
    public void openStorage(final ParameterOptions params) throws IOException {
        producerConfig = createProducerConfig(params);
        consumerConfig = createConsumerConfig(params);
        if (params.getWritersCount() > 0 && config.create) {
            topicHandler = new KafkaTopicHandler(config);
            topicHandler.createTopic(true);
        } else {
            topicHandler = null;
        }
    }

    @Override
    public void closeStorage(final ParameterOptions params) throws IOException {
        if (topicHandler != null) {
            topicHandler.close();
        }
    }

    @Override
    public DataWriter<byte[]> createWriter(final int id, final ParameterOptions params) {
        try {
            return new KafkaWriter(id, params, config.topicName, producerConfig);
        } catch (IOException ex) {
            ex.printStackTrace();
            return null;
        }
    }

    @Override
    public DataReader<byte[]> createReader(final int id, final ParameterOptions params) {
        try {
            return new KafkaReader(id, params, config.topicName, consumerConfig);
        } catch (IOException ex) {
            ex.printStackTrace();
            return null;
        }
    }
}
