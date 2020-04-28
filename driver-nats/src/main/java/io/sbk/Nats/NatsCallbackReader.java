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

import io.sbk.api.CallbackReader;
import io.sbk.api.Parameters;
import io.sbk.api.Callback;

import java.io.IOException;
import java.time.Duration;
import java.util.concurrent.TimeoutException;
import io.nats.client.Connection;
import io.nats.client.Dispatcher;
import io.nats.client.Nats;
import io.nats.client.Options;

/**
 * Class for NATS Push Reader.
 */
public class NatsCallbackReader  implements CallbackReader<byte[]> {
    final private String topic;
    final private String subscriptionName;
    final private Connection cn;
    private Dispatcher consumer;

    public NatsCallbackReader(int readerId, Parameters params, String topicName,
                              String subscriptionName, Options option) throws IOException {
        this.topic = topicName;
        this.subscriptionName = subscriptionName;
        try {
            cn = Nats.connect(option);
        } catch (InterruptedException ex) {
            throw new IOException(ex);
        }

    }


    @Override
    public void start(Callback callback) throws IOException {
        consumer = cn.createDispatcher(msg -> {
             callback.consume(msg.getData());
        });
        consumer.subscribe(topic, subscriptionName);
        try {
            cn.flush(Duration.ZERO);
        } catch (InterruptedException | TimeoutException ex) {
            throw new IOException(ex);
        }
    }

    @Override
    public void close() throws IOException {
        try {
            cn.close();
        } catch (InterruptedException ex) {
            throw new IOException(ex);
        }
    }
}