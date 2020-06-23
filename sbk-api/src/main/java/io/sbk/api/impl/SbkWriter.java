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
import io.sbk.api.RecordTime;
import io.sbk.api.Worker;
import io.sbk.api.Writer;

import java.io.IOException;

/**
 * Writer Benchmarking Implementation.
 */
public class SbkWriter extends Worker implements Runnable {
    final private static int MS_PER_SEC = 1000;
    final private DataType dType;
    final private Writer writer;
    final private RunBenchmark perf;
    final private Object payload;

    public SbkWriter(int writerID, int idMax, Parameters params, RecordTime recordTime, DataType dType, Writer writer) {
        super(writerID, idMax, params, recordTime);
        this.dType = dType;
        this.writer = writer;
        this.payload = dType.create(params.getRecordSize());
        this.perf = createBenchmark();
    }

    @Override
    public void run()  {
        try {
            perf.run();
        } catch (InterruptedException | IOException ex) {
            ex.printStackTrace();
        }
    }


    private RunBenchmark createBenchmark() {
        final RunBenchmark perfWriter;
        if (params.getSecondsToRun() > 0) {
            if (params.isWriteAndRead()) {
                perfWriter = this::RecordsWriterTimeRW;
            } else {
                if (params.getRecordsPerSec() > 0 || params.getRecordsPerFlush() < Integer.MAX_VALUE) {
                    perfWriter = this::RecordsWriterTimeFlush;
                } else {
                    perfWriter = this::RecordsWriterTime;
                }
            }
        } else {
            if (params.isWriteAndRead()) {
                perfWriter = this::RecordsWriterRW;
            } else {
                if (params.getRecordsPerSec() > 0 || params.getRecordsPerFlush() < Integer.MAX_VALUE) {
                    perfWriter = this::RecordsWriterFlush;
                } else {
                    perfWriter = this::RecordsWriter;
                }
            }
        }
        return perfWriter;
    }

    private void RecordsWriter() throws InterruptedException, IOException {
        writer.RecordsWriter(this, dType, payload);
    }


    private void RecordsWriterFlush() throws InterruptedException, IOException {
        writer.RecordsWriterFlush(this, dType, payload);
    }


    private void RecordsWriterTime() throws InterruptedException, IOException {
        writer.RecordsWriterTime(this, dType, payload);
    }


    private void RecordsWriterTimeFlush() throws InterruptedException, IOException {
        writer.RecordsWriterTimeFlush(this, dType, payload);
    }


    private void RecordsWriterRW() throws InterruptedException, IOException {
        writer.RecordsWriterRW(this, dType, payload);
    }


    private void RecordsWriterTimeRW() throws InterruptedException, IOException {
        writer.RecordsWriterTimeRW(this, dType, payload);
    }

}
