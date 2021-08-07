/**
 * Copyright (c) KMG. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 */
package io.sbk.HDFS;

import io.sbk.parameters.ParameterOptions;
import io.sbk.api.Reader;

import java.io.IOException;
import java.util.Arrays;

import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.fs.FSDataInputStream;
/**
 * Class for File Reader.
 */
public class HDFSReader implements Reader<byte[]> {
    final private FSDataInputStream in;
    final private byte[] readBuffer;

    public HDFSReader(int id, ParameterOptions params, FileSystem fileSystem, Path filePath) throws IOException {
        this.in = fileSystem.open(filePath);
        this.readBuffer = new byte[params.getRecordSize()];
    }

    @Override
    public byte[] read() throws IOException {
        final int ret = in.read(readBuffer);
        if (ret < 0) {
            return null;
        } else if (ret < readBuffer.length) {
            return Arrays.copyOf(readBuffer, ret);
        }
        return  readBuffer;
    }

    @Override
    public void close() throws  IOException {
        in.close();
    }
}