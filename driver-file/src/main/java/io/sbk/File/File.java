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

import io.sbk.api.Storage;
import io.sbk.api.Parameters;
import io.sbk.api.Writer;
import io.sbk.api.Reader;

import java.io.IOException;

/**
 * Class for File System Benchmarking.
 */
public class File implements Storage<byte[]> {
    private String fileName;
    private boolean sync;

    @Override
    public void addArgs(final Parameters params) {
        params.addOption("file", true, "File name");
        params.addOption("sync", true, "sync to storage device; only for writer");
    }

    @Override
    public void parseArgs(final Parameters params) throws IllegalArgumentException {
        fileName =  params.getOptionValue("file", null);
        sync = Boolean.parseBoolean(params.getOptionValue("sync", "false"));

        if (fileName == null) {
            throw new IllegalArgumentException("Error: Must specify file Name");
        }
        if (params.getWritersCount() > 1) {
            throw new IllegalArgumentException("Writers should be only 1 for File writing");
        }
        if (params.getReadersCount() > 0 && params.getWritersCount() > 0) {
            throw new IllegalArgumentException("Specify either Writer or readers ; both are not allowed");
        }
    }

    @Override
    public void openStorage(final Parameters params) throws  IOException {

    }

    @Override
    public void closeStorage(final Parameters params) throws IOException {

    }

    @Override
    public Writer createWriter(final int id, final Parameters params) {
        try {
            return new FileWriter(id, params, fileName, sync);
        } catch (IOException ex) {
            ex.printStackTrace();
            return null;
        }
    }

    @Override
    public Reader createReader(final int id, final Parameters params) {
        try {
            return new FileReader(id, params, fileName);
        } catch (IOException ex) {
            ex.printStackTrace();
            return null;
        }
    }
}



