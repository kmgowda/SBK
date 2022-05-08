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

public interface ParsedOptions {

    /**
     * Returns whether the named Option has the value after parsing.
     *
     * @param name name of the parameter option
     * @return true if the named Option is a member of this Options
     */
    boolean hasOptionValue(String name);

    /**
     * Retrieve the Option matching the parameter name specified.
     *
     * @param name Name of the parameter.
     * @return parameter value
     */
    String getOptionValue(String name);

    /**
     * Retrieve the Option matching the parameter name specified.
     *
     * @param name         Name of the parameter.
     * @param defaultValue default value if the parameter not found
     * @return parameter value
     */
    String getOptionValue(String name, String defaultValue);

    /**
     * Get the -help output.
     *
     * @return formatted Help text
     */
    String getHelpText();

    /**
     * Print the -help output.
     */
    default void printHelp() {
        System.out.println("\n" + getHelpText());
    }

}
