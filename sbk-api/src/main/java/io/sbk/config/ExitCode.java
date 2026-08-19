/**
 * Copyright (c) KMG. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 */
package io.sbk.config;

/** Process exit statuses shared by SBK suite launchers. */
public final class ExitCode {
    /** Successful execution. */
    public static final int SUCCESS = 0;
    /** Runtime or operational failure. */
    public static final int FAILURE = 1;
    /** Invalid command-line arguments. */
    public static final int INVALID_ARGUMENT = 2;

    private ExitCode() {
    }
}
