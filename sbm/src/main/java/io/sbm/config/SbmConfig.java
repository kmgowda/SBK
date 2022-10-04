/**
 * Copyright (c) KMG. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.sbm.config;

import io.perl.config.LatencyConfig;

/**
 * Class RamConfig.
 */
final public class SbmConfig extends LatencyConfig {

    /**
     * <code>String NAME = "sbm"</code>.
     */
    public final static String NAME = "sbm";

    /**
     * <code>String DESC = "Storage Benchmark Monitorr"</code>.
     */
    final public static String DESC = "Storage Benchmark Monitor";


    /**
     * <code>int port</code>.
     */
    public int port;
    /**
     * <code>int maxConnections</code>.
     */
    public int maxConnections;
    /**
     * <code>int maxQueues</code>.
     */
    public int maxQueues;
    /**
     * <code>int idleMS</code>.
     */
    public int idleMS;
}
