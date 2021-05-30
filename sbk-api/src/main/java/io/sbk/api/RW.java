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

public class RW {
    public int readers;
    public int writers;
    public int maxReaders;
    public int maxWriters;

    public RW() {
        reset();
    }

    public void reset() {
        readers = writers = maxWriters = maxReaders = 0;
    }

    public void resetRW() {
        readers = writers = 0;
    }

    public void update(int readers, int writers, int maxReaders, int maxWriters) {
        this.readers = readers;
        this.writers = writers;
        this.maxReaders = Math.max(this.maxReaders, maxReaders);
        this.maxWriters = Math.max(this.maxWriters, maxWriters);
    }

    public void update(RW rw) {
        update(rw.readers, rw.writers, rw.maxReaders, rw.maxWriters);
    }
}
