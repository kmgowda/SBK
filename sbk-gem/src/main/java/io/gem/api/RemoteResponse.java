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
    /** Sentinel used when SSH did not provide a remote process exit code. */
    public static final int UNKNOWN_RETURN_CODE = -1;

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

    /** Terminal remote execution classification. */
    public final RemoteExecutionStatus status;

    /** Host-tagged failure description, empty for successful commands. */
    public final String failureMessage;

    /**
     * Create a remote response snapshot.
     *
     * @param returnCode exit status returned by the remote command
     * @param stdOutput  captured standard output
     * @param errOutput  captured standard error
     * @param host       remote host identifier for this response
     */
    public RemoteResponse(int returnCode, String stdOutput, String errOutput, String host) {
        this(returnCode, stdOutput, errOutput, host,
                returnCode == 0 ? RemoteExecutionStatus.SUCCESS : RemoteExecutionStatus.EXIT_FAILURE,
                returnCode == 0 ? "" : "Remote process returned exit code " + returnCode);
    }

    /**
     * Create a fully classified remote response snapshot.
     *
     * @param returnCode exit status, or {@link #UNKNOWN_RETURN_CODE} when unavailable
     * @param stdOutput bounded standard-output tail
     * @param errOutput bounded standard-error tail
     * @param host remote host identifier
     * @param status terminal execution status
     * @param failureMessage host-tagged failure detail
     */
    public RemoteResponse(int returnCode, String stdOutput, String errOutput, String host,
                          RemoteExecutionStatus status, String failureMessage) {
        this.returnCode = returnCode;
        this.stdOutput = stdOutput;
        this.errOutput = errOutput;
        this.host = host;
        this.status = status;
        this.failureMessage = failureMessage;
    }

}
