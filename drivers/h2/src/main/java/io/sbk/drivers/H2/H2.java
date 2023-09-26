/**
 * Copyright (c) KMG. All Rights Reserved..
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.sbk.drivers.H2;

import io.sbk.drivers.Jdbc.Jdbc;

/**
 * Class for H2.
 */
public class H2 extends Jdbc {
    private final static String CONFIGFILE = "H2.properties";

    @Override
    public String getConfigFile() {
        return CONFIGFILE;
    }
}