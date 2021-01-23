/**
 * Copyright (c) KMG. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 */
package io.sbk.NatsStream;

import io.sbk.api.AbstractCallbackReader;
import io.sbk.api.Parameters;
import io.sbk.api.Callback;
import io.nats.streaming.Message;
import io.nats.streaming.MessageHandler;
import io.nats.streaming.NatsStreaming;
import io.nats.streaming.Options.Builder;
import io.nats.streaming.StreamingConnection;
import io.nats.streaming.Subscription;
import io.nats.streaming.SubscriptionOptions;

import java.io.IOException;
import java.util.concurrent.TimeoutException;


/**
 * Class for NATS Streaming Push Reader.
 */
public class NatsStreamReader extends AbstractCallbackReader<byte[]> {
    final private String topic;
    final private String subscriptionName;
    final private StreamingConnection cn;
    final private SubscriptionOptions.Builder subBuilder;
    private Subscription sub;

    public NatsStreamReader(int readerId, Parameters params, String topicName,
                                    String subscriptionName, NatsStreamClientConfig config, Builder builder) throws IOException {
        this.topic = topicName;
        this.subscriptionName = subscriptionName;
        this.subBuilder = new SubscriptionOptions.Builder();
        this.subBuilder.maxInFlight(config.maxInFlight);
        try {
            cn = NatsStreaming.connect(config.clusterName, String.valueOf(readerId), builder.build());
        } catch (InterruptedException ex) {
            throw new IOException(ex);
        }
    }

    public void start(Callback<byte[]> callback) throws IOException {
        try {
            sub = cn.subscribe(topic, subscriptionName, new MessageHandler() {
                @Override
                public void onMessage(Message message) {
                    callback.consume(message.getData());
                }
            }, subBuilder.build());
        } catch (InterruptedException | TimeoutException ex) {
            throw  new IOException(ex);
        }
    }

    @Override
    public void close() throws IOException {
        try {
            cn.close();
        } catch (InterruptedException | TimeoutException ex) {
            throw new IOException(ex);
        }
    }
}