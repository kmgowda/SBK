/**
 * Copyright (c) KMG. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.sbk.drivers.Kafka;

import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.NewTopic;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ExecutionException;

/**
 * Class for Kafka topic and partitions.
 */
public class KafkaTopicHandler {
    final private Properties topicProperties;
    final private Properties adminProperties;
    final private AdminClient admin;
    final private NewTopic topic;

    public KafkaTopicHandler(KafkaConfig config) throws IOException {
        adminProperties = new Properties();
        topicProperties = new Properties();
        adminProperties.put("bootstrap.servers", config.brokerUri);
        topicProperties.put("min.insync.replicas", Short.toString(config.sync));
        admin = AdminClient.create(adminProperties);
        topic = new NewTopic(config.topicName, config.partitions, config.replica);
        topic.configs(new HashMap<>((Map) topicProperties));

    }

    public void createTopic(boolean recreate) throws IOException {
        try {
            if (recreate) {
                admin.deleteTopics(Arrays.asList(topic.name())).all().get();
            }
        } catch (InterruptedException | ExecutionException ex) {
            ex.printStackTrace();
        }
        try {
            admin.createTopics(Arrays.asList(topic)).all().get();
        } catch (InterruptedException | ExecutionException e) {
            throw new IOException(e);
        }
    }

    public void close() {
        admin.close();
    }

}
