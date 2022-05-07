/**
 * Copyright (c) KMG. All Rights Reserved..
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 */
package io.sbk.Db2;

import io.sbk.Jdbc.Jdbc;
import io.sbk.api.ParameterOptions;
import io.sbk.options.InputOptions;

/**
 * Class for Db2.
 */
public class Db2 extends Jdbc {
    private final static String CONFIGFILE = "db2.properties";

    @Override
    public void addArgs(final InputOptions params) throws IllegalArgumentException {
        super.addArgs(params, CONFIGFILE);

    }

    @Override
    public void parseArgs(final ParameterOptions params) throws IllegalArgumentException {
        super.parseArgs(params);
        if (params.getWritersCount() > 1) {
            throw new IllegalArgumentException("Error: Db2: Multiple Writers are not allowed");
        }
    }

    @Override
    public String getDriver() {
        return config.driver;
    }

    @Override
    public String createTableQuery(final ParameterOptions params) throws IllegalArgumentException {
        return "CREATE TABLE " + config.table +
                "(ID BIGINT" +
                ", DATA VARCHAR(" + params.getRecordSize() + "))";

    }
}
