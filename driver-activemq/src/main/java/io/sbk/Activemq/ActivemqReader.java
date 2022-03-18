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

import io.sbk.api.ParameterOptions;
import io.sbk.api.Reader;

import java.io.IOException;

/**
 * Class for Activemq Reader.
 */
public class ActivemqReader implements Reader<byte[]> {

    public ActivemqReader(int readerId, ParameterOptions params, ActivemqConfig config) {
    }

    @Override
    public byte[] read() throws IOException {
        throw new IOException("The Activemq Reader Driver not defined");
    }

    @Override
    public void close() throws IOException {
        throw new IOException("The Activemq Reader Driver not defined");
    }
}