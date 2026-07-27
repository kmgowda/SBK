/**
 * Copyright (c) KMG. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 */
package io.sbk.driver.PerlBench;

import io.sbk.api.DataReader;
import io.sbk.api.DataWriter;
import io.sbk.api.Storage;
import io.sbk.params.InputOptions;
import io.sbk.params.ParameterOptions;

import java.io.IOException;

/**
 * Synthetic storage driver for measuring SBK and PerL instrumentation
 * overhead without external storage I/O.
 *
 * <p>The writer completes synchronously without work. Each reader returns a
 * worker-owned, preallocated payload. Neither operation allocates, blocks, or
 * introduces shared mutable state on the per-record path.</p>
 */
public final class PerlBench implements Storage<byte[]> {

    /**
     * Register driver-specific arguments.
     *
     * <p>PerlBench intentionally has no driver-specific arguments.</p>
     *
     * @param params command-line option registry
     */
    @Override
    public void addArgs(InputOptions params) {
    }

    /**
     * Parse driver-specific arguments.
     *
     * <p>PerlBench intentionally has no driver-specific arguments.</p>
     *
     * @param params parsed benchmark parameters
     */
    @Override
    public void parseArgs(ParameterOptions params) {
    }

    /**
     * Open the synthetic storage target.
     *
     * @param params parsed benchmark parameters
     * @throws IOException retained by the storage contract
     */
    @Override
    public void openStorage(ParameterOptions params) throws IOException {
    }

    /**
     * Close the synthetic storage target.
     *
     * @param params parsed benchmark parameters
     * @throws IOException retained by the storage contract
     */
    @Override
    public void closeStorage(ParameterOptions params) throws IOException {
    }

    /**
     * Create an allocation-free synchronous writer.
     *
     * @param id writer identifier
     * @param params parsed benchmark parameters
     * @return synthetic writer
     */
    @Override
    public DataWriter<byte[]> createWriter(int id,
                                           ParameterOptions params) {
        return new PerlBenchWriter();
    }

    /**
     * Create a reader with one worker-owned payload.
     *
     * @param id reader identifier
     * @param params parsed benchmark parameters
     * @return synthetic reader
     */
    @Override
    public DataReader<byte[]> createReader(int id,
                                           ParameterOptions params) {
        return new PerlBenchReader(params.getRecordSize());
    }
}
