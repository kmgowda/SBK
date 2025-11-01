/**
 * Copyright (c) KMG. All Rights Reserved..
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.gem.logger;

import io.gem.params.GetArguments;
import io.sbm.logger.RamLogger;

/**
 * Logger contract for SBK-GEM orchestrations.
 *
 * <p>Extends the SBM {@link RamLogger} for RAM-side metrics and {@link GetArguments} to supply
 * CLI arguments required when launching remote SBK instances. Implementations define how
 * metrics are exported (e.g., Prometheus) and what arguments are needed.
 */
public interface GemLogger extends RamLogger, GetArguments {
}
