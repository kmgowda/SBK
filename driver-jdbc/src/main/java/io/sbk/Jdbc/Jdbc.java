/**
 * Copyright (c) KMG. All Rights Reserved..
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 */
package io.sbk.Jdbc;

import io.sbk.api.DataType;
import io.sbk.api.Storage;
import io.sbk.api.Parameters;
import io.sbk.api.Writer;
import io.sbk.api.Reader;
import io.sbk.api.impl.SbkLogger;
import io.sbk.api.impl.StringHandler;

import java.io.IOException;
import java.sql.Connection;
import java.sql.Driver;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Properties;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.javaprop.JavaPropsFactory;

/**
 * Class for Jdbc.
 */
public class Jdbc implements Storage<String> {
    private final static String CONFIGFILE = "jdbc.properties";
    private String tableName;
    private JdbcConfig config;

    @Override
    public void addArgs(final Parameters params) throws IllegalArgumentException {
        final ObjectMapper mapper = new ObjectMapper(new JavaPropsFactory())
                .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

        try {
            config = mapper.readValue(Jdbc.class.getClassLoader().getResourceAsStream(CONFIGFILE),
                    JdbcConfig.class);
        } catch (Exception ex) {
            ex.printStackTrace();
            throw new IllegalArgumentException(ex);
        }

        params.addOption("table", true, "table name");
        params.addOption("driver", true, "Database driver, default Driver: "+config.driver);
        params.addOption("url", true, "Database url, default url: "+config.url);
        params.addOption("user", true, "User Name, default User name: "+config.user);
        params.addOption("password", true, "password, default password: "+config.password);
        params.addOption("recreate", true,
                "If the table is already existing, delete and recreate the same, default: "+config.reCreate);

    }

    @Override
    public void parseArgs(final Parameters params) throws IllegalArgumentException {
        tableName =  params.getOptionValue("table", null);
        if (tableName == null) {
            throw new IllegalArgumentException("Error: Must specify Table Name");
        }
        tableName = tableName.toUpperCase();
        config.driver = params.getOptionValue("driver", config.driver);
        config.url = params.getOptionValue("url", config.url);
        config.user = params.getOptionValue("user", config.user);
        config.password = params.getOptionValue("password", config.password);
        config.reCreate = Boolean.parseBoolean(params.getOptionValue("recreate", String.valueOf(config.reCreate)));
    }

    @Override
    public void openStorage(final Parameters params) throws  IOException {
        try {
            Class.forName(config.driver);
        } catch (ClassNotFoundException ex) {
            SbkLogger.log.error("The JDBC Driver: "+ config.driver+" not found");
            throw  new IOException(ex);
        }
        final Properties props = new Properties();
        if (params.getWritersCount() > 0) {
            props.put("create", "true");
        }
        if (config.user != null) {
            props.put("user", config.user);

        }
        if (config.password != null) {
            props.put("password", config.password);
        }
        try {
            final Connection conn;
            if (props.isEmpty()) {
                conn = DriverManager.getConnection(config.url);
            } else {
                conn = DriverManager.getConnection(config.url, props);
            }
            Statement st = conn.createStatement();
            if (params.getWritersCount() > 0) {
                if (config.reCreate) {
                    final String query = "DROP TABLE " + tableName;
                    st.execute(query);
                    conn.commit();
                }
                if (!tableExist(conn, tableName)) {
                    final String query = "CREATE TABLE " + tableName +
                            "(ID BIGINT GENERATED ALWAYS AS IDENTITY not null primary key" +
                            ", DATA VARCHAR(" + params.getRecordSize() + ") NOT NULL)";
                    SbkLogger.log.info("query :" + query);
                    st.execute(query);
                    conn.commit();
                }
                conn.close();
            }
        } catch (SQLException ex) {
            throw  new IOException(ex);
        }
    }

    private boolean tableExist(Connection conn, String tableName) throws SQLException {
        boolean tExists = false;
        try (ResultSet rs = conn.getMetaData().getTables(null, null, tableName, null)) {
            while (rs.next()) {
                String tName = rs.getString("TABLE_NAME");
                if (tName != null && tName.equals(tableName)) {
                    tExists = true;
                    break;
                }
            }
        }
        return tExists;
    }


    @Override
    public void closeStorage(final Parameters params) throws IOException {

    }

    @Override
    public Writer createWriter(final int id, final Parameters params) {
        try {
           return new JdbcWriter(id, params, tableName, config);
        } catch (IOException ex) {
            ex.printStackTrace();
            return null;
        }
    }

    @Override
    public Reader createReader(final int id, final Parameters params) {
        try {
            return  new JdbcReader(id, params, tableName, config);
        } catch (IOException ex) {
            ex.printStackTrace();
            return null;
        }
    }

    @Override
    public DataType getDataType() {
        return new StringHandler();
    }
}
