/**
 * Copyright (c) 2020 KMG. All Rights Reserved..
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 */

package io.dsb.api;
import java.io.IOException;

/**
 * Abstract class for Benchmarking.
 */
public abstract class Benchmark {

    public abstract void addArgs(final Parameters params);

    public abstract void parseArgs(final Parameters params) throws IllegalArgumentException;

    public abstract void openStorage(final Parameters params) throws IOException;

    public abstract void closeStorage(final Parameters params) throws IOException;

    public abstract Writer createWriter(final int id, QuadConsumer recordTime, final Parameters params);

    public abstract Reader createReader(final int id, QuadConsumer recordTime, final Parameters params);
}
