/**
 * Copyright (c) KMG. All Rights Reserved..
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 */
package io.sbk.Artemis;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.javaprop.JavaPropsFactory;
import io.sbk.api.CallbackReader;
import io.sbk.api.Storage;
import io.sbk.api.Parameters;
import io.sbk.api.Writer;
import io.sbk.api.Reader;
import org.apache.activemq.artemis.api.core.RoutingType;
import org.apache.activemq.artemis.api.core.SimpleString;
import org.apache.activemq.artemis.api.core.client.ActiveMQClient;
import org.apache.activemq.artemis.api.core.client.ClientSession;
import org.apache.activemq.artemis.api.core.client.ClientSessionFactory;
import org.apache.activemq.artemis.api.core.client.ServerLocator;

import java.io.IOException;

/**
 * Class for Artemis.
 */
public class Artemis implements Storage<byte[]> {
    private final static String CONFIGFILE = "artemis.properties";
    private String topicName;
    private ArtemisClientConfig config;
    private ClientSessionFactory sessionFactory;
    private ClientSession session;

    @Override
    public void addArgs(final Parameters params) throws IllegalArgumentException {
        final ObjectMapper mapper = new ObjectMapper(new JavaPropsFactory())
                .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

        try {
            config = mapper.readValue(Artemis.class.getClassLoader().getResourceAsStream(CONFIGFILE),
                    ArtemisClientConfig.class);
        } catch (Exception ex) {
            ex.printStackTrace();
            throw new IllegalArgumentException(ex);
        }

        params.addOption("topic", true, "Topic name");
        params.addOption("uri", true, "Broker URI, default uri: "+config.uri);
    }

    @Override
    public void parseArgs(final Parameters params) throws IllegalArgumentException {
        topicName =  params.getOptionValue("topic", null);
        if (topicName == null) {
            throw new IllegalArgumentException("Error: Must specify Topic Name");
        }
        config.uri = params.getOptionValue("uri", config.uri);
    }

    @Override
    public void openStorage(final Parameters params) throws  IOException {
        try {
            ServerLocator serverLocator = ActiveMQClient.createServerLocator(config.uri);
            serverLocator.setConfirmationWindowSize(1000);
            sessionFactory = serverLocator.createSessionFactory();
            session = sessionFactory.createSession(config.user, config.password, config.xa,
                    config.autoCommitSends, config.autoCommitAcks, config.preAcknowledge, config.ackBatchSize);
            if (params.getWritersCount() > 0) {
                session.createAddress(SimpleString.toSimpleString(topicName),
                        RoutingType.MULTICAST, true);
            }
            session.start();
        } catch (Exception ex) {
            throw new IOException(ex);
        }
    }

    @Override
    public void closeStorage(final Parameters params) throws IOException {
        try {
            if (session != null) {
                session.close();
            }
        } catch (Exception ex) {
            throw  new IOException(ex);
        }

        if (sessionFactory != null) {
            sessionFactory.close();
        }
    }

    @Override
    public Writer<byte[]> createWriter(final int id, final Parameters params) {
        try {
            return new ArtemisWriter(id, params, topicName, config, session);
        } catch (IOException ex) {
            ex.printStackTrace();
            return null;
        }
    }

    @Override
    public Reader<byte[]> createReader(final int id, final Parameters params) {
        return null;
    }

    @Override
    public CallbackReader<byte[]> createCallbackReader(final int id, final Parameters params) {
        try {
            return new ArtemisCallbackReader(id, params, topicName, topicName + "-" + id,
                    config, session);
        } catch (IOException ex) {
            ex.printStackTrace();
            return null;
        }
    }
}
