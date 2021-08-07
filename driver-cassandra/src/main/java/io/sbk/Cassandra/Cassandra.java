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
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.javaprop.JavaPropsFactory;
import io.sbk.api.DataReader;
import io.sbk.data.DataType;
import io.sbk.api.DataWriter;
import io.sbk.api.ParameterOptions;
import io.sbk.api.Storage;
import io.sbk.data.impl.SbkString;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.Objects;

public class Cassandra implements Storage<String>  {
    private final static String CONFIGFILE = "cassandra.properties";
    final public DataType<String> dType =  new SbkString();
    private CassandraConfig config;
    private CqlSession session;

    @Override
    public void addArgs(final ParameterOptions params) throws IllegalArgumentException {
        final ObjectMapper mapper = new ObjectMapper(new JavaPropsFactory())
                .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        try {
            config = mapper.readValue(
                    Objects.requireNonNull(Cassandra.class.getClassLoader().getResourceAsStream(CONFIGFILE)),
                    CassandraConfig.class);
        } catch (Exception ex) {
            ex.printStackTrace();
            throw new IllegalArgumentException(ex);
        }
        params.addOption("node", true, "Node IP Address, default: "+config.node);
        params.addOption("port", true, "Node port number, default : "+config.port);
        params.addOption("keyspace", true, "Key space name Name, default: "+config.keyspace);
        params.addOption("table", true, "Table Name, default: "+config.table);
        params.addOption("rs", true, "Replication strategy , default: "+config.replicationStrategy);
        params.addOption("rf", true, "Replication factor, default: "+config.replicationFactor);
        params.addOption("recreate", true, "Recreate Table, default: "+config.reCreate);
    }

    @Override
    public void parseArgs(final ParameterOptions params) throws IllegalArgumentException {
        config.node =  params.getOptionValue("node", config.node);
        config.port =  Integer.parseInt(params.getOptionValue("port", String.valueOf(config.port)));
        config.keyspace =  params.getOptionValue("keyspace", config.keyspace);
        config.table = params.getOptionValue("table", config.table);
        config.replicationStrategy = params.getOptionValue("rs", config.replicationStrategy);
        config.replicationFactor = params.getOptionValue("rf", config.replicationFactor);
        config.reCreate = Boolean.parseBoolean(params.getOptionValue("recreate", String.valueOf(config.reCreate)));

    }

    @Override
    public void openStorage(final ParameterOptions params) throws IOException {
        session = CqlSession.builder().addContactPoint(new InetSocketAddress(config.node, config.port))
                        .withLocalDatacenter("datacenter1").build();
        if (config.reCreate && params.getWritersCount() > 0) {
                session.execute("DROP TABLE IF EXISTS " + config.keyspace + "." + config.table);
                session.execute("DROP KEYSPACE IF EXISTS " + config.keyspace);
        }
        session.execute("CREATE KEYSPACE IF NOT EXISTS "+config.keyspace+" WITH replication = {'class': " +
                " '" + config.replicationStrategy+ "', 'replication_factor': '" + config.replicationFactor+"'}");
        session.execute("CREATE TABLE IF NOT EXISTS " + config.keyspace+ "." + config.table +
                " (id uuid PRIMARY KEY, data text)" );
    }

    @Override
    public void closeStorage(final ParameterOptions params) throws IOException {
        session.close();
    }

    @Override
    public DataWriter<String> createWriter(final int id, final ParameterOptions params) {
        try {
            return new CassandraWriter(id, params, config, session);
        } catch (IOException ex) {
            ex.printStackTrace();
            return null;
        }
    }

    @Override
    public DataReader<String> createReader(final int id, final ParameterOptions params) {
        try {
            return new CassandraReader(id, params, config, session);
        } catch (IOException ex) {
            ex.printStackTrace();
            return null;
        }
    }

    @Override
    public DataType<String> getDataType() {
        return this.dType;
    }
}
