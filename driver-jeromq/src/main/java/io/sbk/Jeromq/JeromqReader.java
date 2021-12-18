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
import io.sbk.api.Reader;
import org.zeromq.SocketType;
import org.zeromq.ZContext;
import org.zeromq.ZMQ;

import java.io.IOException;

/**
 * Class for Jeromq Reader.
 */
public class JeromqReader implements Reader<byte[]> {
    private final ZContext context;
    private final ZMQ.Socket socket;

    public JeromqReader(int readerId, ParameterOptions params, JeromqConfig config) {
        context = new ZContext();
        socket = context.createSocket(SocketType.REP);
        socket.bind(config.host);
    }

    @Override
    public byte[] read() throws IOException {
        return socket.recv(zmq.ZMQ.ZMQ_DONTWAIT);
    }

    @Override
    public void close() throws IOException {
        socket.close();
    }
}