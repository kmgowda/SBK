/**
 * Copyright (c) KMG. All Rights Reserved..
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 */
package io.sbk.Exasol;

import io.sbk.Jdbc.Jdbc;
import io.sbk.api.DataWriter;
import io.sbk.params.ParameterOptions;

import java.io.IOException;

public class Exasol extends Jdbc {
    private final static String CONFIGFILE = "Exasol.properties";

    @Override
    public String getConfigFile() {
        return CONFIGFILE;
    }

    /*
     * By default, the Data column name in Jdbc is 'Data'. In Exasol 'Data' is a predefined keyword and can not be used as column name.
     * That's why we have updated the create table query and insert query
     * */
    @Override
    public String createTableQuery(ParameterOptions params) throws IllegalArgumentException {
        String query = "CREATE TABLE " + config.table +
                "(ID BIGINT IDENTITY not null primary key" +
                ", DATA_EXASOL VARCHAR(" + params.getRecordSize() + ") NOT NULL)";
        return query;
    }

    @Override
    public DataWriter<String> createWriter(final int id, final ParameterOptions params) {
        try {
            return new ExasolWriter(id, params, config, dType);
        } catch (IOException ex) {
            ex.printStackTrace();
            return null;
        }
    }
}
