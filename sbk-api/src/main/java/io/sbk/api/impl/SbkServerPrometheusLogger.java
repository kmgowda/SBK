/**
 * Copyright (c) KMG. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 */
package io.sbk.api.impl;

import io.sbk.api.Action;
import io.sbk.api.Config;
import io.sbk.api.ServerLogger;
import io.sbk.api.InputOptions;
import io.sbk.perl.Time;
import io.sbk.system.Printer;
import java.io.IOException;


/**
 * Class for Recoding/Printing benchmark results on micrometer Composite Meter Registry.
 */
public class SbkServerPrometheusLogger extends SbkPrometheusLogger implements ServerLogger {
    final static String CONFIG_FILE = "server-metrics.properties";
    private ConnectionsRWMetricsPrometheusServer prometheusServer;

    public SbkServerPrometheusLogger() {
        super();
        prometheusServer = null;
    }

    @Override
    public String getConfigFile() {
        return CONFIG_FILE;
    }

    @Override
    public RWMetricsPrometheusServer getMetricsPrometheusServer() throws IOException {
        if (prometheusServer == null) {
            prometheusServer = new ConnectionsRWMetricsPrometheusServer(Config.NAME + " " + storageName, action.name(),
                    percentiles, time, config);
        }
        return prometheusServer;
    }


    @Override
    public void open(final InputOptions params, final String storageName, Action action, Time time) throws IllegalArgumentException, IOException {
        super.open(params, storageName, action, time);
        Printer.log.info("SBK Connections PrometheusLogger Started");
    }


    @Override
    public void increment(int val) {
        prometheusServer.increment(val);
    }

    @Override
    public void decrement(int val) {
        prometheusServer.decrement(val);
    }
}
