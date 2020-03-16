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

import io.sbk.api.AsyncReader;
import io.sbk.api.Parameters;
import io.sbk.api.ReaderCallback;

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
 * Class for RabbitMQ Async Reader.
 */
public class RabbitMQAsyncReader extends DefaultConsumer implements AsyncReader<byte[]> {
    final private Channel channel;
    final private Parameters params;
    final private String queueName;
    private ReaderCallback callback;

    public RabbitMQAsyncReader(int readerId, Parameters params, Connection connection, String topicName,
                          String queueName) throws IOException {
        super(connection.createChannel());
        this.params = params;
        this.queueName = queueName;
        channel = super.getChannel();
        channel.exchangeDeclare(topicName, BuiltinExchangeType.FANOUT);
        channel.queueDeclare(queueName, true, false, false, Collections.emptyMap());
        channel.queueBind(queueName, topicName, "");
        this.callback = null;
    }

    @Override
    public void handleDelivery(String consumerTag, Envelope envelope, BasicProperties properties, byte[] body) {
        if (callback != null) {
            callback.consume(body);
        }
    }

    @Override
    public void start(ReaderCallback callback) throws IOException {
        this.callback = callback;
        channel.basicConsume(queueName, true, this);
    }

    @Override
    public void close() throws IOException {
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