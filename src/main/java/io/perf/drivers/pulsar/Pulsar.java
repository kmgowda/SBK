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
import io.perf.core.TriConsumer;
import io.perf.core.Writer;
import io.perf.core.Reader;

import java.io.IOException;

import org.apache.pulsar.client.api.PulsarClient;
import org.apache.pulsar.client.api.PulsarClientException;

/**
 * Abstract class for Benchmarking.
 */
public class Pulsar extends Benchmark {
    private String topicName;
    private String brokerUri;
    private PulsarClient client;

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

    @Override
    public void openStorage(final Parameters params) throws  IOException {
        try {
            client = PulsarClient.builder().serviceUrl(brokerUri).build();
        } catch (PulsarClientException ex) {
            ex.printStackTrace();
            throw new IOException(ex);
        }
    }

    @Override
    public void closeStorage(final Parameters params) throws IOException {

    }

    @Override
    public Writer createWriter(final int id, TriConsumer recordTime , final Parameters params) {
        try {
            return new PulsarWriter(id, recordTime, params, topicName, client);
        } catch (IOException ex) {
            ex.printStackTrace();
            return null;
        }
    }

    @Override
    public Reader createReader(final int id, TriConsumer recordTime, final Parameters params) {
        try {
            return new PulsarReader(id, recordTime, params, topicName, topicName+"rdGrp", client);
        } catch (IOException ex) {
            ex.printStackTrace();
            return null;
        }
    }
}
