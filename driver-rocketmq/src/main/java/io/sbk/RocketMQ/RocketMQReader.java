/**
 * Copyright (c) KMG. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 */
package io.sbk.RocketMQ;

import io.sbk.api.ParameterOptions;
import io.sbk.api.Reader;
import org.apache.rocketmq.client.consumer.DefaultMQPushConsumer;
import org.apache.rocketmq.client.consumer.listener.ConsumeConcurrentlyStatus;
import org.apache.rocketmq.client.consumer.listener.MessageListenerConcurrently;
import org.apache.rocketmq.client.exception.MQClientException;
import org.apache.rocketmq.common.message.MessageExt;

import java.io.IOException;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

/**
 * Class for RocketMQ Reader.
 */
public class RocketMQReader implements Reader<byte[]> {
    final private BlockingQueue<byte[]> queue;
    final private DefaultMQPushConsumer rmqConsumer;
    final private ParameterOptions params;

    public RocketMQReader(int readerId, ParameterOptions params, String namesAdr, String topicName,
                          RocketMQClientConfig config, String subscriptionName) throws IOException {
        this.params = params;
        queue = new LinkedBlockingQueue();
        rmqConsumer = new DefaultMQPushConsumer(subscriptionName);
        rmqConsumer.setNamesrvAddr(namesAdr);
        rmqConsumer.setInstanceName("ConsumerInstance" + readerId);
        if (null != config.vipChannelEnabled) {
            rmqConsumer.setVipChannelEnabled(config.vipChannelEnabled);
        }
        try {
            rmqConsumer.subscribe(topicName, "*");
            rmqConsumer.registerMessageListener((MessageListenerConcurrently) (msgs, context) -> {
                for (MessageExt message : msgs) {
                    queue.add(message.getBody());
                }
                return ConsumeConcurrentlyStatus.CONSUME_SUCCESS;
            });
            rmqConsumer.start();
        } catch (MQClientException ex) {
            ex.printStackTrace();
            throw new IOException(ex);
        }
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
        rmqConsumer.shutdown();
    }
}