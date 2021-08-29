/**
 * Copyright (c) KMG. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 */
package io.sbk.RabbitMQ;

import com.rabbitmq.client.AMQP.BasicProperties;
import com.rabbitmq.client.BuiltinExchangeType;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.DefaultConsumer;
import com.rabbitmq.client.Envelope;
import io.sbk.api.ParameterOptions;
import io.sbk.api.Reader;

import java.io.IOException;
import java.util.Collections;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;


/**
 * Class for RabbitMQ Reader.
 */
public class RabbitMQReader extends DefaultConsumer implements Reader<byte[]> {
    final private Channel channel;
    final private BlockingQueue<byte[]> queue;
    final private ParameterOptions params;

    public RabbitMQReader(int readerId, ParameterOptions params, Connection connection, String topicName,
                          String queueName) throws IOException {
        super(connection.createChannel());
        this.params = params;
        channel = super.getChannel();
        channel.exchangeDeclare(topicName, BuiltinExchangeType.FANOUT);
        channel.queueDeclare(queueName, true, false, false, Collections.emptyMap());
        channel.queueBind(queueName, topicName, "");
        channel.basicConsume(queueName, true, this);
        queue = new LinkedBlockingQueue();
    }

    @Override
    public void handleDelivery(String consumerTag, Envelope envelope, BasicProperties properties, byte[] body) {
        queue.add(body);
    }

    @Override
    public byte[] read() throws IOException {
        try {
            return queue.poll(params.getTimeoutMS(), TimeUnit.MILLISECONDS);
        } catch (InterruptedException ex) {
            ex.printStackTrace();
            throw new IOException(ex);
        }
    }

    @Override
    public void close() throws IOException {
        try {
            if (this.channel.isOpen()) {
                this.channel.close();
            }
        } catch (TimeoutException ex) {
            ex.printStackTrace();
            throw new IOException(ex);
        }
    }
}