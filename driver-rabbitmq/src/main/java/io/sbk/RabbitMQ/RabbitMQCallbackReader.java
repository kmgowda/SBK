/**
 * Copyright (c) KMG. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 */
package io.sbk.RabbitMQ;

import io.sbk.api.AbstractCallbackReader;
import io.sbk.api.ParameterOptions;
import io.sbk.api.Callback;

import java.io.IOException;
import java.util.Collections;
import java.util.concurrent.TimeoutException;

import com.rabbitmq.client.BuiltinExchangeType;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.AMQP.BasicProperties;
import com.rabbitmq.client.DefaultConsumer;
import com.rabbitmq.client.Envelope;



/**
 * Class for RabbitMQ Callback Reader.
 */
public class RabbitMQCallbackReader extends AbstractCallbackReader<byte[]> {
    final private Channel channel;
    final private ParameterOptions params;
    final private String queueName;
    private DefaultConsumer consumer;

    public RabbitMQCallbackReader(int readerId, ParameterOptions params, Connection connection, String topicName,
                                  String queueName) throws IOException {
        channel = connection.createChannel();
        this.params = params;
        this.queueName = queueName;
        channel.exchangeDeclare(topicName, BuiltinExchangeType.FANOUT);
        channel.queueDeclare(queueName, true, false, false, Collections.emptyMap());
        channel.queueBind(queueName, topicName, "");
        this.consumer = null;
    }

    private static class Consumer extends DefaultConsumer {
        private Channel channel;
        private Callback<byte[]> callback;


        public Consumer(Channel channel, Callback<byte[]> callback) {
            super(channel);
            this.channel = channel;
            this.callback = callback;
        }

        @Override
        public void handleDelivery(String consumerTag, Envelope envelope, BasicProperties properties, byte[] body) {
            if (callback != null) {
                callback.consume(body);
            }
        }

    }


    @Override
    public void start(Callback<byte[]> callback) throws IOException {
        this.consumer = new Consumer(channel, callback);
        channel.basicConsume(queueName, true, this.consumer);
    }

    @Override
    public void stop() throws IOException {
        try {
            if (this.channel.isOpen()) {
                this.channel.close();
            }
        } catch (TimeoutException ex) {
            ex.printStackTrace();
            throw  new IOException(ex);
        }
    }
}