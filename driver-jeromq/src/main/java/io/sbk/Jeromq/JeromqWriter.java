/**
 * Copyright (c) KMG. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 */
package io.sbk.Jeromq;

import io.sbk.api.ParameterOptions;
import io.sbk.api.Writer;
import org.zeromq.SocketType;
import org.zeromq.ZContext;
import org.zeromq.ZMQ;

import java.io.IOException;
import java.util.concurrent.CompletableFuture;


/**
 * Class for Jeromq Writer.
 */
public class JeromqWriter implements Writer<byte[]> {
    private final ZContext  context;
    private final ZMQ.Socket socket;

    public JeromqWriter(int writerID, ParameterOptions params, JeromqConfig config) {
        context = new ZContext();
        socket = context.createSocket(SocketType.REQ);
        socket.connect(config.host);
    }

    @Override
    public CompletableFuture writeAsync(byte[] data) throws IOException {
            socket.send(data, zmq.ZMQ.ZMQ_DONTWAIT );
            return null;
    }

    @Override
    public void sync() throws IOException {

    }

    @Override
    public void close() throws IOException {
        socket.close();
    }
}