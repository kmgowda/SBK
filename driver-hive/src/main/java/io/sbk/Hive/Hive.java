/**
 * Copyright (c) KMG. All Rights Reserved..
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 */
package io.sbk.Hive;

import io.sbk.Jdbc.Jdbc;
import io.sbk.api.Parameters;
import io.sbk.api.Writer;

import java.io.IOException;

/**
 * Class for Hive.
 */
public class Hive extends Jdbc {
    private final static String CONFIGFILE = "hive.properties";

    @Override
    public void addArgs(final Parameters params) throws IllegalArgumentException {
        super.addArgs(params, CONFIGFILE);

    }

    @Override
    public void parseArgs(final Parameters params) throws IllegalArgumentException {
        super.parseArgs(params);
        if (params.getWritersCount() > 1) {
            throw new IllegalArgumentException("Error: Hive: Multiple Writers are not allowed");
        }
    }

    @Override
    public Writer<String> createWriter(final int id, final Parameters params) {
        try {
            return new HiveWriter(id, params, tableName, config, dType);
        } catch (IOException ex) {
            ex.printStackTrace();
            return null;
        }
    }

    @Override
    public String createTable(final Parameters params) {
        return  "CREATE TABLE " + tableName +
                "(ID BIGINT" +
                ", DATA VARCHAR(" + params.getRecordSize() + "))";

    }

    @Override
    public String dropTable(final Parameters parameters) {
        return "DROP TABLE " + tableName;
    }
}
