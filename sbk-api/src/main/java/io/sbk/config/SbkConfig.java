/**
 * Copyright (c) KMG. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 */
package io.sbk.config;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.perl.config.PerlConfig;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.dataformat.javaprop.JavaPropsFactory;

import java.io.IOException;
import java.io.InputStream;

/** SBK command defaults and the separately loaded PerL configuration. */
@JsonIgnoreProperties(ignoreUnknown = true)
public final class SbkConfig {
    private static final String CONFIG_FILE = "sbk-command.properties";
    private static final String PERL_CONFIG_FILE = "sbk.properties";
    private static final SbkConfig INSTANCE = loadConfig();

    /** Default read-only mode for mixed writer/reader benchmarks. */
    public boolean defaultReadOnly;
    /** Default writer count when the option is omitted. */
    public int defaultWriters;
    /** Default reader count when the option is omitted. */
    public int defaultReaders;
    /** Default record count when the option is omitted. */
    public long defaultRecords;
    /** Default record size when the option is omitted. */
    public int defaultRecordSize;
    /** Default records per sync when the option is omitted. */
    public int defaultSyncRecords;
    /** Default throughput selector when the option is omitted. */
    public double defaultThroughput;
    /** Default writer ramp step. */
    public int defaultWriterStep;
    /** Default writer ramp interval in seconds. */
    public int defaultWriterStepSeconds;
    /** Default reader ramp step. */
    public int defaultReaderStep;
    /** Default reader ramp interval in seconds. */
    public int defaultReaderStepSeconds;
    /** Default worker idle sleep in milliseconds. */
    public int defaultIdleSleepMillis;
    /** Default thread selector: p, f, or v. */
    public String defaultThreadType;
    @JsonIgnore
    private PerlConfig perlConfig;

    /** Creates an empty configuration for properties binding. */
    public SbkConfig() {
    }

    /**
     * Returns the validated bundled SBK configuration.
     *
     * @return shared SBK configuration
     */
    public static SbkConfig get() {
        return INSTANCE;
    }

    /**
     * Returns the validated PerL settings loaded from the SBK PerL properties resource.
     *
     * @return shared PerL configuration
     */
    public PerlConfig getPerlConfig() {
        return perlConfig;
    }

    /**
     * Loads a fresh validated copy of the bundled PerL settings.
     *
     * <p>Callers may safely apply benchmark-specific command-line overrides to
     * the returned object without mutating the shared {@link #get()} defaults.
     *
     * @return independently mutable validated PerL configuration
     * @throws IOException if the bundled PerL properties are missing or invalid
     * @throws IllegalArgumentException if a bundled PerL value violates SBK constraints
     */
    public static PerlConfig loadPerlConfig() throws IOException {
        try (InputStream input = SbkConfig.class.getClassLoader().getResourceAsStream(PERL_CONFIG_FILE)) {
            if (input == null) {
                throw new IOException("Missing " + PERL_CONFIG_FILE);
            }
            final PerlConfig config = PerlConfig.build(input);
            validatePerlConfig(config);
            return config;
        }
    }

    private static SbkConfig loadConfig() {
        try (InputStream input = SbkConfig.class.getClassLoader().getResourceAsStream(CONFIG_FILE)) {
            if (input == null) {
                throw new IOException("Missing " + CONFIG_FILE);
            }
            final SbkConfig config = new ObjectMapper(new JavaPropsFactory()).readValue(input, SbkConfig.class);
            config.perlConfig = loadPerlConfig();
            config.validate();
            return config;
        } catch (IOException exception) {
            throw new ExceptionInInitializerError(exception);
        }
    }

    private void validate() {
        if (defaultWriters < 0 || defaultReaders < 0 || defaultRecords < 0 || defaultRecordSize < 0
                || defaultSyncRecords < 0 || defaultWriterStep < 1 || defaultWriterStepSeconds < 0
                || defaultReaderStep < 1 || defaultReaderStepSeconds < 0 || defaultIdleSleepMillis < 0
                || defaultThreadType == null || !defaultThreadType.matches("(?i)[pfv]")
                || !Double.isFinite(defaultThroughput) || defaultThroughput < -1) {
            throw new IllegalArgumentException("Invalid SBK defaults in " + CONFIG_FILE);
        }
    }

    private static void validatePerlConfig(PerlConfig config) {
        if (config.qPerWorker < PerlConfig.MIN_Q_PER_WORKER || config.maxQs < 0
                || config.idleTimeoutSeconds < 1) {
            throw new IllegalArgumentException("Invalid PerL defaults in " + PERL_CONFIG_FILE);
        }
    }
}
