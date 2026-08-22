/**
 * Copyright (c) KMG. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.sbk.logger.impl;

import javax.annotation.concurrent.NotThreadSafe;

/**
 * Logger implementation that prints periodic and total benchmark results to {@code System.out}.
 *
 * <p>The per-latency callback is intentionally a final no-op. System-output loggers and their
 * CSV, SLF4J, Prometheus, Web Console, and SBM subclasses must not add work to the PerL
 * performance-recorder path.</p>
 */
@NotThreadSafe
public class SystemLogger extends AbstractSystemLogger {

    /** Creates a system-output benchmark logger. */
    public SystemLogger() {
        super();
    }

    /**
     * Intentionally ignores the individual latency callback.
     *
     * @param startTime event start time
     * @param events number of events represented by this measurement
     * @param bytes number of bytes represented by this measurement
     * @param latency measured latency
     */
    @Override
    public final void recordLatency(long startTime, int events, int bytes, long latency) {
    }

    @Override
    public final boolean recordsIndividualLatencies() {
        return false;
    }
}
