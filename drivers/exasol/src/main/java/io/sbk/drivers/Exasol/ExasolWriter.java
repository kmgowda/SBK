/**
 * Copyright (c) KMG. All Rights Reserved.
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 */
package io.sbk.drivers.Exasol;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.sbk.drivers.Jdbc.JdbcConfig;
import io.sbk.drivers.Jdbc.JdbcWriter;
import io.sbk.data.DataType;
import io.sbk.params.ParameterOptions;

import java.io.IOException;
/**
 * Class for Exasol  Writer.
 */
public class ExasolWriter extends JdbcWriter {

    @SuppressFBWarnings("EI_EXPOSE_REP2")
    public ExasolWriter(int writerID, ParameterOptions params,
                        JdbcConfig config, DataType<String> dType) throws IOException {
        super(writerID, params, config, dType);
    }

    /*
    * By default, the Data column name in Jdbc is 'Data'. In Exasol 'Data' is a predefined keyword and can not be used as column name.
    * That's why we have updated the create table query and insert query
    * */

    public String gerWriteQuery() {
        return "INSERT INTO " + this.config.table + " (DATA_EXASOL) VALUES ('" + this.data + "')";
    }
}