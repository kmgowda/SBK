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

import io.sbk.api.DataType;
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
    final private DataType<String> dType;
    final private String  data;
    final private String  insertQuery;


    public JdbcWriter(int writerID, Parameters params,
                      String tableName, JdbcConfig config, DataType<String> dType) throws IOException {
        final Properties props = new Properties();
        this.tableName = tableName;
        this.dType = dType;
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
            conn.setAutoCommit(config.autoCommit);
            st = conn.createStatement();

        } catch (SQLException ex) {
            throw  new IOException(ex);
        }
        this.data = dType.create(params.getRecordSize());
        this.insertQuery = "INSERT INTO " + tableName + " (DATA) VALUES ('" + this.data + "')";
    }

    @Override
    public long recordWrite(String data, int size, RecordTime record, int id) throws IOException {
        final long beginTime = System.currentTimeMillis();
        try {
            st.executeUpdate(this.insertQuery);
        } catch (SQLException ex) {
            SbkLogger.log.error("JDBC: recordWrite failed !");
            throw  new IOException(ex);
        }
        final long endTime =  System.currentTimeMillis();
        record.accept(id, beginTime, endTime, size, 1);
        return endTime;
    }


    @Override
    public CompletableFuture writeAsync(String data) throws IOException {
        final String query = "INSERT INTO " + tableName + " (DATA) VALUES ('" + data + "')";
        try {
            st.executeUpdate(query);
        } catch (SQLException ex) {
            throw  new IOException(ex);
        }
        return null;
    }

    @Override
    public void flush() throws IOException {
        try {
            if (!conn.getAutoCommit()) {
                conn.commit();
            }
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