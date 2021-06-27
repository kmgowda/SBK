/**
 * Copyright (c) KMG. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 */

package io.sbk.gem.impl;


import java.io.ByteArrayOutputStream;
import java.io.OutputStream;

public final class SshResponseStream {
    public int returnCode;
    public final OutputStream errOutputStream;
    public final OutputStream stdOutputStream;


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