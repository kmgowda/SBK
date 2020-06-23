/**
 * Copyright (c) KMG. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 */

package io.sbk.api.impl;

import io.sbk.api.DataType;
import io.sbk.api.Parameters;
import io.sbk.api.RecordTime;
import io.sbk.api.Reader;
import io.sbk.api.Worker;
import java.io.IOException;

/**
 * Reader Benchmarking Implementation.
 */
public class SbkReader extends Worker implements Runnable {
    final private DataType dType;
    final private Reader reader;
    final private RunBenchmark perf;

    public SbkReader(int readerId, int idMax, Parameters params, RecordTime recordTime, DataType dType, Reader reader) {
        super(readerId, idMax, params, recordTime);
        this.dType = dType;
        this.reader = reader;
        this.perf = createBenchmark();
    }

    @Override
    public void run() {
        try {
            perf.run();
        } catch (InterruptedException | IOException ex) {
            ex.printStackTrace();
        }
    }

    private RunBenchmark createBenchmark() {
        final RunBenchmark perfReader;
        if (params.getSecondsToRun() > 0) {
            perfReader = params.isWriteAndRead() ? this::RecordsTimeReaderRW : this::RecordsTimeReader;
        } else {
            perfReader = params.isWriteAndRead() ? this::RecordsReaderRW : this::RecordsReader;
        }
        return perfReader;
    }

    private void RecordsReader() throws IOException {
        reader.RecordsReader(this, dType);
    }


    private void RecordsReaderRW() throws IOException {
        reader.RecordsReaderRW(this, dType);
    }

    private void RecordsTimeReader() throws IOException {
        reader.RecordsTimeReader(this, dType);
    }

    private void RecordsTimeReaderRW() throws IOException {
        reader.RecordsTimeReaderRW(this, dType);
    }
}
