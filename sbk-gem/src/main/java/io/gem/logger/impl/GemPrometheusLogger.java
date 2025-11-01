/**
 * Copyright (c) KMG. All Rights Reserved..
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.gem.logger.impl;

import io.gem.logger.GemLogger;
import io.sbm.logger.impl.SbmPrometheusLogger;

/**
 * Prometheus-backed GEM logger built atop {@link SbmPrometheusLogger}.
 *
 * <p>Provides the CLI arguments expected by GEM when launching remote SBK instances.
 * Options returned by {@link #getOptionsArgs()} are those the logger contributes to the
 * command line. {@link #getParsedArgs()} returns the concrete values based on the current
 * logger configuration (time unit, latency bounds, CSV settings, metrics context).
 */
public final class GemPrometheusLogger extends SbmPrometheusLogger implements GemLogger {

    /**
     * List of logger-specific CLI options that GEM should include when composing the
     * remote SBK command line.
     *
     * @return array of option names (e.g., -time, -minlatency, -maxlatency, -csvfile, -context)
     */
    @Override
    public String[] getOptionsArgs() {
        return new String[]{"-time", "-minlatency", "-maxlatency", "-csvfile", "-context"};
    }

    /**
     * Concrete logger arguments reflecting current configuration.
     *
     * <p>If CSV is enabled, includes the CSV filepath; otherwise omits it. Always includes
     * time unit, latency bounds, and the metrics context (port + context path).
     *
     * @return array of option/value pairs to be appended to the remote SBK command.
     */
    @Override
    public String[] getParsedArgs() {
        if (isCsvEnable()) {
            return new String[]{"-csvfile", getCsvFile(),
                    "-time", getTimeUnit().name(),
                    "-minlatency", String.valueOf(getMinLatency()),
                    "-maxlatency", String.valueOf(getMaxLatency()),
                    "-context", getMetricsConfig().port + getMetricsConfig().context};

        }
        return new String[]{"-time", getTimeUnit().name(),
                "-minlatency", String.valueOf(getMinLatency()),
                "-maxlatency", String.valueOf(getMaxLatency()),
                "-context", getMetricsConfig().port + getMetricsConfig().context};
    }

}
