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

/**
 * Configuration for the SBK Local Web Console.
 */
public final class WebConsoleConfig {
    /** Address on which the web console server listens. */
    public String host;
    /** TCP port used by the web console server. */
    public int port;
    /** Whether SBK may start the web console server when it is unavailable. */
    public boolean start;
    /** Whether SBK should open the web console in the default browser. */
    public boolean open;
    /** Number of minutes of snapshots retained for each benchmark run. */
    public int minutes;
    /** Optional human-readable benchmark name. */
    public String name;

    /**
     * Creates an empty web console configuration for property binding.
     */
    public WebConsoleConfig() {
    }
}
