/**
 * Copyright (c) KMG. All Rights Reserved..
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.sbk.gem.impl;

import io.sbk.config.YalConfig;
import io.sbk.params.impl.SbkYalParameters;

/**
 * Class SbkGemYalParameters.
 */
public final class SbkGemYalParameters extends SbkYalParameters {

    /**
     * Constructor SbkGemYalParameters pass all values to its super class SbkYalParameters.
     *
     * @param name      String
     * @param desc      String
     * @param config    YalConfig
     */
    public SbkGemYalParameters(String name, String desc, YalConfig config) {
        super(name, desc, config);
    }

    @Override
    public String getFileOptionDescription() {
        return "SBK-GEM YAML file, default: " + config.yamlFileName;
    }

    @Override
    public String getPrintOptionDescription() {
        return "Print SBK-GEM Options Help Text";
    }
}

