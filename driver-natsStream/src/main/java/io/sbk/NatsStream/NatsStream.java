/**
 * Copyright (c) KMG. All Rights Reserved..
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 */
package io.sbk.NatsStream;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.javaprop.JavaPropsFactory;
import io.sbk.api.CallbackReader;
import io.sbk.api.Storage;
import io.sbk.api.Parameters;
import io.sbk.api.Writer;
import io.sbk.api.Reader;

import java.io.IOException;
import io.nats.streaming.Options.Builder;


/**
 * Class for Nats Streaming.
 */
public class NatsStream implements Storage<byte[]> {
    private final static String CONFIGFILE = "natsstream.properties";
    private String topicName;
    private NatsStreamClientConfig config;
    private Builder optsBuilder;

    @Override
    public void addArgs(final Parameters params) throws IllegalArgumentException {
        final ObjectMapper mapper = new ObjectMapper(new JavaPropsFactory())
                .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

        try {
            config = mapper.readValue(NatsStream.class.getClassLoader().getResourceAsStream(CONFIGFILE),
                    NatsStreamClientConfig.class);
        } catch (Exception ex) {
            ex.printStackTrace();
            throw new IllegalArgumentException(ex);
        }

        params.addOption("topic", true, "Topic name");
        params.addOption("uri", true, "Server URI, default uri: "+config.uri);
        params.addOption("cluster", true, "Cluster ID, default id: " + config.clusterName);
    }

    @Override
    public void parseArgs(final Parameters params) throws IllegalArgumentException {
        topicName =  params.getOptionValue("topic", null);
        if (topicName == null) {
            throw new IllegalArgumentException("Error: Must specify Topic Name");
        }
        config.uri = params.getOptionValue("uri", config.uri);
        config.clusterName = params.getOptionValue("cluster", config.clusterName);
    }

    @Override
    public void openStorage(final Parameters params) throws  IOException {
        optsBuilder =  new Builder();
        optsBuilder.natsUrl(config.uri);
        optsBuilder.maxPubAcksInFlight(config.maxPubAcksInFlight);
    }

    @Override
    public void closeStorage(final Parameters params) throws IOException {
    }

    @Override
    public Writer createWriter(final int id, final Parameters params) {
        try {
            return new NatsStreamWriter(id, params, topicName, config, optsBuilder);
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
            return new NatsStreamCallbackReader(id, params, topicName, topicName + "-" + id,
                    config, optsBuilder);
        } catch (IOException ex) {
            ex.printStackTrace();
            return null;
        }
    }
}
