/**
 * Copyright (c) KMG. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.sbk.params;

import tools.jackson.databind.ObjectMapper;
import tools.jackson.dataformat.yaml.YAMLFactory;
import io.sbk.utils.SbkUtils;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.IOException;
import java.util.Map;

/**
 * Helper base class for mapping YAML files to CLI argument arrays.
 *
 * <p>Subclasses represent the expected YAML structure. {@link #getYmlArgs(String, Class)}
 * reads a YAML file into the given {@code YmlMap} subclass and converts its {@link #args}
 * map into a {@code String[]} suitable for feeding into SBK's CLI parser.
 */
public abstract class YmlMap {
    /** Key-value pairs that will be transformed into CLI arguments. */
    public Map<String, String> args;

    /**
     * Create a {@code YmlMap} with the provided arguments map.
     *
     * @param args mapping of option name to value
     */
    public YmlMap(Map<String, String> args) {
        this.args = args;
    }

    /**
     * Read a YAML file and convert its args map into a CLI argument array.
     *
     * @param fileName path to YAML file
     * @param tClass   concrete {@code YmlMap} subclass to deserialize into
     * @return array of CLI arguments (e.g., ["-option", "value", ...])
     * @throws IOException if the YAML file cannot be read or parsed
     */
    public static @NotNull String[] getYmlArgs(String fileName, Class<? extends YmlMap> tClass) throws IOException {
        final ObjectMapper mapper = new ObjectMapper(new YAMLFactory());

        final YmlMap yap = mapper.readValue(new File(fileName), tClass);
        return SbkUtils.mapToArgs(yap.args, true);
    }
}
