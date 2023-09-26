/**
 * Copyright (c) KMG. All Rights Reserved..
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.sbk.drivers.MariaDB;

import io.sbk.drivers.MySQL.MySQL;

/**
 * Class for MySQL.
 */
public class MariaDB extends MySQL {
    private final static String CONFIGFILE = "mariadb.properties";

    @Override
    public String getConfigFile() {
        return CONFIGFILE;
    }
}
