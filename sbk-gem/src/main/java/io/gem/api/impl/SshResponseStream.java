/**
 * Copyright (c) KMG. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.gem.api.impl;


import java.io.ByteArrayOutputStream;
import java.io.OutputStream;

/**
 * Class SshResponseStream.
 */
public final class SshResponseStream {
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
     * This constructor is responsible for initializing all values.
     *
     * @param stdout boolean
     */
    public SshResponseStream(boolean stdout) {
        this.returnCode = 0;
        this.errOutputStream = new ByteArrayOutputStream();
        if (stdout) {
            this.stdOutputStream = new ByteArrayOutputStream();
        } else {
            this.stdOutputStream = OutputStream.nullOutputStream();
        }
    }

}