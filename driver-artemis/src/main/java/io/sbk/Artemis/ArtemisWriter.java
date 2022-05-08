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

import io.perl.api.PerlChannel;
import io.sbk.params.ParameterOptions;
import io.sbk.api.Status;
import io.sbk.api.Writer;
import io.sbk.data.DataType;
import io.time.Time;
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
    final private ClientSession session;
    final private ClientProducer producer;

    public ArtemisWriter(int writerID, ParameterOptions params,
                         String topicName, ArtemisClientConfig config, ClientSession session) throws IOException {
        this.session = session;
        try {
            producer = session.createProducer(topicName);
        } catch (ActiveMQException ex) {
            throw new IOException(ex);
        }
    }

    @Override
    public void recordWrite(DataType<byte[]> dType, byte[] data, int size, Time time, Status status, PerlChannel record) {
        final long ctime = time.getCurrentTime();
        status.startTime = ctime;
        status.bytes = size;
        status.records = 1;
        ClientMessage msg = session.createMessage(true /* durable */);
        msg.setTimestamp(ctime);
        msg.getBodyBuffer().writeBytes(data);
        try {
            producer.send(msg, handler -> {
                final long endTime = time.getCurrentTime();
                record.send(ctime, endTime, size, 1);
            });
        } catch (ActiveMQException ex) {
            ex.printStackTrace();
        }
    }


    @Override
    public CompletableFuture<Void> writeAsync(byte[] data) throws IOException {
        ClientMessage msg = session.createMessage(true /* durable */);
        msg.getBodyBuffer().writeBytes(data);
        try {
            producer.send(msg);
        } catch (ActiveMQException ex) {
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
            throw new IOException(ex);
        }
    }
}