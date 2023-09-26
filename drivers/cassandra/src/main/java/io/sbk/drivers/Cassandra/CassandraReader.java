/**
 * Copyright (c) KMG. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 */
package io.sbk.drivers.Cassandra;

import com.datastax.oss.driver.api.core.CqlSession;
import com.datastax.oss.driver.api.core.cql.ResultSet;
import com.datastax.oss.driver.api.core.cql.Row;
import io.sbk.params.ParameterOptions;
import io.sbk.api.Reader;

import java.io.EOFException;
import java.io.IOException;
import java.util.Iterator;


public class CassandraReader implements Reader<String> {
    final private CassandraConfig config;
    final private CqlSession session;
    private Iterator<Row> resIterator;

    public CassandraReader(int id, ParameterOptions params, CassandraConfig config, CqlSession session) throws IOException {
        this.config = config;
        this.session = session;
        this.resIterator = null;
    }

    @Override
    public String read() throws EOFException {
        if (resIterator == null) {
            StringBuilder sb = new StringBuilder("SELECT * FROM ").append(config.keyspace)
                    .append(".").append(config.table);
            String query = sb.toString();
            ResultSet rs = session.execute(query);
            resIterator = rs.iterator();
        }
        if (resIterator.hasNext()) {
            return resIterator.next().getString("data");
        }
        throw new EOFException();
    }

    @Override
    public void close() {
    }
}
