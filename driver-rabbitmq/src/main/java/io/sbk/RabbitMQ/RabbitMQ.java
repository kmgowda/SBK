/**
 * Copyright (c) KMG. All Rights Reserved..
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 */
package io.sbk.RabbitMQ;
import io.sbk.api.CallbackReader;
import io.sbk.api.Storage;
import io.sbk.api.Parameters;
import io.sbk.api.Writer;
import io.sbk.api.Reader;

import java.io.IOException;
import java.util.concurrent.TimeoutException;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;


/**
 * Class for RabbitMQ.
 */
public class RabbitMQ implements Storage<byte[]> {
    final private static String USER = "guest";
    final private static String PASSWORD = "guest";
    private Connection connection;
    private String topicName;
    private String brokerUri;
    private Boolean isPersist;
    private String user;
    private String password;

    @Override
    public void addArgs(final Parameters params) throws IllegalArgumentException {
        params.addOption("topic", true, "Topic name");
        params.addOption("broker", true, "Broker URI");
        params.addOption("persist", true, "keep messages persistent");
        params.addOption("user", true, "user name, default: " + USER);
        params.addOption("password", true, "user password, default: " + PASSWORD);
    }

    @Override
    public void parseArgs(final Parameters params) throws IllegalArgumentException {
        topicName =  params.getOptionValue("topic", null);
        brokerUri = params.getOptionValue("broker", null);
        if (brokerUri == null) {
            throw new IllegalArgumentException("Error: Must specify Broker IP address");
        }

        if (topicName == null) {
            throw new IllegalArgumentException("Error: Must specify Topic Name");
        }
        isPersist = Boolean.parseBoolean(params.getOptionValue("persist", "false"));
        user = params.getOptionValue("user", USER);
        password = params.getOptionValue("password", PASSWORD);
    }

    @Override
    public void openStorage(final Parameters params) throws  IOException {
        ConnectionFactory connectionFactory = new ConnectionFactory();
        connectionFactory.setAutomaticRecoveryEnabled(true);
        connectionFactory.setHost(brokerUri);
        connectionFactory.setUsername(user);
        connectionFactory.setPassword(password);

        try {
            connection = connectionFactory.newConnection();
        } catch (TimeoutException ex) {
            ex.printStackTrace();
            throw  new IOException("Timeout Exception occurred at openStorage of RabbitMQ");
        }

    }

    @Override
    public void closeStorage(final Parameters params) throws IOException {
        connection.close();
    }

    @Override
    public Writer createWriter(final int id, final Parameters params) {
        try {
            return new RabbitMQWriter(id, params, connection, topicName, isPersist);
        } catch (IOException ex) {
            ex.printStackTrace();
            return null;
        }
    }

    @Override
    public Reader createReader(final int id, final Parameters params) {
        return null;
    }

    @Override
    public CallbackReader createCallbackReader(final int id, final Parameters params) {
        try {
            return new RabbitMQCallbackReader(id, params, connection, topicName, topicName + "-" + id);
        } catch (IOException ex) {
            ex.printStackTrace();
            return null;
        }
    }
}
