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

import io.sbk.api.DataReader;
import io.sbk.api.DataType;
import io.sbk.api.Parameters;
import io.sbk.api.RateController;
import io.sbk.api.RunBenchmark;
import io.sbk.api.SendChannel;
import io.sbk.api.Time;
import io.sbk.api.Worker;

import java.io.EOFException;
import java.io.IOException;

/**
 * Reader Benchmarking Implementation.
 */
public class SbkReader extends Worker implements RunBenchmark {
    final private DataType<Object> dType;
    final private DataReader<Object> reader;
    final private Time time;
    final private RateController rCnt;
    final private RunBenchmark perf;

    public SbkReader(int readerId, int idMax, Parameters params, SendChannel sendChannel,
                     DataType<Object> dType, Time time, DataReader<Object> reader) {
        super(readerId, idMax, params, sendChannel);
        this.dType = dType;
        this.reader = reader;
        this.time = time;
        this.rCnt = new SbkRateController();
        this.perf = createBenchmark();
    }

    @Override
    public void run() throws EOFException, IOException {
        perf.run();
     }

    private RunBenchmark createBenchmark() {
        final RunBenchmark perfReader;
        if (params.getSecondsToRun() > 0) {
            if (params.isWriteAndRead() ) {
                if (params.getRecordsPerSec() > 0) {
                    perfReader = this::RecordsTimeReaderRWRateControl;
                } else {
                    perfReader = this::RecordsTimeReaderRW;
                }
            } else {
                if (params.getRecordsPerSec() > 0) {
                    perfReader = this::RecordsTimeReaderRateControl;
                } else {
                    perfReader = this::RecordsTimeReader;
                }
            }
        } else {
            if (params.isWriteAndRead() ) {
                if (params.getRecordsPerSec() > 0) {
                    perfReader = this::RecordsReaderRWRateControl;
                } else {
                    perfReader = this::RecordsReaderRW;
                }
            } else {
                if (params.getRecordsPerSec() > 0) {
                    perfReader = this::RecordsReaderRateControl;
                } else {
                    perfReader = this::RecordsReader;
                }
            }
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

    private void RecordsReaderRateControl() throws EOFException, IOException {
        reader.RecordsReaderRateControl(this, dType, time, rCnt);
    }

    private void RecordsReaderRWRateControl() throws EOFException, IOException {
        reader.RecordsReaderRWRateControl(this, dType, time, rCnt);
    }

    private void RecordsTimeReaderRateControl() throws EOFException, IOException {
        reader.RecordsTimeReaderRateControl(this, dType, time, rCnt);
    }

    private void RecordsTimeReaderRWRateControl() throws EOFException, IOException {
        reader.RecordsTimeReaderRWRateControl(this, dType, time, rCnt);
    }

}
