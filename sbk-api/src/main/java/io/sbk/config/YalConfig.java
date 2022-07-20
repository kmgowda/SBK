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
    public boolean isPrintOption;
}
