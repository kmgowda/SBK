/**
 * Copyright (c) KMG. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 */
package io.sbk.driver.Pulsar;

import io.sbk.params.ParameterOptions;
import io.sbk.api.Writer;
import lombok.Synchronized;
import org.apache.pulsar.client.api.Producer;
import org.apache.pulsar.client.api.PulsarClient;
import org.apache.pulsar.client.api.PulsarClientException;

import java.io.IOException;
import java.util.concurrent.CompletableFuture;

/**
 * Class for Pulsar writer/producer.
 */
public class PulsarWriter implements Writer<byte[]> {
    final private Producer<byte[]> producer;

    public PulsarWriter(int writerID, ParameterOptions params, String topicName, PulsarClient client) throws IOException {
        try {
            this.producer = client.newProducer()
                    .enableBatching(true)
                    .topic(topicName)
                    .blockIfQueueFull(true).create();
        } catch (PulsarClientException ex) {
            throw new IOException(ex);
        }
    }

    @Override
    public CompletableFuture writeAsync(byte[] data) throws IOException {
        return producer.sendAsync(data);
    }


    @Override
    public void sync() throws IOException {
        try {
            producer.flush();
        } catch (PulsarClientException ex) {
            throw new IOException(ex);
        }
    }

    @Override
    @Synchronized
    public void close() throws IOException {
        try {
            producer.close();
        } catch (PulsarClientException ex) {
            throw new IOException(ex);
        }
    }
}