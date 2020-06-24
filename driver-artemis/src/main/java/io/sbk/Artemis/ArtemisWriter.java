/**
 * Copyright (c) KMG. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 */
package io.sbk.Artemis;
import io.sbk.api.RecordTime;
import io.sbk.api.Writer;
import io.sbk.api.Parameters;
import org.apache.activemq.artemis.api.core.ActiveMQException;
import org.apache.activemq.artemis.api.core.client.ClientMessage;
import org.apache.activemq.artemis.api.core.client.ClientProducer;
import org.apache.activemq.artemis.api.core.client.ClientSession;

import java.io.IOException;
import java.util.concurrent.CompletableFuture;

/**
 * Class for NATS Stream Writer.
 */
public class ArtemisWriter implements Writer<byte[]> {
    final private  ClientSession session;
    final private  ClientProducer producer;

    public ArtemisWriter(int writerID, Parameters params,
                            String topicName, ArtemisClientConfig config, ClientSession session) throws IOException {
        this.session = session;
        try {
            producer = session.createProducer(topicName);
        } catch (ActiveMQException ex) {
            throw new IOException(ex);
        }
    }

    @Override
    public long recordWrite(byte[] data, int size, RecordTime record, int id) {
        final long time = System.currentTimeMillis();
        ClientMessage msg = session.createMessage(true /* durable */ );
        msg.setTimestamp(time);
        msg.getBodyBuffer().writeBytes(data);
        try {
            producer.send(msg, handler -> {
                final long endTime = System.currentTimeMillis();
                record.accept(id, time, endTime, size, 1);
            });
        } catch ( ActiveMQException ex) {
            ex.printStackTrace();
        }
        return time;
    }


    @Override
    public CompletableFuture<Void> writeAsync(byte[] data) throws IOException {
        ClientMessage msg = session.createMessage(true /* durable */ );
        msg.getBodyBuffer().writeBytes(data);
        try {
            producer.send(msg);
        } catch ( ActiveMQException ex) {
            ex.printStackTrace();
        }
        return null;
    }

    @Override
    public void sync() throws IOException {

    }

    @Override
    public void close() throws IOException {
        try {
            producer.close();
        } catch (ActiveMQException ex) {
            throw  new IOException(ex);
        }
    }
}