/**
 * Copyright (c) KMG. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 */
package io.sbk.dashboard;

/**
 * Configuration for the local SBK browser dashboard.
 */
public final class DashboardConfig {
    /** Address on which the dashboard server listens. */
    public String host;
    /** TCP port used by the dashboard server. */
    public int port;
    /** Whether SBK may start the dashboard server when it is unavailable. */
    public boolean start;
    /** Whether SBK should open the dashboard in the default browser. */
    public boolean open;
    /** Maximum number of snapshots retained for each benchmark run. */
    public int retention;
    /** Optional human-readable benchmark name. */
    public String name;

    /**
     * Creates an empty dashboard configuration for property binding.
     */
    public DashboardConfig() {
    }
}
