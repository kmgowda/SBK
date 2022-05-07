/**
 * Copyright (c) KMG. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.sbk.options;

import io.sbk.exception.HelpException;
import org.apache.commons.cli.ParseException;

/**
 * Interface InputOptions.
 */
public non-sealed interface ParseInputOptions extends InputOptions, ParsedOptions {

    /**
     * Parse the command line arguments.
     *
     * @param args list of command line arguments.
     * @throws IllegalArgumentException If an exception occurred.
     * @throws ParseException           If an exception occurred.
     * @throws HelpException            If the 'help' option is supplied.
     */
    void parseArgs(String[] args) throws ParseException, IllegalArgumentException, HelpException;
}
