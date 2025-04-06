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

import io.time.TimeUnit;

/**
 * Class DefaultLogger.
 */
public class DefaultLogger extends ResultsLogger {

    /**
     * Constructor DefaultLogger pass all values to its super class.
     *
     * @param header                String
     * @param percentiles           double[]
     * @param latencyTimeUnit       TimeUnit
     * @param minLatency            long
     * @param maxLatency            long
     */
    public DefaultLogger(String header, double[] percentiles, TimeUnit latencyTimeUnit,
                         long minLatency, long maxLatency) {
        super(header, percentiles, latencyTimeUnit, minLatency, maxLatency);
    }

    /**
     * Constructor DefaultLogger takes no arguments but initialize all values with default values.
     */
    public DefaultLogger() {
        super();
    }

}
