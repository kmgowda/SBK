/**
 * Copyright (c) KMG. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 */

package io.sbk.ram;

import io.sbk.api.PerformanceLogger;
import io.sbk.perl.ReportLatencies;

public interface RamLogger extends PerformanceLogger, ReportLatencies, SetRW, CountConnections {
}
