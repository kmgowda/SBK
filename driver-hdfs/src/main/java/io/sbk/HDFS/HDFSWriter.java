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
import io.sbk.api.Parameters;
import io.sbk.api.Writer;

import java.io.IOException;
import java.util.concurrent.CompletableFuture;

import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.fs.FSDataOutputStream;

/**
 * Class for HDFS Writer.
 */
public class HDFSWriter implements Writer<byte[]> {
    final private FSDataOutputStream out;

    public HDFSWriter(int id, Parameters params, FileSystem fileSystem, Path filePath) throws IOException {
        out = fileSystem.create(filePath, true);
    }

    @Override
    public CompletableFuture writeAsync(byte[] data) throws IOException {
        out.write(data);
        return null;
    }

    @Override
    public void flush() throws IOException {
        out.flush();
     }

    @Override
    public void close() throws  IOException {
        out.close();
    }
}