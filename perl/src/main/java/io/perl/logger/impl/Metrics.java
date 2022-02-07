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

import io.perl.api.LatencyConfig;
import java.text.DecimalFormat;

public abstract sealed class Metrics permits PrintMetrics {
    final protected String metricPrefix;
    final protected DecimalFormat percentileFormat;
    final protected String metricTimeUnit;
    final protected String bytesName;
    final protected String recordsName;
    final protected String mbPsecName;
    final protected String recsPsecName;
    final protected String avgLatencyName;
    final protected String maxLatencyName;
    final protected String invalidLatencyRecordsName;
    final protected String lowerDiscardName;
    final protected String higherDiscardName;
    final protected String slc1Name;
    final protected String slc2Name;
    final protected String[] percentileNames;

    public Metrics(String prefixName, String timeUnitName, double[] percentiles) {
        metricPrefix = prefixName.replace(" ", "_");
        metricTimeUnit = timeUnitName.replace(" ", "_");
        percentileFormat = new DecimalFormat(LatencyConfig.PERCENTILE_FORMAT);
        bytesName = metricPrefix + "_Bytes";
        recordsName = metricPrefix + "_Records";
        mbPsecName = metricPrefix + "_MBPerSec";
        recsPsecName = metricPrefix + "_RecordsPerSec";
        avgLatencyName = metricPrefix + "_" + metricTimeUnit + "_AvgLatency";
        maxLatencyName = metricPrefix + "_" + metricTimeUnit + "_MaxLatency";
        invalidLatencyRecordsName = metricPrefix + "_InvalidLatencyRecords";
        lowerDiscardName = metricPrefix + "_LowerDiscardedLatencyRecords";
        higherDiscardName = metricPrefix + "_HigherDiscardLatencyRecords";
        slc1Name = metricPrefix + "_SLC_1";
        slc2Name = metricPrefix + "_SLC_2";
        percentileNames = new String[percentiles.length];
        for (int i = 0; i < percentiles.length; i++) {
            this.percentileNames[i] = metricPrefix + "_" + metricTimeUnit + "_" + percentileFormat.format(percentiles[i]);
        }
    }

}
