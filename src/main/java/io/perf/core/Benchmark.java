/**
 * Copyright (c) 2020 KMG. All Rights Reserved..
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 */
package io.perf.core;

/**
 * Abstract class for Benchmarking.
 */
public abstract class Benchmark {

    public abstract void addArgs(final Parameters params);

    public abstract boolean parseArgs(final Parameters params);

    public abstract boolean openStorage();

    public abstract boolean closeStorage();

    public abstract Writer createWriter(final int id, TriConsumer recordTime , final Parameters params);

    public abstract Reader createReader(final int id, TriConsumer recordTime, final Parameters params);
}
