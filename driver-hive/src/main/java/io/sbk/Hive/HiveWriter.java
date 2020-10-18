/**
 * Copyright (c) KMG. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 */
package io.sbk.Hive;

import io.sbk.Jdbc.JdbcConfig;
import io.sbk.Jdbc.JdbcWriter;
import io.sbk.api.DataType;
import io.sbk.api.Parameters;

import java.io.IOException;

/**
 * Class for Hive  Writer.
 */
public class HiveWriter extends JdbcWriter {
    private long id;

    public HiveWriter(int writerID, Parameters params,
                      String tableName, JdbcConfig config, DataType<String> dType) throws IOException {
        super(writerID, params, tableName, config, dType);
        this.id = 0;
    }

    public String insertTable() {
        this.id += 1;
        return "INSERT INTO " + tableName + " VALUES ("+  this.id +",'" + this.data + "')";
    }

}