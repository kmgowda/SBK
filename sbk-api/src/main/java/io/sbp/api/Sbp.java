/**
 * Copyright (c) KMG. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.sbp.api;


import tools.jackson.databind.ObjectMapper;
import tools.jackson.dataformat.javaprop.JavaPropsFactory;
import io.sbp.config.SbpVersion;

import java.io.IOException;

/**
 * Loads the Storage Benchmark Protocol version used by SBK and SBM.
 */
public class Sbp {
    final private static String VERSION_FILE = "sbp-version.properties";

    /**
     * Creates an SBP version-resource accessor.
     */
    public Sbp() {
    }

    /**
     * Loads the protocol version from the bundled properties resource.
     *
     * @return configured SBP version
     * @throws IOException when the version resource cannot be read
     */
    public static SbpVersion getVersion() throws IOException {
        final ObjectMapper mapper = new ObjectMapper(new JavaPropsFactory());
        return mapper.readValue(Sbp.class.getClassLoader().getResourceAsStream(VERSION_FILE),
                SbpVersion.class);
    }
}
