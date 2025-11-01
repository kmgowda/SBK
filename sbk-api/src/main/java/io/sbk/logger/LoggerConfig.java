/**
 * Copyright (c) KMG. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 */
package io.sbk.logger;

import io.time.TimeUnit;

/**
 * Configuration model for logger behavior and latency bounds.
 *
 * Typically loaded from properties and used by implementations of
 * {@code io.sbk.logger.impl.AbstractRWLogger} to initialize CLI defaults and
 * runtime behavior.
 */
public final class LoggerConfig {
    /* Reporting interval in seconds for periodic prints. */
    public int reportingSeconds;
    /* Time unit used to interpret configured min/max latency. */
    public TimeUnit timeUnit;
    /* Default minimum latency bound expressed in {@link #timeUnit}. */
    public long minLatency;
    /* Default maximum latency bound expressed in {@link #timeUnit}. */
    public long maxLatency;
    /* Comma-separated percentile list (e.g., "50, 75, 95, 99, 99.9"). */
    public String percentiles;
}
