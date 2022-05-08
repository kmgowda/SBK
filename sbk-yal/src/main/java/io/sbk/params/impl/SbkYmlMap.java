/**
 * Copyright (c) KMG. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 */


package io.sbk.params.impl;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.sbk.params.YmlMap;

import java.util.Map;

/**
 * Class SbkYmlMap.
 */
public final class SbkYmlMap extends YmlMap {

    /**
     * Passing args to its super class YmlMap.
     *
     * @param args JsonProperty("sbkArgs") Map{String, String}
     */
    @JsonCreator
    public SbkYmlMap(@JsonProperty("sbkArgs") Map<String, String> args) {
        super(args);
    }
}
