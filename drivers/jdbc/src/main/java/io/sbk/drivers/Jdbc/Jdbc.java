/**
 * Copyright (c) KMG. All Rights Reserved..
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 */
package io.sbk.drivers.Jdbc;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.javaprop.JavaPropsFactory;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.sbk.api.DataReader;
import io.sbk.api.DataWriter;
import io.sbk.params.ParameterOptions;
import io.sbk.api.Storage;
import io.sbk.data.DataType;
import io.sbk.data.impl.SbkString;
import io.sbk.params.InputOptions;
import io.sbk.system.Printer;

import java.io.IOException;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Objects;
import java.util.Properties;

/**
 * Class for Jdbc.
 */
public class Jdbc implements Storage<String> {
    private final static String DERBY_NAME = "derby";
    private final static String POSTGRESQL_NAME = "postgresql";
    private final static String MSSQL_NAME = "sqlserver";
    private final static String SQLITE_NAME = "sqlite";
    private final static String H2_NAME = "h2";

    private final static String CONFIGFILE = "jdbc.properties";
    final public DataType<String> dType = new SbkString();
    public String driverType;
    public JdbcConfig config;

    /**
     * Get the JDBC config file.
     * The first invocation of the method is always in addArgs.
     *
     * @return JDBC driver name
     */
    public String getConfigFile() {
        return CONFIGFILE;
    }

    /**
     * Get the JDBC Driver String.
     * This method is always invoked after reading the configuration file.
     * The first invocation of the method is always in addArgs.
     *
     * @return JDBC driver name
     */
    public String getDriver() {
        return null;
    }


    /**
     * Query for Creating a Table.
     *
     * @param params Parameters object to be extended.
     * @return Query String.
     * @throws IllegalArgumentException If an exception occurred.
     */
    public String createTableQuery(final ParameterOptions params) throws IllegalArgumentException {
        String query;
        if (driverType.equalsIgnoreCase(DERBY_NAME)) {
            query = "CREATE TABLE " + config.table +
                    "(ID BIGINT GENERATED ALWAYS AS IDENTITY not null primary key" +
                    ", DATA VARCHAR(" + params.getRecordSize() + ") NOT NULL)";
        } else if (driverType.equalsIgnoreCase(POSTGRESQL_NAME)) {
            query = "CREATE TABLE " + config.table +
                    "(ID BIGSERIAL PRIMARY KEY" +
                    ", DATA VARCHAR(" + params.getRecordSize() + ") NOT NULL)";
        } else if (driverType.equalsIgnoreCase(MSSQL_NAME)) {
            query = "CREATE TABLE " + config.table +
                    "(ID BIGINT IDENTITY(1,1) PRIMARY KEY" +
                    ", DATA VARCHAR(" + params.getRecordSize() + ") NOT NULL)";
        } else if (driverType.equalsIgnoreCase(SQLITE_NAME)) {
            query = "CREATE TABLE " + config.table +
                    "(ID INTEGER PRIMARY KEY AUTOINCREMENT" +
                    ", DATA VARCHAR(" + params.getRecordSize() + ") NOT NULL)";
        }  else {
            // This statement works for MySQL, h2 and MariaDB too..
            query = "CREATE TABLE " + config.table +
                    "(ID BIGINT PRIMARY KEY AUTO_INCREMENT" +
                    ", DATA VARCHAR(" + params.getRecordSize() + ") NOT NULL)";
        }
        return query;
    }

    /**
     * Query for Deleting the Table.
     *
     * @param params Parameters object to be extended.
     * @return Query String.
     * @throws IllegalArgumentException If an exception occurred.
     */
    public String dropTableQuery(final ParameterOptions params) throws IllegalArgumentException {
        return "DROP TABLE " + config.table;
    }

    /**
     * Add the driver specific command line arguments.
     *
     * @param params     Parameters object to be extended.
     * @param jdbcConfig JDBC Configuration.
     * @throws IllegalArgumentException If an exception occurred.
     */
    public void addArgs(final InputOptions params, JdbcConfig jdbcConfig) throws IllegalArgumentException {
        final ObjectMapper mapper = new ObjectMapper(new JavaPropsFactory())
                .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        this.config = jdbcConfig;

        if (getDriver() == null) {
            params.addOption("driver", true, "Database driver, default Driver: " + config.driver);
        }

        params.addOption("table", true, "table name, default table: " + config.table);
        params.addOption("url", true, "Database url, default url: " + config.url);
        params.addOption("user", true, "User Name, default User name: " + config.user);
        params.addOption("password", true, "password, default password: " + config.password);
        params.addOption("createdb", true,
                "create the database for writers during JDBC connect, default: " + config.createDb);
        params.addOption("recreate", true,
                "If the table is already existing, delete and recreate the same, default: " + config.reCreate);
    }


    /**
     * Add the driver specific command line arguments.
     *
     * @param params     Parameters object to be extended.
     * @param configFile Configuration file to read.
     * @throws IllegalArgumentException If an exception occurred.
     */
    public void addArgs(final InputOptions params, String configFile) throws IllegalArgumentException {
        final ObjectMapper mapper = new ObjectMapper(new JavaPropsFactory())
                .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

        try {
            addArgs(params, mapper.readValue(Objects.requireNonNull(Jdbc.class.getClassLoader().getResourceAsStream(configFile)),
                    JdbcConfig.class));
        } catch (Exception ex) {
            ex.printStackTrace();
            throw new IllegalArgumentException(ex);
        }

    }


