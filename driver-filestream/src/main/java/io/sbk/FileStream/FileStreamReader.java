/**
 * Copyright (c) KMG. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 */
package io.sbk.FileStream;

import io.sbk.api.ParameterOptions;
import io.sbk.api.Reader;

import java.io.EOFException;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Arrays;

/**
 * Class for File Reader.
 */
public class FileStreamReader implements Reader<byte[]> {
    private final FileInputStream in;
    private final byte[] readBuffer;

    public FileStreamReader(int id, ParameterOptions params, FileStreamConfig config) throws IOException {
        this.in = new FileInputStream(config.fileName);
        this.readBuffer = new byte[params.getRecordSize()];
    }

    @Override
    public byte[] read() throws EOFException, IOException {
        final int ret = in.read(readBuffer);
        if (ret < 0) {
            throw new EOFException("File Red: EOF");
        } else if (ret < readBuffer.length) {
            return Arrays.copyOf(readBuffer, ret);
        }
        return readBuffer;
    }

    @Override
    public void close() throws IOException {
        in.close();
    }
}