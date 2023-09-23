/**
 * Copyright (c) KMG. All Rights Reserved..
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.sbk.logger.impl;

/**
 * Class GemRamPrometheusLogger.
 */
public final class GemSbmPrometheusLogger extends AbstractGemLogger {

    @Override
    public String[] getOptionsArgs() {
        return new String[]{"-time", "-minlatency", "-maxlatency", "-csvfile", "-context"};
    }

    @Override
    public String[] getParsedArgs() {
        if (csvEnable) {
            return new String[]{"-csvfile", csvFile,
                    "-time", getTimeUnit().name(),
                    "-minlatency", String.valueOf(getMinLatency()),
                    "-maxlatency", String.valueOf(getMaxLatency()),
                    "-context", metricsConfig.port + metricsConfig.context};

        }
        return new String[]{"-time", getTimeUnit().name(),
                "-minlatency", String.valueOf(getMinLatency()),
                "-maxlatency", String.valueOf(getMaxLatency()),
                "-context", metricsConfig.port + metricsConfig.context};
    }

}
