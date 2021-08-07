/**
 * Copyright (c) KMG. All Rights Reserved..
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 */
package io.sbk.Pulsar;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.javaprop.JavaPropsFactory;
import io.sbk.api.DataReader;
import io.sbk.api.DataWriter;
import io.sbk.api.Storage;
import io.sbk.parameters.ParameterOptions;

import java.io.IOException;
import java.util.Objects;

import org.apache.pulsar.client.api.PulsarClient;
import org.apache.pulsar.client.api.PulsarClientException;

/**
 * Class for Pulsar Benchmarking.
 */
public class Pulsar implements Storage<byte[]> {
    private static final String CONFIGFILE = "pulsar.properties";
    private static final String DEFAULT_NAMESPACE = null;
    private static final String DEFAULT_TENANT = null;
    private static final String DEFAULT_CLUSTER = null;
    private PulsarConfig config;

    private PulsarClient client;
    private PulsarTopicHandler topicHandler;


    @Override
    public void addArgs(final ParameterOptions params) throws IllegalArgumentException {
        final ObjectMapper mapper = new ObjectMapper(new JavaPropsFactory())
                .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        try {
            config = mapper.readValue(
                    Objects.requireNonNull(Pulsar.class.getClassLoader().getResourceAsStream(CONFIGFILE)),
                    PulsarConfig.class);
        } catch (Exception ex) {
            ex.printStackTrace();
            throw new IllegalArgumentException(ex);
        }
        params.addOption("cluster", true, "Cluster name (optional parameter)");
        params.addOption("topic", true, "Topic name, default : " + config.topicName);
        params.addOption("broker", true, "Broker URI, default: " + config.brokerUri);
        params.addOption("admin", true,
                "Admin URI, required to create the partitioned topic, default: " + config.adminUri);
        params.addOption("partitions", true,
                "Number of partitions of the topic, default: "+ config.partitions);
        params.addOption("ensembleSize", true, "EnsembleSize default: " + config.ensembleSize );
        params.addOption("writeQuorum", true, "WriteQuorum default: " + config.writeQuorum);
        params.addOption("ackQuorum", true, "AckQuorum default: " + config.ackQuorum);
        params.addOption("deduplication", true,
                "Enable or Disable Deduplication; default: " + config.deduplicationEnabled);
        params.addOption("threads", true, "io threads per Topic, default: " + config.ioThreads);
    }

    @Override
    public void parseArgs(final ParameterOptions params) throws IllegalArgumentException {
        config.topicName =  params.getOptionValue("topic", config.topicName);
        config.brokerUri = params.getOptionValue("broker", config.brokerUri);

        config.adminUri = params.getOptionValue("admin", config.adminUri);
        config.cluster =  params.getOptionValue("cluster", DEFAULT_CLUSTER);
        config.partitions = Integer.parseInt(params.getOptionValue("partitions", String.valueOf(config.partitions)));
        config.ioThreads =  Integer.parseInt(params.getOptionValue("threads", String.valueOf(config.ioThreads)));
        config.ensembleSize = Integer.parseInt(params.getOptionValue("ensembleSize", String.valueOf(config.ensembleSize)));
        config.writeQuorum = Integer.parseInt(params.getOptionValue("writeQuorum", String.valueOf(config.writeQuorum)));
        config.ackQuorum = Integer.parseInt(params.getOptionValue("ackQuorum", String.valueOf(config.ackQuorum)));
        config.deduplicationEnabled = Boolean.parseBoolean(params.getOptionValue("recreate",
                                        String.valueOf(config.deduplicationEnabled)));

        final String[] names = config.topicName.split("[/]");
        try {
            config.nameSpace = names[names.length-2];
        } catch (ArrayIndexOutOfBoundsException ex) {
            config.nameSpace = DEFAULT_NAMESPACE;
        }
        try {
            config.tenant = names[names.length-3];
        } catch ( ArrayIndexOutOfBoundsException ex) {
            config.tenant = DEFAULT_TENANT;
        }

    }

    @Override
    public void openStorage(final ParameterOptions params) throws  IOException {
        try {
            client = PulsarClient.builder().ioThreads(config.ioThreads).serviceUrl(config.brokerUri).build();
        } catch (PulsarClientException ex) {
            ex.printStackTrace();
            throw new IOException(ex);
        }
        if (config.adminUri != null && params.getWritersCount() > 0) {
            topicHandler = new PulsarTopicHandler(config);
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
            return new PulsarWriter(id, params, config.topicName, client);
        } catch (IOException ex) {
            ex.printStackTrace();
            return null;
        }
    }

    @Override
    public DataReader<byte[]> createReader(final int id, final ParameterOptions params) {
        try {
            return new PulsarReader(id, params, config.topicName, config.topicName+"rdGrp", client);
        } catch (IOException ex) {
            ex.printStackTrace();
            return null;
        }
    }
}
