/**
 * Copyright (c) KMG. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 */
package io.sbk.drivers.Activemq;

import io.sbk.params.ParameterOptions;
import io.sbk.api.Writer;

import javax.jms.JMSException;
import javax.jms.MessageProducer;
import java.io.IOException;
import java.util.concurrent.CompletableFuture;

/**
 * Class for Activemq Writer.
 */
public class ActivemqWriter implements Writer<String> {
    ActivemqConfig config;
    MessageProducer producer;

    public ActivemqWriter(int writerID, ParameterOptions params, ActivemqConfig config) {
        this.config = config;
        try {
           this.producer = config.session.createProducer(config.dst);
        } catch (JMSException ex) {
           ex.printStackTrace();
        }
    }

    @Override
    public CompletableFuture writeAsync(String data) throws IOException {
        try {
            this.producer.send(config.session.createTextMessage(data));
        } catch (JMSException ex) {
           throw new IOException(ex);
        }
        return null;
    }

    @Override
    public void sync() throws IOException {

    }

    @Override
    public void close() throws IOException {
    }
}