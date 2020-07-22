/**
 * Copyright (c) KMG. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 */
package io.sbk.File;

import io.sbk.api.DataType;
import io.sbk.api.Parameters;
import io.sbk.api.Reader;


import java.io.EOFException;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;

/**
 * Class for File Reader.
 */
public class FileReader implements Reader<ByteBuffer> {
    final private FileChannel in;
    final private ByteBuffer readBuffer;

    public FileReader(int id, Parameters params, DataType<ByteBuffer> dType, FileConfig config) throws IOException {
        this.in = FileChannel.open(Paths.get(config.fileName), StandardOpenOption.READ);
        this.readBuffer = dType.create(params.getRecordSize());
    }

    @Override
    public ByteBuffer read() throws IOException {
        ByteBuffer retBuffer = readBuffer.slice();
        final int ret = in.read(retBuffer);
        if (ret <= 0) {
            throw new EOFException("File Red: EOF");
        }
        return retBuffer.flip();
    }

    @Override
    public void close() throws  IOException {
        in.close();
    }
}