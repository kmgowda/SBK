/**
 * Copyright (c) KMG. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 */
package io.sbk.Activemq;

import io.sbk.params.ParameterOptions;
import io.sbk.params.Parameters;
import io.sbk.api.Reader;

import javax.jms.JMSException;
import javax.jms.MessageConsumer;
import javax.jms.TextMessage;
import java.io.IOException;

/**
 * Class for Activemq Reader.
 */
public class ActivemqReader implements Reader<String> {
    ActivemqConfig config;
    MessageConsumer consumer;
    Parameters params;

    public ActivemqReader(int readerId, ParameterOptions params, ActivemqConfig config) {
        this.params = params;
        this.config = config;
        try {
            this.consumer = config.session.createConsumer(config.dst);
        } catch (JMSException e) {
            e.printStackTrace();
        }
    }

    @Override
    public String read() throws IOException {
        try {
            TextMessage msg = (TextMessage) this.consumer.receive(params.getTimeoutMS());
            return msg.getText();
        } catch (JMSException e) {
            throw  new IOException(e);
        }
    }

    @Override
    public void close() throws IOException {

    }
}