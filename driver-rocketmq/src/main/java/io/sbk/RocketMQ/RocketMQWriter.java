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
import io.sbk.api.QuadConsumer;
import io.sbk.api.Writer;
import io.sbk.api.Parameters;
import org.apache.rocketmq.client.exception.MQClientException;
import org.apache.rocketmq.client.producer.DefaultMQProducer;
import org.apache.rocketmq.client.producer.SendCallback;
import org.apache.rocketmq.client.producer.SendResult;
import org.apache.rocketmq.common.message.Message;

import java.io.IOException;
import java.util.concurrent.CompletableFuture;


/**
 * Class for RocketMQ Writer.
 */
public class RocketMQWriter implements Writer<byte[]> {
    final private DefaultMQProducer rmqProducer;
    final private String topicName;

    public RocketMQWriter(int writerID, Parameters params, String namesAdr,
                          String topicName, RocketMQClientConfig config) throws IOException {
        this.topicName = topicName;
        rmqProducer = new DefaultMQProducer("ProducerGroup_" + RocketMQ.getRandomString());
        rmqProducer.setNamesrvAddr(namesAdr);
        rmqProducer.setInstanceName("ProducerInstance" + writerID);
        if (null != config.vipChannelEnabled) {
            rmqProducer.setVipChannelEnabled(config.vipChannelEnabled);
        }
        if (null != config.maxMessageSize) {
            rmqProducer.setMaxMessageSize(config.maxMessageSize);
        }
        if (null != config.compressMsgBodyOverHowmuch) {
            rmqProducer.setCompressMsgBodyOverHowmuch(config.compressMsgBodyOverHowmuch);
        }
        try {
            rmqProducer.start();
        } catch (MQClientException ex) {
            ex.printStackTrace();
            throw  new IOException(ex);
        }
    }

    @Override
    public long recordWrite(byte[] data, int size, QuadConsumer record) {
        final long time = System.currentTimeMillis();
        Message message = new Message(topicName, data);

        try {
            this.rmqProducer.send(message, new SendCallback() {
                @Override
                public void onSuccess(final SendResult sendResult) {
                    final long endTime = System.currentTimeMillis();
                    record.accept(time, endTime, size, 1);
                }

                @Override
                public void onException(final Throwable e) {
                  e.printStackTrace();
                }
            });
        } catch (Exception ex) {
          ex.printStackTrace();
        }

        return time;
    }


    @Override
    public CompletableFuture writeAsync(byte[] data) throws IOException {
        Message message = new Message(topicName, data);
        try {
            this.rmqProducer.send(message);
        } catch (Exception ex) {
            ex.printStackTrace();
            throw  new IOException(ex);
        }
        return null;
    }


    @Override
    public void flush() throws IOException {

    }

    @Override
    public void close() throws IOException {
        rmqProducer.shutdown();
    }
}