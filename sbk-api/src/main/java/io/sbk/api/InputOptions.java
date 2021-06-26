/**
 * Copyright (c) KMG. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 */

package io.sbk.api;

import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

public interface InputOptions {

    /**
     * Add the driver specific command line arguments.
     * @param name Name of the parameter to add.
     * @param hasArg flag signalling if an argument is required after this option.
     * @param description Self-documenting description.
     * @return Options return the added options
     */
    Options addOption(String name, boolean hasArg, String description);

    /**
     * Parse the driver specific command line arguments.
     * @param name Name of the parameter to add.
     * @param description Self-documenting description.
     * @return Options return the added options
     */
    Options addOption(String name, String description);

    /**
     * Returns whether the named Option is a member of this Parameters.
     * @param name name of the parameter option
     * @return  true if the named Option is a member of this Options
     */
    boolean hasOption(String name);

    /**
     * Retrieve the Option matching the parameter name specified.
     * @param name Name of the parameter.
     * @return  parameter value
     */
    String getOptionValue(String name);

    /**
     * Retrieve the Option matching the parameter name specified.
     * @param name Name of the parameter.
     * @param defaultValue default value if the parameter not found
     * @return   parameter value
     */
    String getOptionValue(String name, String defaultValue);


    /**
     * Get the -help output.
     */
    String getHelpText();

    /**
     * Print the -help output.
     */
    default void printHelp() {
        System.out.println("\n"+getHelpText());
    }


    /**
     * Parse the command line arguments.
     * @param args list of command line arguments.
     * @throws IllegalArgumentException If an exception occurred.
     * @throws ParseException If an exception occurred.
     */
    void parseArgs(String[] args) throws ParseException, IllegalArgumentException;
}
