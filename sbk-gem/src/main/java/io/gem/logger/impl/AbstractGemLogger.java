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
 * Base class for GEM loggers built on top of {@link SbmPrometheusLogger}.
 *
 * <p>Provides the SBM metrics implementation and leaves argument exposure to subclasses
 * via {@link #getOptionsArgs()} and {@link #getParsedArgs()} so orchestrators can pass
 * appropriate CLI parameters when launching remote SBK instances.
 */
public abstract class AbstractGemLogger extends SbmPrometheusLogger implements GemLogger {
    @Override
    public abstract String[] getOptionsArgs();

    @Override
    public abstract String[] getParsedArgs();
}
