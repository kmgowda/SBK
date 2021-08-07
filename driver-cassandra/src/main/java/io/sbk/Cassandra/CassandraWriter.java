/**
 * Copyright (c) KMG. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 */
package io.sbk.Cassandra;

import com.datastax.oss.driver.api.core.CqlSession;
import io.sbk.parameters.ParameterOptions;
import io.sbk.api.Writer;
import java.io.IOException;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public class CassandraWriter implements Writer<String> {
    final public CassandraConfig config;
    final public CqlSession session;

    public CassandraWriter(int writerID, ParameterOptions params,
                      CassandraConfig config, CqlSession session) throws IOException {
        this.config = config;
        this.session = session;
    }


    @Override
    public CompletableFuture writeAsync(String data) throws IOException {
        StringBuilder sb = new StringBuilder("INSERT INTO ")
                .append(config.keyspace).append(".").append(config.table).append(" (id, data) ")
                .append("VALUES (").append(UUID.randomUUID() )
                .append(", '").append(data).append("');");
        String query = sb.toString();
        session.execute(query);
        return null;
    }

    @Override
    public void sync() throws IOException {

    }

    @Override
    public void close() throws IOException {

    }
}