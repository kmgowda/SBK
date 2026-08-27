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

import java.nio.file.Path;
import java.util.List;

/**
 * Immutable Apache SSHD client configuration shared by compatible remote sessions.
 *
 * @param acceptAllHostKeys whether server host keys are accepted without verification
 * @param passwordPreferred whether password authentication is attempted before public-key authentication
 * @param knownHosts verified known-hosts file, or {@code null} when verification is disabled
 * @param preferredHostKeyTypes known host-key types promoted for matching hosts
 */
record SshClientPolicy(boolean acceptAllHostKeys, boolean passwordPreferred, Path knownHosts,
                       List<String> preferredHostKeyTypes) {

    SshClientPolicy {
        preferredHostKeyTypes = List.copyOf(preferredHostKeyTypes);
    }
}
