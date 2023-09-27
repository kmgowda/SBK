/**
 * Copyright (c) KMG. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 */
package io.sbk.driver.Jdbc;

import io.sbk.params.ParameterOptions;
import io.sbk.api.Reader;
import io.sbk.system.Printer;

import java.io.EOFException;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Properties;

/**
 * Class for JDBC reader.
 */
public class JdbcReader implements Reader<String> {
    final public JdbcConfig config;
    final private Connection conn;
    final private Statement st;
    final private String readQuery;
    private ResultSet res;

    public JdbcReader(int id, ParameterOptions params, JdbcConfig config) throws IOException {
        this.config = config;
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
            throw new IOException(ex);
        }
        readQuery = "SELECT * from " + config.table;
        res = null;
    }

    public String getReadQuery() {
        return readQuery;
    }

    @Override
    public String read() throws EOFException {
        if (res == null) {
            try {
                res = st.executeQuery(getReadQuery());
            } catch (SQLException ex) {
                Printer.log.error("JDBC:JdbcReader " + getReadQuery() + " failed");
                ex.printStackTrace();
                res = null;
            }
        } else {
            try {
                if (res.next()) {
                    return res.getString(2);
                } else {
                    throw new EOFException("JDBC : file red EOF");
                }
            } catch (SQLException ex) {
                Printer.log.error("JDBC:JdbcReader result next failed");
                ex.printStackTrace();
            }
        }
        return null;
    }

    @Override
    public void close() {
        try {
            conn.close();
        } catch (SQLException ex) {
            Printer.log.error("JDBC:JdbcReader close failed");
            ex.printStackTrace();
        }
    }
}