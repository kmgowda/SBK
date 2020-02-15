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

import io.sbk.api.Parameters;
import io.sbk.api.Reader;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.Arrays;

/**
 * Class for File Reader.
 */
public class FileReader implements Reader<byte[]> {
    private final String fileName;
    private final FileInputStream in;
    private final byte[] readBuffer;

    public FileReader(int id, Parameters params, String fileName) throws IOException {
        this.fileName = fileName;
        this.in = new FileInputStream(fileName);
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