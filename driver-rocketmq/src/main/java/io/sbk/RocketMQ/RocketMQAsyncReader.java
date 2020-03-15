/**
 * Copyright (c) KMG. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 */
package io.sbk.RocketMQ;

import io.sbk.api.AsyncReader;
import io.sbk.api.Parameters;
import io.sbk.api.ReaderCallback;
import org.apache.rocketmq.client.consumer.DefaultMQPushConsumer;
import org.apache.rocketmq.client.consumer.listener.ConsumeConcurrentlyStatus;
import org.apache.rocketmq.client.consumer.listener.MessageListenerConcurrently;
import org.apache.rocketmq.client.exception.MQClientException;
import org.apache.rocketmq.common.message.MessageExt;

import java.io.IOException;

/**
 * Class for Asynchronous RocketMQ Reader.
 */
public class RocketMQAsyncReader implements AsyncReader<byte[]> {
    final private DefaultMQPushConsumer rmqConsumer;
    final private Parameters params;

    public RocketMQAsyncReader(int readerId, Parameters params, String namesAdr, String topicName,
                          RocketMQClientConfig config, String subscriptionName ) throws IOException {
        this.params = params;
        rmqConsumer = new DefaultMQPushConsumer(subscriptionName);
        rmqConsumer.setNamesrvAddr(namesAdr);
        rmqConsumer.setInstanceName("ConsumerInstance" + readerId);
        if (null != config.vipChannelEnabled) {
            rmqConsumer.setVipChannelEnabled(config.vipChannelEnabled);
        }
        try {
            rmqConsumer.subscribe(topicName, "*");
        } catch (MQClientException ex) {
            ex.printStackTrace();
            throw  new IOException(ex);
        }
      }

    @Override
    public void start(ReaderCallback callback) throws IOException {
        try {
            rmqConsumer.registerMessageListener((MessageListenerConcurrently) (msgs, context) -> {
                for (MessageExt message : msgs) {
                   callback.consume(message);
                }
                return ConsumeConcurrentlyStatus.CONSUME_SUCCESS;
            });
            rmqConsumer.start();
        } catch (MQClientException ex) {
            ex.printStackTrace();
            throw  new IOException(ex);
        }
    }

    @Override
    public void close() throws IOException {
        rmqConsumer.shutdown();
    }
}