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
 * Configuration for metrics exporters (e.g., Prometheus).
 *
 * Used by {@code io.sbk.logger.impl.PrometheusLogger} to start an HTTP endpoint
 * exposing metrics and to set the latency {@link #latencyTimeUnit}.
 */
public final class MetricsConfig {
    /* HTTP port for the metrics endpoint. */
    public int port;
    /* HTTP path/context under which metrics are served (e.g., "/metrics"). */
    public String context;
    /* Time unit used to publish latency metrics. */
    public TimeUnit latencyTimeUnit;
}
