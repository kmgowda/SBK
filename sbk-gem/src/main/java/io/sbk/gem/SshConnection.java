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
public final class SshConnection {

    @Getter
    private final String host;

    @Getter
    private final String userName;

    @Getter
    private final String password;

    @Getter
    private final int port;

    public SshConnection(String host, String userName, String password, int port) {
        this.host = host;
        this.userName = userName;
        this.password = password;
        this.port = port;
    }

}