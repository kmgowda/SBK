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

import io.sbk.perl.MetricsConfig;
import io.sbk.perl.impl.PrometheusServer;
import io.sbk.perl.Time;

import java.io.IOException;

public class RWMetricsPrometheusServer extends RWMetricsLogger  {
    final private PrometheusServer server;

    private RWMetricsPrometheusServer(String header, String action, double[] percentiles, Time time,
                                      MetricsConfig config, PrometheusServer server)  {
        super(header, action, percentiles, time, config.latencyTimeUnit, server);
        this.server = server;
    }

    public RWMetricsPrometheusServer(String header, String action, double[] percentiles, Time time,
                                     MetricsConfig config) throws IOException {
        this(header, action, percentiles, time, config, new PrometheusServer(config.port, config.context));
    }

    public void start() throws IOException  {
        server.start();
    }

    public void stop() throws  IOException {
        super.close();
        server.stop();
    }

}
