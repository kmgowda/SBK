/**
 * Copyright (c) KMG. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.sbk.drivers.RedPanda;

import io.sbk.drivers.Kafka.Kafka;
import io.sbk.params.InputOptions;

/**
 * Class for RedPanda Benchmarking.
 */
public class RedPanda extends Kafka {
    private final static String CONFIGFILE = "redpanda.properties";

    @Override
    public void addArgs(final InputOptions params) throws IllegalArgumentException {
        addArgs(params, CONFIGFILE);
    }

}



