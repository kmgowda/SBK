/**
 * Copyright (c) KMG. All Rights Reserved..
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.sbk.params;

import io.sbk.gem.SshConnection;

/**
 * Interface GemParameters.
 */
public sealed interface GemParameters extends Parameters permits GemParameterOptions {

    /**
     * to get ssh connections.
     *
     * @return SshConnection.
     */
    SshConnection[] getConnections();

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
     * to get ram port number.
     *
     * @return ram port number.
     */
    int getSbmPort();

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
