/**
 * Copyright (c) KMG. All Rights Reserved..
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.sbk.config;

import tools.jackson.databind.ObjectMapper;
import tools.jackson.dataformat.javaprop.JavaPropsFactory;

import java.io.IOException;
import java.io.InputStream;
import java.util.Objects;

/**
 * Class YalConfig.
 */
final public class YalConfig {

    /**
     * <code>FILE_OPTION = "FILE_OPTION";</code>.
     */
    public final static String FILE_OPTION = "f";

    /**
     * <code>FILE_OPTION_ARG = ARG_PREFIX + FILE_OPTION;</code>.
     */
    public final static String FILE_OPTION_ARG = Config.ARG_PREFIX + FILE_OPTION;

    /**
     * <code>PRINT_OPTION = "PRINT_OPTION";</code>.
     */
    public final static String PRINT_OPTION = "p";

    /**
     * <code>PRINT_OPTION_ARG = ARG_PREFIX + PRINT_OPTION;</code>.
     */
    public final static String PRINT_OPTION_ARG = Config.ARG_PREFIX + PRINT_OPTION;

    /**
     * <code>String yamlFileName</code>.
     */
    public String yamlFileName;

    /**
     * Creates an empty YAML launcher configuration for property binding.
     */
    public YalConfig() {
    }

    /**
     * Loads and validates YAML launcher defaults from a class-path resource.
     *
     * @param classLoader class loader that owns the launcher resource
     * @param configFile class-path properties resource name
     * @return validated YAML launcher configuration
     * @throws IOException if the resource is missing or cannot be parsed
     * @throws IllegalArgumentException if a required default is empty
     */
    public static YalConfig load(ClassLoader classLoader, String configFile) throws IOException {
        Objects.requireNonNull(classLoader, "classLoader");
        Objects.requireNonNull(configFile, "configFile");
        try (InputStream input = classLoader.getResourceAsStream(configFile)) {
            if (input == null) {
                throw new IOException("Missing " + configFile);
            }
            final YalConfig config = new ObjectMapper(new JavaPropsFactory()).readValue(input, YalConfig.class);
            if (config.yamlFileName == null || config.yamlFileName.isBlank()) {
                throw new IllegalArgumentException("Invalid YAML launcher defaults in " + configFile);
            }
            return config;
        }
    }

}