    @Override
    public void addArgs(final InputOptions params) throws IllegalArgumentException {
        addArgs(params, getConfigFile());
    }


    @Override
    public void parseArgs(final ParameterOptions params) throws IllegalArgumentException {
        if (params.getWritersCount() > 0 && params.getReadersCount() > 0) {
            throw new IllegalArgumentException("Error: JDBC: Specify either writers or readers, both are not allowed");
        }
        config.table = params.getOptionValue("table", config.table);
        if (params.hasOptionValue("driver")) {
            config.driver = params.getOptionValue("driver", config.driver);
        }
        config.url = params.getOptionValue("url", config.url);
        config.user = params.getOptionValue("user", config.user);
        config.password = params.getOptionValue("password", config.password);
        config.reCreate = Boolean.parseBoolean(params.getOptionValue("recreate", String.valueOf(config.reCreate)));
        config.createDb = Boolean.parseBoolean(params.getOptionValue("recreate", String.valueOf(config.createDb)));
    }


    @Override
    @SuppressFBWarnings("ODR_OPEN_DATABASE_RESOURCE")
    public void openStorage(final ParameterOptions params) throws IOException {
        try {
            Class.forName(config.driver);
        } catch (ClassNotFoundException ex) {
            Printer.log.error("The JDBC Driver: " + config.driver + " not found");
            throw new IOException(ex);
        }
        final Connection conn;
        final Statement st;
        final Properties props = new Properties();
        if (params.getWritersCount() > 0 && config.createDb) {
            props.put("create", "true");
        }
        if (config.user != null) {
            props.put("user", config.user);

        }
        if (config.password != null) {
            props.put("password", config.password);
        }
        try {
            Printer.log.info("JDBC Url: " + config.url);

            if (props.isEmpty()) {
                conn = DriverManager.getConnection(config.url);
            } else {
                conn = DriverManager.getConnection(config.url, props);
            }
            conn.setAutoCommit(config.autoCommit);
            driverType = getDriverType(config.url);
            final DatabaseMetaData dbmd = conn.getMetaData();

            Printer.log.info("JDBC Driver Type: " + driverType);
            Printer.log.info("JDBC Driver Name: " + dbmd.getDriverName());
            Printer.log.info("JDBC Driver Version: " + dbmd.getDriverVersion());
            st = conn.createStatement();
        } catch (SQLException ex) {
            throw new IOException(ex);
        }
        if (params.getWritersCount() > 0) {

            try {
                if (tableExist(conn, config.table)) {
                    Printer.log.info("The Table: " + config.table + " already exists");
                }
            } catch (SQLException ex) {
                Printer.log.error(ex.getMessage());
            }

            if (config.reCreate) {
                Printer.log.info("Deleting the Table: " + config.table);
                final String query = dropTableQuery(params);
                try {
                    st.execute(query);
                    if (!conn.getAutoCommit()) {
                        conn.commit();
                    }
                } catch (SQLException ex) {
                    Printer.log.info(ex.getMessage());
                    try {
                        conn.rollback();
                    } catch (SQLException e) {
                        Printer.log.error(e.getMessage());
                    }
                }
            }

            try {
                Printer.log.info("Creating the Table: " + config.table);
                st.execute(createTableQuery(params));
                if (!conn.getAutoCommit()) {
                    conn.commit();
                }
            } catch (SQLException ex) {
                Printer.log.info(ex.getMessage());
                try {
                    conn.rollback();
                } catch (SQLException e) {
                    Printer.log.error(e.getMessage());
                }
            }
            try {
                conn.close();
            } catch (SQLException ex) {
                throw new IOException(ex);
            }
        }
    }


    public String getDriverType(String url) {
        String[] st = url.split(":");
        return st[1];
    }

    public boolean tableExist(Connection conn, String tableName) throws SQLException {
        boolean tExists = false;
        ResultSet rs = conn.getMetaData().getTables(null, null, tableName, null);
        while (rs.next()) {
            String tName = rs.getString("TABLE_NAME");
            if (tName != null && tName.equalsIgnoreCase(tableName)) {
                tExists = true;
                break;
            }
        }
        return tExists;
    }


    @Override
    @SuppressFBWarnings("ODR_OPEN_DATABASE_RESOURCE")
    public void closeStorage(final ParameterOptions params) throws IOException {
        final Properties props = new Properties();
        props.put("shutdown", "true");
        if (config.user != null) {
            props.put("user", config.user);

        }
        if (config.password != null) {
            props.put("password", config.password);
        }
        try {
            DriverManager.getConnection(config.url, props);
        } catch (SQLException ex) {
            // throw new IOException(ex);
        }
    }

    @Override
    public DataWriter<String> createWriter(final int id, final ParameterOptions params) {
        try {
            return new JdbcWriter(id, params, config, dType);
        } catch (IOException ex) {
            ex.printStackTrace();
            return null;
        }
    }

    @Override
    public DataReader<String> createReader(final int id, final ParameterOptions params) {
        try {
            return new JdbcReader(id, params, config);
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
