/**
 * Copyright (c) KMG. All Rights Reserved..
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 */
package io.sbk.Nats;
import io.sbk.api.CallbackReader;
import io.sbk.api.DataReader;
import io.sbk.api.DataWriter;
import io.sbk.api.Storage;
import io.sbk.api.Parameters;
import java.io.IOException;
import io.nats.client.Options;


/**
 * Class for Nats.
 */
public class Nats implements Storage<byte[]> {
    private String topicName;
    private String uri;
    private Options options;

    @Override
    public void addArgs(final Parameters params) throws IllegalArgumentException {
        params.addOption("topic", true, "Topic name");
        params.addOption("uri", true, "Server URI");
    }

    @Override
    public void parseArgs(final Parameters params) throws IllegalArgumentException {
        topicName =  params.getOptionValue("topic", null);
        uri = params.getOptionValue("uri", null);
        if (uri == null) {
            throw new IllegalArgumentException("Error: Must specify Nats server IP address");
        }

        if (topicName == null) {
            throw new IllegalArgumentException("Error: Must specify Topic Name");
        }
        if (params.getReadersCount() < 1 || params.getWritersCount() < 1) {
            throw new IllegalArgumentException("Specify both Writer or readers for NATS Server");
        }
    }

    @Override
    public void openStorage(final Parameters params) throws  IOException {
        options = new Options.Builder().server(uri).maxReconnects(5).build();
    }

    @Override
    public void closeStorage(final Parameters params) throws IOException {
    }

    @Override
    public DataWriter<byte[]> createWriter(final int id, final Parameters params) {
        try {
            return new NatsWriter(id, params, topicName, options);
        } catch (IOException ex) {
            ex.printStackTrace();
            return null;
        }
    }

    @Override
    public DataReader<byte[]> createReader(final int id, final Parameters params) {
        return null;
    }

    @Override
    public CallbackReader<byte[]> createCallbackReader(final int id, final Parameters params) {
        try {
            return new NatsCallbackReader(id, params, topicName, topicName + "-" + id, options);
        } catch (IOException ex) {
            ex.printStackTrace();
            return null;
        }
    }
}
