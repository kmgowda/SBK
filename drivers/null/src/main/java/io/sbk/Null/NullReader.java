/**
 * Copyright (c) KMG. All Rights Reserved..
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.sbk.Null;

import io.sbk.api.Reader;

import java.io.IOException;

/**
 * Class for Reader.
 */
public class NullReader implements Reader<byte[]> {
    private final int timeoutMS;

    public NullReader(int timeoutMS) {
        this.timeoutMS = timeoutMS;
    }

    @Override
    public byte[] read() throws IOException {
        try {
            Thread.sleep(timeoutMS);
        } catch (InterruptedException ex) {
            throw new IOException(ex);
        }
        return null;
    }

    @Override
    public void close() throws IOException {

    }
}