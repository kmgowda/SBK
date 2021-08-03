/**
 * Copyright (c) KMG. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 */
package io.sbk.perl;

final public class LatencyPercentiles {
    final public double[] fractions;
    final public long[] latencies;
    final public long[] indexes;
    final public long[] latencyCount;

    public LatencyPercentiles(double[] percentileFractions) {
        this.fractions = percentileFractions;
        this.latencies = new long[this.fractions.length];
        this.indexes = new long[this.fractions.length];
        this.latencyCount = new long[this.fractions.length];
    }

}
