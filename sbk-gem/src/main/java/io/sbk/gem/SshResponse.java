/**
 * Copyright (c) KMG. All Rights Reserved..
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 */

package io.sbk.gem;


import java.io.ByteArrayOutputStream;
import java.io.OutputStream;

public final class SshResponse {
    public int returnCode;
    public final OutputStream errOutput;
    public final OutputStream stdOutput;


    public SshResponse(boolean stdout) {
        this.returnCode = 0;
        this.errOutput = new ByteArrayOutputStream();
        if (stdout) {
            this.stdOutput = new ByteArrayOutputStream();
        } else {
            this.stdOutput = OutputStream.nullOutputStream();
        }
    }

}