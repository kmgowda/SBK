/**
 * Copyright (c) KMG. All Rights Reserved..
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.sbk.PostgreSQL;

import io.sbk.Jdbc.Jdbc;

/**
 * Class for PostgreSQL.
 */
public class PostgreSQL extends Jdbc {
    private final static String CONFIGFILE = "postgresql.properties";

    @Override
    public String getConfigFile() {
        return CONFIGFILE;
    }
}

