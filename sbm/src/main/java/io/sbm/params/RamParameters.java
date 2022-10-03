/**
 * Copyright (c) KMG. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.sbm.params;

import io.sbk.params.ActionParameter;

/**
 * Interface RamParameters.
 */
public sealed interface RamParameters extends ActionParameter permits RamParameterOptions {

    /**
     * Get Storage Name.
     *
     * @return Name of the storage
     */
    String getStorageName();

    /**
     * Get the Port number to user.
     *
     * @return port number.
     */
    int getPort();

    /**
     * get Max Connections.
     *
     * @return Maximum allowed connections.
     */
    int getMaxConnections();

}
