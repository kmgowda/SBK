/**
 * Copyright (c) KMG. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 */

package io.sbk.api;

import io.sbk.perl.LatencyRecord;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class TransactionRecord {
    public final long transID;
    public final LatencyRecord record;
    public final List<Map<Long, Long>> list;
    public int writers;
    public int readers;
    public int maxWriters;
    public int maxReaders;


    public TransactionRecord(long transID) {
        this.transID = transID;
        this.record = new LatencyRecord();
        this.list = new ArrayList<>();
        this.writers = this.readers = this.maxReaders = this.maxWriters = 0;
    }

}
