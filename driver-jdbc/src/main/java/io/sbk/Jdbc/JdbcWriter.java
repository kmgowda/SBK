/**
 * Copyright (c) KMG. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 */
package io.sbk.Jdbc;

import io.sbk.api.Parameters;
import io.sbk.api.RecordTime;
import io.sbk.api.Writer;
import io.sbk.api.impl.SbkLogger;

import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Properties;
import java.util.concurrent.CompletableFuture;

/**
 * Class for JDBC  Writer.
 */
public class JdbcWriter implements Writer<String> {
    final private String tableName;
    final private Connection conn;
    final private Statement st;


    public JdbcWriter(int writerID, Parameters params,
                         String tableName, JdbcConfig config) throws IOException {
        this.tableName = tableName;
        final Properties props = new Properties();
        if (config.user != null) {
            props.put("user", config.user);

        }
        if (config.password != null) {
            props.put("password", config.password);
        }
        try {
            if (props.isEmpty()) {
                conn = DriverManager.getConnection(config.url);
            } else {
                conn = DriverManager.getConnection(config.url, props);
            }
            st = conn.createStatement();

        } catch (SQLException ex) {
            throw  new IOException(ex);
        }
    }

    @Override
    public long recordWrite(String data, int size, RecordTime record, int id) {
        final long time = System.currentTimeMillis();
        final String query = "INSERT INTO " + tableName + " (DATA) VALUES ('" + data + "')";
        try {
            SbkLogger.log.info("JDBC Write data: "+data);
            st.executeUpdate(query);
        } catch (SQLException ex) {
            SbkLogger.log.error("JDBC: recordWrite failed !");
            ex.printStackTrace();
        }
        record.accept(id, time, System.currentTimeMillis(), size, 1);
        return time;
    }


    @Override
    public CompletableFuture writeAsync(String data) throws IOException {
        final String query = "INSERT INTO " + tableName + " (DATA) VALUES ('" + data + "')";
        try {
            SbkLogger.log.info("JDBC Write data: "+data);
            st.executeUpdate(query);
            flush();
        } catch (SQLException ex) {
            throw  new IOException(ex);
        }
        return null;
    }

    @Override
    public void flush() throws IOException {
        try {
            conn.commit();
        } catch (SQLException ex) {
            throw  new IOException(ex);
        }
    }

    @Override
    public void close() throws IOException {
        try {
            conn.close();
        } catch (SQLException ex) {
            throw new IOException(ex);
        }
    }
}