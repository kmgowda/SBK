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

import io.perl.config.LatencyConfig;
import java.text.DecimalFormat;

/**
 *  Class Metrics.
 */
public abstract sealed class Metrics permits PrintMetrics {
    /**
     * <code>String metricPrefix</code>.
     */
    final protected String metricPrefix;
    /**
     * <code>DecimalFormat percentileFormat</code>.
     */
    final protected DecimalFormat percentileFormat;
    /**
     * <code>String metricTimeUnit</code>.
     */
    final protected String metricTimeUnit;
    /**
     * <code>String bytesName</code>.
     */
    final protected String bytesName;
    /**
     * <code>String recordsName</code>.
     */
    final protected String recordsName;
    /**
     * <code>String mbPsecName</code>.
     */
    final protected String mbPsecName;
    /**
     * <code>String recsPsecName</code>.
     */
    final protected String recsPsecName;
    /**
     * <code>String avgLatencyName</code>.
     */
    final protected String avgLatencyName;

    /**
     * <code>String minLatencyName</code>.
     */
    final protected String minLatencyName;

    /**
     * <code>String maxLatencyName</code>.
     */
    final protected String maxLatencyName;
    /**
     * <code>String invalidLatencyRecordsName</code>.
     */
    final protected String invalidLatencyRecordsName;
    /**
     * <code>String lowerDiscardName</code>.
     */
    final protected String lowerDiscardName;
    /**
     * <code>String higherDiscardName</code>.
     */
    final protected String higherDiscardName;
    /**
     * <code>String slc1Name</code>.
     */
    final protected String slc1Name;
    /**
     * <code>String slc2Name</code>.
     */
    final protected String slc2Name;
    /**
     * <code>String[] percentileNames</code>.
     */
    final protected String[] percentileNames;

    /**
     * Constructor Metrics for initializing All Values.
     *
     * @param prefixName        String
     * @param timeUnitName      String
     * @param percentiles       double[]
     */
    public Metrics(String prefixName, String timeUnitName, double[] percentiles) {
        metricPrefix = prefixName.replace(" ", "_");
        metricTimeUnit = timeUnitName.replace(" ", "_");
        percentileFormat = new DecimalFormat(LatencyConfig.PERCENTILE_FORMAT);
        bytesName = metricPrefix + "_Bytes";
        recordsName = metricPrefix + "_Records";
        mbPsecName = metricPrefix + "_MBPerSec";
        recsPsecName = metricPrefix + "_RecordsPerSec";
        avgLatencyName = metricPrefix + "_" + metricTimeUnit + "_AvgLatency";
        minLatencyName = metricPrefix+"_"+metricTimeUnit+"_MinLatency";
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
