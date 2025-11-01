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


import java.io.ByteArrayOutputStream;
import java.io.OutputStream;

/**
 * Mutable holder for SSH command execution streams and exit code.
 *
 * <p>Used by {@link SshUtils#runCommand(org.apache.sshd.client.session.ClientSession, String, long, SshResponse)}
 * to channel stdout/stderr and record the return code.
 */
public final class SshResponse {
    /**
     * <code>OutputStream errOutputStream</code>.
     */
    public final OutputStream errOutputStream;
    /**
     * <code>OutputStream stdOutputStream</code>.
     */
    public final OutputStream stdOutputStream;
    /**
     * <code>int returnCode</code>.
     */
    public int returnCode;


    /**
     * Create response streams.
     *
     * @param stdout if true, allocate a buffer for stdout; otherwise discard to a null stream
     */
    public SshResponse(boolean stdout) {
        this.returnCode = 0;
        this.errOutputStream = new ByteArrayOutputStream();
        if (stdout) {
            this.stdOutputStream = new ByteArrayOutputStream();
        } else {
            this.stdOutputStream = OutputStream.nullOutputStream();
        }
    }

}