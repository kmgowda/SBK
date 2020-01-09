/**
 * Copyright (c) 2020 KMG. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 */
package io.perf.drivers.Pulsar;

import io.perf.core.Reader;
import io.perf.core.TriConsumer;
import io.perf.core.Parameters;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import org.apache.pulsar.client.api.PulsarClient;
import org.apache.pulsar.client.api.Consumer;
import org.apache.pulsar.client.api.SubscriptionType;
import org.apache.pulsar.client.api.PulsarClientException;
import org.apache.pulsar.client.api.SubscriptionInitialPosition;

/**
 * Class for Pulsar reader/consumer.
 */
public class PulsarReader extends Reader {
    final private Consumer<byte[]> consumer;

    public PulsarReader(int readerId, TriConsumer recordTime, Parameters params,
                              String topicName, String subscriptionName, PulsarClient client) throws  IOException {
        super(readerId, recordTime, params);

        try {

            this.consumer = client.newConsumer()
                    .topic(topicName)
                    // Allow multiple consumers to attach to the same subscription
                    // and get messages dispatched as a queue
                    .subscriptionType(SubscriptionType.Shared)
                    .subscriptionName(subscriptionName)
                    .subscriptionInitialPosition(SubscriptionInitialPosition.Earliest)
                    .receiverQueueSize(1)
                    .subscribe();

        } catch (PulsarClientException ex){
            throw new IOException(ex);
        }
    }

    @Override
    public byte[] read() throws IOException {
        try {
            return consumer.receive(params.timeout, TimeUnit.SECONDS).getData();
        } catch (PulsarClientException ex){
            throw new IOException(ex);
        }
    }

    @Override
    public void close() throws IOException {
        try {
            consumer.close();
        } catch (PulsarClientException ex){
            throw new IOException(ex);
        }
    }
}