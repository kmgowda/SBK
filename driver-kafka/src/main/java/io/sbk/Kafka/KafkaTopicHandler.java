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

import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.NewTopic;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ExecutionException;

public class KafkaTopicHandler {
    final private Properties topicProperties;
    final private Properties adminProperties;
    final private AdminClient admin;
    final private NewTopic topic;


    public KafkaTopicHandler(String brokerUri, String topicName, int partitions,
                              short replicationFactor, short minSync) throws IOException {
            adminProperties = new Properties();
            topicProperties = new Properties();
            adminProperties.put("bootstrap.servers", brokerUri);
            topicProperties.put("min.insync.replicas", Short.toString(minSync));
            admin = AdminClient.create(adminProperties);
            topic = new NewTopic(topicName, partitions, replicationFactor);
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

    public  void close() {
        admin.close();
    }

}
