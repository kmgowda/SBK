/**
 * Copyright (c) KMG. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 */

package io.sbk.gem;

public final class RemoteResponse {
    public final int returnCode;
    public final String stdOutput;
    public final String errOutput;
    public final String host;


    public RemoteResponse(int returnCode, String stdOutput, String errOutput, String host) {
        this.returnCode = returnCode;
        this.stdOutput = stdOutput;
        this.errOutput = errOutput;
        this.host = host;
    }

}
