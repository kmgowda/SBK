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

/**
 * Parameters contract for YAML-based configuration (YAL) support.
 *
 * <p>Implementations provide accessors to retrieve the YAML file name that should be
 * read to load CLI arguments, typically used together with {@link YmlMap} helpers.
 */
public interface YalParameters {

    /**
     * Get the configured YAML file name.
     *
     * @return path or name of the YAML file to read
     */
    String getFileName();
}
