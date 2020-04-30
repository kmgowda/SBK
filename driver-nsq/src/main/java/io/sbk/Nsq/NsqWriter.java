/**
 * Copyright (c) KMG. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 */
package io.sbk.Nsq;

import com.github.brainlag.nsq.NSQProducer;
import com.github.brainlag.nsq.exceptions.NSQException;
import io.sbk.api.Writer;
import io.sbk.api.Parameters;

import java.io.IOException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeoutException;

/**
 * Class for NSQ Writer.
 */
public class NsqWriter implements Writer<byte[]> {
    final private NSQProducer producer;
    final private String topicName;

    public NsqWriter(int writerID, Parameters params,
                         String topicName, NsqClientConfig config) throws IOException {
        final String[] uri = config.uri.split(":", 2);
        this.topicName = topicName;
        producer = new NSQProducer();
        producer.addAddress(uri[0], Integer.parseInt(uri[1]));
        producer.start();
    }


    @Override
    public CompletableFuture writeAsync(byte[] data) throws IOException {
        try {
            producer.produce(topicName, data);
        } catch (NSQException | TimeoutException ex) {
            throw  new IOException(ex);
        }
        return null;
    }

    @Override
    public void flush() throws IOException {

    }

    @Override
    public void close() throws IOException {
        producer.shutdown();
    }
}