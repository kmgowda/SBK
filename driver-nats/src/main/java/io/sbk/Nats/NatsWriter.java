/**
 * Copyright (c) KMG. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 */
package io.sbk.Nats;
import io.nats.client.Connection;
import io.nats.client.Nats;
import io.sbk.api.Writer;
import io.sbk.api.ParameterOptions;

import java.io.IOException;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeoutException;
import io.nats.client.Options;

/**
 * Class for NATS Writer.
 */
public class NatsWriter implements Writer<byte[]> {
    final private Connection producer;
    final private String topic;

    public NatsWriter(int writerID, ParameterOptions params,
                           String topicName, Options option) throws IOException {
        this.topic = topicName;
        try {
            producer = Nats.connect(option);
        } catch (InterruptedException ex) {
            throw new IOException(ex);
        }
    }

    @Override
    public CompletableFuture writeAsync(byte[] data) throws IOException {
        producer.publish(topic, data);
        return null;
    }


    @Override
    public void sync() throws IOException {
        try {
            producer.flush(Duration.ZERO);
        } catch (InterruptedException | TimeoutException ex) {
            throw new IOException(ex);
        }
    }

    @Override
    public void close() throws IOException {
        try {
            producer.close();
        } catch (InterruptedException ex) {
            throw new IOException(ex);
        }
    }
}