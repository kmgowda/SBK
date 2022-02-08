/**
 * Copyright (c) KMG. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.perl.logger.impl;

import io.perl.api.ReportLatency;

public class DefaultLogger extends ResultsLogger implements ReportLatency {

    @Override
    public final void recordLatency(long startTime, int bytes, int events, long latency) {

    }
}
