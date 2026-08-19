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

/** Shell exit statuses interpreted by SBK-GEM remote deployment. */
public final class RemoteExitCode {
    /** POSIX shell status used when a command cannot be found. */
    public static final int COMMAND_NOT_FOUND = 127;

    private RemoteExitCode() {
    }
}
