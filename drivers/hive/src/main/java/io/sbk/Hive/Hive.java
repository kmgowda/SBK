/**
 * Copyright (c) KMG. All Rights Reserved..
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 */
package io.sbk.Hive;

import io.sbk.Jdbc.Jdbc;
import io.sbk.api.DataWriter;
import io.sbk.params.ParameterOptions;
import io.sbk.params.InputOptions;

import java.io.IOException;

/**
 * Class for Hive.
 */
public class Hive extends Jdbc {
    private final static String CONFIGFILE = "hive.properties";

    @Override
    public void addArgs(final InputOptions params) throws IllegalArgumentException {
        super.addArgs(params, CONFIGFILE);

    }

    @Override
    public void parseArgs(final ParameterOptions params) throws IllegalArgumentException {
        super.parseArgs(params);
        if (params.getWritersCount() > 1) {
            throw new IllegalArgumentException("Error: Hive: Multiple Writers are not allowed");
        }
    }

    @Override
    public DataWriter<String> createWriter(final int id, final ParameterOptions params) {
        try {
            return new HiveWriter(id, params, config, dType);
        } catch (IOException ex) {
            ex.printStackTrace();
            return null;
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

    @Override
    public String dropTableQuery(final ParameterOptions parameters) throws IllegalArgumentException {
        return "DROP TABLE " + config.table;
    }
}
