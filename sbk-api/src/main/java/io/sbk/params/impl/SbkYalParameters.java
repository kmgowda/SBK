/**
 * Copyright (c) KMG. All Rights Reserved..
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.sbk.params.impl;

import io.sbk.config.YalConfig;
import io.sbk.exception.HelpException;
import io.sbk.params.YalParameters;
import org.apache.commons.cli.ParseException;

public class SbkYalParameters extends SbkInputOptions implements YalParameters {
    protected YalConfig config;

    public SbkYalParameters(String name, String desc, YalConfig config) {
        super(name, desc);
        this.config = config;
        this.config.isPrintOption = false;
        addOption(YalConfig.FILE_OPTION, true, getFileOptionDescription());
        addOption(YalConfig.PRINT_OPTION, false, getPrintOptionDescription());
    }

    public String getFileOptionDescription() {
        return "SBK YAML file, default: " + config.yamlFileName;
    }

    @Override
    final public String getFileName() {
        return this.config.yamlFileName;
    }

    public String getPrintOptionDescription() {
        return "Print SBK Options Help Text";
    }

    final public boolean isPrintOption() {
        return this.config.isPrintOption;
    }

    @Override
    public void parseArgs(String[] args) throws ParseException, IllegalArgumentException, HelpException {
        super.parseArgs(args);
        config.yamlFileName = getOptionValue(YalConfig.FILE_OPTION, config.yamlFileName);
        config.isPrintOption = hasOptionValue(YalConfig.PRINT_OPTION);
    }
}
