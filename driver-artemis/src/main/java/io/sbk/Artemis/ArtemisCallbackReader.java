/**
 * Copyright (c) KMG. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 */
package io.sbk.Artemis;

import io.sbk.api.AbstractCallbackReader;
import io.sbk.api.Callback;
import io.sbk.params.ParameterOptions;
import io.sbk.system.Printer;
import org.apache.activemq.artemis.api.core.ActiveMQException;
import org.apache.activemq.artemis.api.core.ActiveMQQueueExistsException;
import org.apache.activemq.artemis.api.core.RoutingType;
import org.apache.activemq.artemis.api.core.SimpleString;
import org.apache.activemq.artemis.api.core.client.ClientConsumer;
import org.apache.activemq.artemis.api.core.client.ClientSession;

import java.io.IOException;

/**
 * Class for Artemis Push Reader.
 */
public class ArtemisCallbackReader extends AbstractCallbackReader<byte[]> {
    private final ClientConsumer consumer;
    private final ClientSession session;

    public ArtemisCallbackReader(int readerId, ParameterOptions params, String topicName,
                                 String subscriptionName, ArtemisClientConfig config, ClientSession session) throws IOException {
        this.session = session;
        try {
            session.createQueue(SimpleString.toSimpleString(topicName), RoutingType.MULTICAST,
                    SimpleString.toSimpleString(subscriptionName), true /* durable */);
        } catch (ActiveMQQueueExistsException ex) {
            //
        } catch (ActiveMQException ex) {
            throw new IOException(ex);
        }
        try {
            consumer = session.createConsumer(subscriptionName);
        } catch (ActiveMQException ex) {
            throw new IOException(ex);
        }
    }

    @Override
    public void start(Callback<byte[]> callback) throws IOException {
        try {
            consumer.setMessageHandler(message -> {
                byte[] payload = new byte[message.getBodyBuffer().readableBytes()];
                message.getBodyBuffer().readBytes(payload);
                callback.consume(payload);
                try {
                    message.acknowledge();
                } catch (ActiveMQException e) {
                    Printer.log.error("ArtemisCallbackReader : Message acknowledge failed");
                }
            });
        } catch (ActiveMQException ex) {
            throw new IOException(ex);
        }
    }

    @Override
    public void stop() throws IOException {
        try {
            consumer.close();
        } catch (ActiveMQException ex) {
            throw new IOException(ex);
        }
    }
}