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

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public abstract class YmlMap {
    public Map<String, String> args;

    public YmlMap(Map<String, String> args) {
        this.args = args;
    }

    public static @NotNull String[] getYmlArgs(String fileName, Class<? extends YmlMap> tClass) throws IOException {
        final ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
        mapper.findAndRegisterModules();

        final YmlMap yap = mapper.readValue(new File(fileName), tClass);
        final List<String> lt = new ArrayList<>();
        yap.args.forEach((k, v) -> {
            lt.add("-" + k.strip());
            lt.add(v.replaceAll("\\n+", " ").strip());
        });
        return lt.toArray(new String[0]);
    }
}
