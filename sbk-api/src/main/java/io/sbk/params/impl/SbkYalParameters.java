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

    /**
     * Create a YAML parameters handler.
     *
     * @param name   benchmark name used in help header
     * @param desc   help description footer
     * @param config YAL configuration holder (provides defaults, receives parsed values)
     */
    public SbkYalParameters(String name, String desc, YalConfig config) {
        super(name, desc);
        this.config = config;
        addOption(YalConfig.FILE_OPTION, true, getFileOptionDescription());
        addOption(YalConfig.PRINT_OPTION, false, getPrintOptionDescription());
    }

    /**
     * Build description text for the {@code -file} option including the default path.
     *
     * @return user-facing description for the file option
     */
    public String getFileOptionDescription() {
        return "SBK YAML file, default: " + config.yamlFileName;
    }

    @Override
    final public String getFileName() {
        return this.config.yamlFileName;
    }

    /**
     * Build description text for the {@code -print} option.
     *
     * @return user-facing description for the print option
     */
    public String getPrintOptionDescription() {
        return "Print SBK Options Help Text";
    }

    /**
     * Parse arguments and update {@link YalConfig#yamlFileName} to reflect any override from CLI.
     */
    @Override
    public void parseArgs(String[] args) throws ParseException, IllegalArgumentException, HelpException {
        super.parseArgs(args);
        config.yamlFileName = getOptionValue(YalConfig.FILE_OPTION, config.yamlFileName);
    }
}
