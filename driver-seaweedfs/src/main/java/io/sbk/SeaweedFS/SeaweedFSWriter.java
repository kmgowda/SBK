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


import io.sbk.api.DataType;
import io.sbk.api.Parameters;
import io.sbk.api.Status;
import io.sbk.api.Writer;
import io.sbk.perl.SendChannel;
import io.sbk.perl.Time;
import org.apache.hadoop.fs.Path;
import seaweed.hdfs.SeaweedOutputStream;
import seaweedfs.client.FilerGrpcClient;

import java.io.IOException;
import java.util.concurrent.CompletableFuture;

/**
 * Class for SeaweedFS Writer.
 */
public class SeaweedFSWriter implements Writer<byte[]> {
    final private SeaweedOutputStream out;
    private Path filePath;

    public SeaweedFSWriter(int id, Parameters params, FilerGrpcClient client, SeaweedFSConfig config) throws IOException {
        filePath = new Path(config.file);
        out = new SeaweedOutputStream(client, filePath, null, 0, params.getRecordSize(), "000");
    }

    @Override
    public void recordWrite(DataType<byte[]> dType, byte[] data, int size, Time time,
                            Status status, SendChannel record, int id) throws IOException {
        status.startTime = time.getCurrentTime();
        out.write(data);
        status.endTime = time.getCurrentTime();
        status.records = 1;
        status.bytes = size;
        record.send(id, status.startTime, status.endTime, size, 1);
    }

    @Override
    public CompletableFuture writeAsync(byte[] data) throws IOException {
        out.write(data, 0, data.length);
        return null;
    }

    @Override
    public void sync() throws IOException {
        out.hflush();
        out.hsync();
    }

    @Override
    public void close() throws  IOException {
        out.close();
    }
}