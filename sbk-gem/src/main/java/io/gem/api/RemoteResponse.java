/**
 * Copyright (c) KMG. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.gem.api;

/**
 * Immutable result of executing a command on a remote host.
 *
 * <p>Captures the exit status, stdout/stderr text, and the host identifier so callers can
 * correlate results across multiple nodes.
 */
public final class RemoteResponse {

    /**
     * <code>int returnCode</code>.
     */
    public final int returnCode;

    /**
     * <code>String stdOutput</code>.
     */
    public final String stdOutput;

    /**
     * <code>String errOutput</code>.
     */
    public final String errOutput;

    /**
     * <code>String host</code>.
     */
    public final String host;


    /**
     * Create a remote response snapshot.
     *
     * @param returnCode exit status returned by the remote command (null may indicate unknown)
     * @param stdOutput  standard output captured as text (may be large)
     * @param errOutput  standard error captured as text
     * @param host       remote host identifier for this response
     */
    public RemoteResponse(int returnCode, String stdOutput, String errOutput, String host) {
        this.returnCode = returnCode;
        this.stdOutput = stdOutput;
        this.errOutput = errOutput;
        this.host = host;
    }

}
