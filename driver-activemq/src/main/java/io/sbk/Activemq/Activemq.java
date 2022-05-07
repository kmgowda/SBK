/**
 * Copyright (c) KMG. All Rights Reserved..
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 */
package io.sbk.Activemq;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.javaprop.JavaPropsFactory;
import io.sbk.api.DataReader;
import io.sbk.api.DataWriter;
import io.sbk.api.ParameterOptions;
import io.sbk.api.Storage;
import io.sbk.data.DataType;
import io.sbk.data.impl.SbkString;
import io.sbk.options.InputOptions;
import org.apache.activemq.ActiveMQConnectionFactory;

import javax.jms.Connection;
import javax.jms.ConnectionFactory;
import javax.jms.JMSException;
import javax.jms.Session;
import java.io.IOException;
import java.util.Objects;

/**
 * Class for Activemq storage driver.
 *
 */
public class Activemq implements Storage<String> {
    private final static String CONFIGFILE = "activemq.properties";
    private ActivemqConfig config;

    @Override
    public void addArgs(final InputOptions params) throws IllegalArgumentException {
        final ObjectMapper mapper = new ObjectMapper(new JavaPropsFactory())
                .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        try {
            config = mapper.readValue(
                    Objects.requireNonNull(Activemq.class.getClassLoader().getResourceAsStream(CONFIGFILE)),
                    ActivemqConfig.class);
        } catch (Exception ex) {
            ex.printStackTrace();
            throw new IllegalArgumentException(ex);
        }

        // change and uncomment the below code as per your driver specific parameters
        params.addOption("url", true, "Activemq url, default: " + config.url);
        params.addOption("qname", true, "Activemq Queue name, default: " + config.qName);
    }

    @Override
    public void parseArgs(final ParameterOptions params) throws IllegalArgumentException {
        if (params.getReadersCount() < 1 || params.getWritersCount() < 1) {
            throw new IllegalArgumentException("Specify both Writer or readers for Java Concurrent Linked Queue");
        }
        config.url = params.getOptionValue("url", config.url);
        config.qName = params.getOptionValue("qname", config.qName);
    }

    @Override
    public void openStorage(final ParameterOptions params) throws IOException {
        ConnectionFactory connectionFactory = new ActiveMQConnectionFactory(config.url);
        try {
            Connection connection = connectionFactory.createConnection();
            connection.start();
            config.session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
            //The queue will be created automatically on the server.
            config.dst = config.session.createQueue(config.qName);
        } catch (JMSException ex) {
            throw new IOException(ex);
        }

    }

    @Override
    public void closeStorage(final ParameterOptions params) throws IOException {
        try {
            config.session.close();
        } catch (JMSException ex) {
            throw new IOException(ex);
        }
    }

    @Override
    public DataWriter<String> createWriter(final int id, final ParameterOptions params) {
        return new ActivemqWriter(id, params, config);
    }

    @Override
    public DataReader<String> createReader(final int id, final ParameterOptions params) {
        return new ActivemqReader(id, params, config);
    }

    @Override
    public DataType<String> getDataType() {
        return new SbkString();
    }
}
