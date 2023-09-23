/**
 * Copyright (c) KMG. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 */
package io.sbk.Hive;

import io.sbk.Jdbc.JdbcConfig;
import io.sbk.Jdbc.JdbcWriter;
import io.sbk.params.ParameterOptions;
import io.sbk.data.DataType;

import java.io.IOException;

/**
 * Class for Hive  Writer.
 */
public class HiveWriter extends JdbcWriter {
    private long id;

    public HiveWriter(int writerID, ParameterOptions params,
                      JdbcConfig config, DataType<String> dType) throws IOException {
        super(writerID, params, config, dType);
        this.id = 0;
    }

    @Override
    public String gerWriteQuery() {
        this.id += 1;
        return "INSERT INTO " + config.table + " VALUES (" + this.id + ",'" + this.data + "')";
    }

}