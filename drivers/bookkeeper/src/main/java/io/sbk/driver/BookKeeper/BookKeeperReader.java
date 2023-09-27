/**
 * Copyright (c) KMG. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 */
package io.sbk.driver.BookKeeper;

import io.sbk.api.Reader;
import org.apache.distributedlog.DLSN;
import org.apache.distributedlog.LogRecord;
import org.apache.distributedlog.api.DistributedLogManager;
import org.apache.distributedlog.api.LogReader;

import java.io.IOException;

/**
 * Class for BookKeeper Reader.
 */
public class BookKeeperReader implements Reader<byte[]> {
    private LogReader reader;

    public BookKeeperReader(DistributedLogManager dlm) throws IOException {
        reader = dlm.openLogReader(DLSN.InitialDLSN);
    }

    @Override
    public byte[] read() throws IOException {
        LogRecord record = reader.readNext(true);
        if (record != null) {
            return record.getPayload();
        }
        return null;
    }

    @Override
    public void close() throws IOException {
        reader.close();
    }
}