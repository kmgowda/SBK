/**
 * Copyright (c) KMG. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 */
package io.sbk.webconsole;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

/**
 * Configuration for the SBK Local Web Console.
 */
public final class WebConsoleConfig {
    /** TCP port used by the web console server. */
    public int port;
    /** Whether SBK should open the web console in the default browser. */
    public boolean open;
    /** Number of minutes of snapshots retained for each benchmark run. */
    public int snapshotMinutes;
    /** Number of idle minutes before the web console exits. */
    public int timeoutMinutes = Math.toIntExact(WebConsoleServer.DEFAULT_IDLE_TIMEOUT.toMinutes());
    /** Optional display name for the benchmark board. */
    @SuppressFBWarnings(value = "UUF_UNUSED_PUBLIC_OR_PROTECTED_FIELD",
            justification = "The sbk-api WebConsoleLoggerSupport adapter reads and writes this public configuration")
    public String name;

    /**
     * Creates an empty web console configuration for property binding.
     */
    public WebConsoleConfig() {
    }
}
