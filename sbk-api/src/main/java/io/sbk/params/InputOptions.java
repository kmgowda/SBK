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

public sealed interface InputOptions permits ParseInputOptions {
    /**
     * Add the driver specific command line arguments.
     *
     * @param name        Name of the parameter to add.
     * @param hasArg      flag signalling if an argument is required after this option.
     * @param description Self-documenting description.
     * @throws IllegalArgumentException if 'name' already added.
     */
    void addOption(String name, boolean hasArg, String description) throws IllegalArgumentException;

    /**
     * Returns whether the named Option exists.
     *
     * @param name name of the parameter option
     * @return true if the named Option is a member of this Options
     */
    boolean hasOption(String name);

}
