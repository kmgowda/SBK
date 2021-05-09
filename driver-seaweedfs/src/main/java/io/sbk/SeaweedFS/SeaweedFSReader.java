/**
 * Copyright (c) KMG. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 */

package io.sbk.SeaweedFS;

import io.sbk.api.Parameters;
import io.sbk.api.Reader;
import seaweed.hdfs.SeaweedInputStream;
import seaweedfs.client.FilerGrpcClient;

import java.io.IOException;
import java.util.Arrays;

/**
 * Class for SeaweedFS Reader.
 */
public class SeaweedFSReader implements Reader<byte[]> {
    final private SeaweedInputStream in;
    final private byte[] readBuffer;

    public SeaweedFSReader(int id, Parameters params, FilerGrpcClient client, SeaweedFSConfig config) throws IOException {
        this.in =  new SeaweedInputStream(client, null, config.file, null,
                8 * 1024 * 1024, 0);
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