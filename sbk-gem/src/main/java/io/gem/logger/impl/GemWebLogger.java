/**
 * Copyright (c) KMG. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 */
package io.gem.logger.impl;

import io.gem.logger.GemLogger;
import io.sbm.logger.impl.SbmWebLogger;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * GEM adapter for {@link SbmWebLogger}; dashboard rendering and publication remain in the SBM logger.
 */
public final class GemWebLogger extends SbmWebLogger implements GemLogger {

    /**
     * Creates a GEM web logger without starting the dashboard.
     */
    public GemWebLogger() {
    }

    @Override
    protected String getDashboardSource() {
        return "SBK-GEM";
    }

    @Override
    public String[] getOptionsArgs() {
        final List<String> options = new ArrayList<>();
        Collections.addAll(options, "-time", "-minlatency", "-maxlatency", "-csvfile");
        Collections.addAll(options, getDashboardOptionsArgs());
        return options.toArray(String[]::new);
    }

    @Override
    public String[] getParsedArgs() {
        final List<String> arguments = new ArrayList<>();
        if (isCsvEnable()) {
            Collections.addAll(arguments, "-csvfile", getCsvFile());
        }
        Collections.addAll(arguments, "-time", getTimeUnit().name(), "-minlatency",
                Long.toString(getMinLatency()), "-maxlatency", Long.toString(getMaxLatency()));
        Collections.addAll(arguments, getDashboardParsedArgs());
        return arguments.toArray(String[]::new);
    }
}
