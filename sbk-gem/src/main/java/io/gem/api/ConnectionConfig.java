/**
 * Copyright (c) KMG. All Rights Reserved..
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.gem.api;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

/**
 * Immutable SSH connection configuration used by SBK-GEM.
 *
 * <p>Holds the remote host, credentials, SSH port, and a working directory where
 * SBK binaries and artifacts will be placed/executed on the remote node.
 */
@Slf4j
public final class ConnectionConfig {

    @Getter
    private final String host;

    @Getter
    private final String userName;

    @Getter
    private final String password;

    @Getter
    private final int port;

    @Getter
    private final String dir;

    /**
     * Create a connection configuration.
     *
     * @param host      remote host name or IP
     * @param userName  SSH user name
     * @param password  SSH password (may be empty if key auth is used upstream)
     * @param port      SSH port
     * @param dir       remote working directory (e.g., target SBK path)
     */
    public ConnectionConfig(String host, String userName, String password, int port, String dir) {
        this.host = host;
        this.userName = userName;
        this.password = password;
        this.port = port;
        this.dir = dir;
    }

}