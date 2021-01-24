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

import io.sbk.api.DataReader;
import io.sbk.api.DataWriter;
import io.sbk.api.Storage;
import io.sbk.api.Parameters;

import java.io.IOException;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;

/**
 * Class for HDFS Benchmarking.
 */
public class HDFS implements Storage<byte[]> {
    private static final String FSTYPE = "fs.defaultFS";
    private String fsType;
    private String fileName;
    private String uri;
    private FileSystem fileSystem;
    private Path filePath;
    private boolean recreate;

    @Override
    public void addArgs(final Parameters params) throws IllegalArgumentException {
        params.addOption("fs", true, "File System Type, default: "+ FSTYPE);
        params.addOption("file", true, "File name");
        params.addOption("uri", true, "URI");
        params.addOption("recreate", true,
                "If the file is already existing, delete and recreate the same; only for writer");
    }

    @Override
    public void parseArgs(final Parameters params) throws IllegalArgumentException {
        fsType =  params.getOptionValue("fs", FSTYPE);
        fileName =  params.getOptionValue("file", null);
        uri =  params.getOptionValue("uri", null);
        recreate = Boolean.parseBoolean(params.getOptionValue("recreate", "false"));

        if (uri == null) {
            throw new IllegalArgumentException("Error: Must specify URI IP");
        }

        if (fileName == null) {
            throw new IllegalArgumentException("Error: Must specify file Name");
        }
        if (params.getReadersCount() > 0 && params.getWritersCount() > 0) {
            throw new IllegalArgumentException("Specify either Writer or readers ; both are not allowed");
        }
    }

    @Override
    public void openStorage(final Parameters params) throws  IOException {
        Configuration configuration = new Configuration();
        configuration.set(fsType, uri);
        fileSystem = FileSystem.get(configuration);
        filePath = new Path(fileName);
        if (recreate && params.getWritersCount() > 0) {
            try {
                fileSystem.delete(filePath, true);
            } catch (IOException ex) {
                // Ignore the error
            }
        }
    }

    @Override
    public void closeStorage(final Parameters params) throws IOException {
        fileSystem.close();
    }

    @Override
    public DataWriter<byte[]> createWriter(final int id, final Parameters params) {
        try {
            return new HDFSWriter(id, params, fileSystem, filePath, recreate);
        } catch (IOException ex) {
            ex.printStackTrace();
            return null;
        }
    }

    @Override
    public DataReader<byte[]> createReader(final int id, final Parameters params) {
        try {
            return new HDFSReader(id, params, fileSystem, filePath);
        } catch (IOException ex) {
            ex.printStackTrace();
            return null;
        }
    }
}