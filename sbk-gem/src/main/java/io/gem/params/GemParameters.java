/**
 * Copyright (c) KMG. All Rights Reserved..
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.gem.params;

import io.gem.api.ConnectionConfig;
import io.sbk.params.Parameters;

/**
 * Interface GemParameters.
 */
public sealed interface GemParameters extends Parameters permits GemParameterOptions {

    /**
     * to get ssh connections.
     *
     * @return SshConnection.
     */
    ConnectionConfig[] getConnections();

    /**
     * to get Sbk directory.
     *
     * @return Sbk directory.
     */
    String getSbkDir();

    /**
     * to get sbk commands.
     *
     * @return Sbk commands.
     */
    String getSbkCommand();

    /**
     * to get local host.
     *
     * @return the local host.
     */
    String getLocalHost();

    /**
     * to get SBM port number.
     *
     * @return SBM port number.
     */
    int getSbmPort();


    /**
     * to get SBM idle milliseconds sleep.
     *
     * @return SBM idle milliseconds sleep.
     */
    int getSbmIdleSleepMilliSeconds();


    /**
     * checks if parameters are copy.
     *
     * @return true or false.
     */
    boolean isCopy();

    /**
     * checks if parameters are deleted.
     *
     * @return true ro false.
     */
    boolean isDelete();
}
