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
import io.perf.core.*;

import java.io.IOException;

import org.apache.pulsar.client.api.PulsarClient;
import org.apache.pulsar.client.api.PulsarClientException;

/**
 * Abstract class for Benchmarking.
 */
public class Pulsar extends Benchmark {
    private String topicname;
    private String brokerUri;
    private PulsarClient client;

    public void addArgs(final Parameters params) {
        params.addOption("topic", true, "Topic name");
        params.addOption("broker", true, "Broker URI");
 }

    public void parseArgs(final Parameters params) throws IllegalArgumentException {
        topicname =  params.getOptionValue("topic", null);
        brokerUri = params.getOptionValue("broker", null);
        if (brokerUri == null) {
            throw new IllegalArgumentException("Error: Must specify Broker IP address");
        }

        if (topicname == null) {
            throw new IllegalArgumentException("Error: Must specify Topic Name");
        }
    }

    public void openStorage(final Parameters params) throws  IOException {
        try {
            client = PulsarClient.builder().serviceUrl(brokerUri).build();
        } catch (PulsarClientException ex) {
            ex.printStackTrace();
            throw new IOException(ex);
        }
    }

    public void closeStorage(final Parameters params) throws IOException {

    }

    public Writer createWriter(final int id, TriConsumer recordTime , final Parameters params) {
        try {
            return new PulsarWriter(id, recordTime, params, topicname, client);
        } catch (IOException ex) {
            ex.printStackTrace();
            return null;
        }
    }

    public Reader createReader(final int id, TriConsumer recordTime, final Parameters params) {
        try {
            return new PulsarReader(id, recordTime, params, topicname, topicname+"rdGrp", client);
        } catch (IOException ex) {
            ex.printStackTrace();
            return null;
        }
    }
}
