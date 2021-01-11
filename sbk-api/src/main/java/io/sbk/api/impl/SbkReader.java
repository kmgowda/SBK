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
import io.sbk.api.SendChannel;
import io.sbk.api.Reader;
import io.sbk.api.Time;
import io.sbk.api.Worker;

import java.io.EOFException;
import java.io.IOException;

/**
 * Reader Benchmarking Implementation.
 */
public class SbkReader extends Worker implements RunBenchmark {
    final private DataType<Object> dType;
    final private Reader<Object> reader;
    final private Time time;
    final private RunBenchmark perf;

    public SbkReader(int readerId, int idMax, Parameters params, SendChannel sendChannel,
                     DataType<Object> dType, Time time, Reader<Object> reader) {
        super(readerId, idMax, params, sendChannel);
        this.dType = dType;
        this.reader = reader;
        this.time = time;
        this.perf = createBenchmark();
    }

    @Override
    public void run() throws EOFException, IOException {
        perf.run();
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

    private void RecordsReader() throws EOFException, IOException {
        reader.RecordsReader(this, dType, time);
    }


    private void RecordsReaderRW() throws EOFException, IOException {
        reader.RecordsReaderRW(this, dType, time);
    }

    private void RecordsTimeReader() throws EOFException, IOException {
        reader.RecordsTimeReader(this, dType, time);
    }

    private void RecordsTimeReaderRW() throws EOFException, IOException {
        reader.RecordsTimeReaderRW(this, dType, time);
    }
}
