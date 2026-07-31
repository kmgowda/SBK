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
 * Terminal outcome of an SBK command submitted to one remote host.
 */
public enum RemoteExecutionStatus {
    /** The remote command completed with exit code zero. */
    SUCCESS,
    /** The remote command completed with a non-zero exit code. */
    EXIT_FAILURE,
    /** The SSH transport failed before a usable exit code was received. */
    SSH_ERROR,
    /** The remote command exceeded its execution deadline. */
    TIMEOUT,
    /** The command was cancelled because another node failed. */
    CANCELLED,
    /** No terminal result was obtained for the configured host. */
    NOT_COMPLETED
}
