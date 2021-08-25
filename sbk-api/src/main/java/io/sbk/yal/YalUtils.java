/**
 * Copyright (c) KMG. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 */

package io.sbk.yal;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class YalUtils {

    public static String[] getYamlArgs(String yamlFileName) throws IOException {
        final ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
        mapper.findAndRegisterModules();

        final YalMap yap = mapper.readValue(new File(yamlFileName), YalMap.class);
        final List<String> lt = new ArrayList<>();
        yap.args.forEach((k, v) -> {
            lt.add("-"+k.strip());
            lt.add(v.replaceAll("\\n+", " ").strip());
        });
        return lt.toArray(new String[0]);
    }
}
