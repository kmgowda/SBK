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

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public final class SshResponse {

    @Getter
    private final String stdOutput;

    @Getter
    private final String errOutput;

    @Getter
    private final int returnCode;

    SshResponse(String stdOutput, String errOutput, int returnCode) {
        this.stdOutput = stdOutput;
        this.errOutput = errOutput;
        this.returnCode = returnCode;
    }

}