/**
 * Copyright (c) KMG. All Rights Reserved..
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.sbk.driver.MySQL;

import io.sbk.driver.Jdbc.Jdbc;

/**
 * Class for MySQL.
 */
public class MySQL extends Jdbc {
    private final static String CONFIGFILE = "mysql.properties";

    @Override
    public String getConfigFile() {
        return CONFIGFILE;
    }
}

