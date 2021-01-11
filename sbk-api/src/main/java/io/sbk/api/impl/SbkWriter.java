/**
 * Copyright (c) KMG. All Rights Reserved..
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
import io.sbk.api.RateController;
import io.sbk.api.SendChannel;
import io.sbk.api.Time;
import io.sbk.api.Worker;
import io.sbk.api.Writer;

import java.io.IOException;

/**
 * Writer Benchmarking Implementation.
 */
public class SbkWriter extends Worker implements RunBenchmark {
    final private DataType<Object> dType;
    final private Writer<Object> writer;
    final private Time time;
    final private RunBenchmark perf;
    final private RateController rCnt;
    final private Object payload;
    final private int dataSize;

    public SbkWriter(int writerID, int idMax, Parameters params, SendChannel sendChannel,
                     DataType<Object> dType, Time time, Writer<Object> writer) {
        super(writerID, idMax, params, sendChannel);
        this.dType = dType;
        this.writer = writer;
        this.time = time;
        this.perf = createBenchmark();
        this.rCnt = new SbkRateController();
        this.payload = dType.create(params.getRecordSize());
        this.dataSize = dType.length(this.payload);
    }

    @Override
    public void run() throws IOException {
        perf.run();
    }


    private RunBenchmark createBenchmark() {
        final RunBenchmark perfWriter;
        if (params.getSecondsToRun() > 0) {
            if (params.isWriteAndRead()) {
                perfWriter = this::RecordsWriterTimeRW;
            } else {
                if (params.getRecordsPerSec() > 0 || params.getRecordsPerSync() < Integer.MAX_VALUE) {
                    perfWriter = this::RecordsWriterTimeSync;
                } else {
                    perfWriter = this::RecordsWriterTime;
                }
            }
        } else {
            if (params.isWriteAndRead()) {
                perfWriter = this::RecordsWriterRW;
            } else {
                if (params.getRecordsPerSec() > 0 || params.getRecordsPerSync() < Integer.MAX_VALUE) {
                    perfWriter = this::RecordsWriterSync;
                } else {
                    perfWriter = this::RecordsWriter;
                }
            }
        }
        return perfWriter;
    }

    private void RecordsWriter() throws  IOException {
        writer.RecordsWriter(this, dType, payload, dataSize, time);
    }


    private void RecordsWriterSync() throws  IOException {
        writer.RecordsWriterSync(this, dType, payload, dataSize, time, rCnt);
    }


    private void RecordsWriterTime() throws  IOException {
        writer.RecordsWriterTime(this, dType, payload, dataSize, time);
    }


    private void RecordsWriterTimeSync() throws IOException {
        writer.RecordsWriterTimeSync(this, dType, payload, dataSize, time, rCnt);
    }


    private void RecordsWriterRW() throws IOException {
        writer.RecordsWriterRW(this, dType, payload, dataSize, time, rCnt);
    }


    private void RecordsWriterTimeRW() throws IOException {
        writer.RecordsWriterTimeRW(this, dType, payload, dataSize, time, rCnt);
    }

}
