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
import io.sbk.api.Benchmark;
import io.sbk.api.Parameters;
import io.sbk.api.Writer;
import io.sbk.api.Reader;

import java.io.IOException;

import org.apache.pulsar.client.api.PulsarClient;
import org.apache.pulsar.client.api.PulsarClientException;

/**
 * class for Pulsar Benchmarking.
 */
public class Pulsar implements Benchmark {
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
    private int ioThreads;
    private int ensembleSize;
    private int writeQuorum;
    private int ackQuorum;
    private boolean deduplication;
    private PulsarClient client;
    private PulsarTopicHandler topicHandler;


    @Override
    public void addArgs(final Parameters params) {
        params.addOption("cluster", true, "Cluster name (optional parameter)");
        params.addOption("topic", true, "Topic name");
        params.addOption("broker", true, "Broker URI");
        params.addOption("admin", true, "Admin URI, required to create the partitioned topic");
        params.addOption("partitions", true, "Number of partitions of the topic (default: 1)");
        params.addOption("ensembleSize", true, "EnsembleSize (default: 1)");
        params.addOption("writeQuorum", true, "WriteQuorum (default: 1)");
        params.addOption("ackQuorum", true, "AckQuorum (default: 1) ");
        params.addOption("deduplication", true, "Enable or Disable Deduplication; by default disabled");
        params.addOption("threads", true, "io threads per Topic; by default (writers + readers)");
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
        ioThreads =  Integer.parseInt(params.getOptionValue("threads", "0"));
        ensembleSize = Integer.parseInt(params.getOptionValue("ensembleSize", "1"));
        writeQuorum = Integer.parseInt(params.getOptionValue("writeQuorum", "1"));
        ackQuorum = Integer.parseInt(params.getOptionValue("ackQuorum", "1"));
        deduplication = Boolean.parseBoolean(params.getOptionValue("recreate", "false"));

        if (ioThreads == 0) {
            ioThreads = params.getWritersCount() + params.getReadersCount();
        }

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
            client = PulsarClient.builder().ioThreads(ioThreads).serviceUrl(brokerUri).build();
        } catch (PulsarClientException ex) {
            ex.printStackTrace();
            throw new IOException(ex);
        }
        if (adminUri != null && params.getWritersCount() > 0) {
            topicHandler = new PulsarTopicHandler(adminUri, brokerUri, tenant, cluster, nameSpace,
                    topicName, partitions, ensembleSize, writeQuorum, ackQuorum, deduplication);
            topicHandler.createTopic(true);
        } else {
            topicHandler = null;
        }

    }

    @Override
    public void closeStorage(final Parameters params) throws IOException {
            topicHandler.close();
    }

    @Override
    public Writer createWriter(final int id, final Parameters params) {
        try {
            return new PulsarWriter(id, params, topicName, client);
        } catch (IOException ex) {
            ex.printStackTrace();
            return null;
        }
    }

    @Override
    public Reader createReader(final int id, final Parameters params) {
        try {
            return new PulsarReader(id, params, topicName, topicName+"rdGrp", client);
        } catch (IOException ex) {
            ex.printStackTrace();
            return null;
        }
    }
}
