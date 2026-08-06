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

import java.io.IOException;

/**
 * Host-tagged SSH command failure retaining bounded command diagnostics.
 */
public final class SshCommandException extends IOException {
    /** Remote host on which command execution failed. */
    private final String host;
    /** Bounded command output retained before the failure. */
    private final SshResponse response;
    /** Whether command execution exceeded its deadline. */
    private final boolean timeout;

    /**
     * Create a remote command failure.
     *
     * @param host remote host
     * @param response partial bounded stdout and stderr
     * @param timeout whether command execution timed out
     * @param cause underlying SSH failure
     */
    public SshCommandException(String host, SshResponse response, boolean timeout, IOException cause) {
        super("SBK-GEM: Remote SBK command " + (timeout ? "timed out" : "failed") +
                " on host '" + host + "': " + cause.getMessage(), cause);
        this.host = host;
        this.response = response;
        this.timeout = timeout;
    }

    /**
     * Return the remote host.
     *
     * @return remote host name or address
     */
    public String getHost() {
        return host;
    }

    /**
     * Return partial bounded command output.
     *
     * @return command response populated before transport failure
     */
    public SshResponse getResponse() {
        return response;
    }

    /**
     * Check whether the command deadline expired.
     *
     * @return true for a command timeout
     */
    public boolean isTimeout() {
        return timeout;
    }
}
