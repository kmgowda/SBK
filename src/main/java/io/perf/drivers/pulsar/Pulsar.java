/**
 * Copyright (c) 2020 KMG. All Rights Reserved..
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 */
package io.perf.drivers.Pulsar;
import io.perf.core.Benchmark;
import io.perf.core.Parameters;
import io.perf.core.QuadConsumer;
import io.perf.core.Writer;
import io.perf.core.Reader;

import java.io.IOException;

import org.apache.pulsar.client.api.PulsarClient;
import org.apache.pulsar.client.api.PulsarClientException;

/**
 * Abstract class for Benchmarking.
 */
public class Pulsar extends Benchmark {
    static final String DEFAULT_NAMESPACE = null;
    static final String DEFAULT_TENANT = null;
    static final String DEFAULT_CLUSTER = null;

    private String topicName;
    private String brokerUri;
    private String nameSpace;
    private String cluster;
    private String tenant;
    private String adminUri;
    private int partitions;
    private int ensembleSize;
    private int writeQuorum;
    private int ackQuorum;
    private boolean deduplication;
    private PulsarClient client;
    private PulsarTopicHandler topicHandler;


    @Override
    public void addArgs(final Parameters params) {
        params.addOption("cluster", true, "Cluster name");
        params.addOption("topic", true, "Topic name");
        params.addOption("broker", true, "Broker URI");
        params.addOption("admin", true, "Admin URI");
        params.addOption("partitions", true, "Number of partitions of the topic");

        params.addOption("ensembleSize", true, "ensembleSize");
        params.addOption("writeQuorum", true, "writeQuorum");
        params.addOption("ackQuorum", true, "ackQuorum");
        params.addOption("deduplication", true, "Enable or Disable Deduplication; by deafult disabled");
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

        adminUri = params.getOptionValue("admin", null);
        cluster =  params.getOptionValue("cluster", DEFAULT_CLUSTER);
        partitions = Integer.parseInt(params.getOptionValue("partitions", "1"));
        ensembleSize = Integer.parseInt(params.getOptionValue("ensembleSize", "1"));
        writeQuorum = Integer.parseInt(params.getOptionValue("writeQuorum", "1"));
        ackQuorum = Integer.parseInt(params.getOptionValue("ackQuorum", "1"));
        deduplication = Boolean.parseBoolean(params.getOptionValue("recreate", "false"));

        final String[] names = topicName.split("[/]");
        try {
            nameSpace = names[names.length-2];
        } catch (ArrayIndexOutOfBoundsException ex) {
            nameSpace = DEFAULT_NAMESPACE;
        }
        try {
            tenant = names[names.length-3];
        } catch ( ArrayIndexOutOfBoundsException ex) {
            tenant = DEFAULT_TENANT;
        }

    }

    @Override
    public void openStorage(final Parameters params) throws  IOException {
        try {
            client = PulsarClient.builder().serviceUrl(brokerUri).build();
        } catch (PulsarClientException ex) {
            ex.printStackTrace();
            throw new IOException(ex);
        }
        if (adminUri != null) {
            topicHandler = new PulsarTopicHandler(adminUri, brokerUri, tenant, cluster, nameSpace,
                    topicName, partitions, ensembleSize, writeQuorum, ackQuorum, deduplication);
            topicHandler.createTopic(params.writersCount > 0);
        } else {
            topicHandler = null;
        }

    }

    @Override
    public void closeStorage(final Parameters params) throws IOException {

    }

    @Override
    public Writer createWriter(final int id, QuadConsumer recordTime, final Parameters params) {
        try {
            return new PulsarWriter(id, recordTime, params, topicName, client);
        } catch (IOException ex) {
            ex.printStackTrace();
            return null;
        }
    }

    @Override
    public Reader createReader(final int id, QuadConsumer recordTime, final Parameters params) {
        try {
            return new PulsarReader(id, recordTime, params, topicName, topicName+"rdGrp", client);
        } catch (IOException ex) {
            ex.printStackTrace();
            return null;
        }
    }
}
