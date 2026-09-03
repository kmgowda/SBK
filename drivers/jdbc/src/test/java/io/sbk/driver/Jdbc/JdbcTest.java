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

import io.sbk.params.InputParameterOptions;
import io.sbk.params.impl.SbkDriversParameters;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Verifies JDBC command-line configuration. */
public final class JdbcTest {

    /** Keep database creation and table recreation as independent options. */
    @Test
    public void parsesCreateDatabaseIndependentlyFromRecreate() throws Exception {
        final Jdbc jdbc = new Jdbc();
        final InputParameterOptions params = parameters(jdbc,
                "-createdb", "false", "-recreate", "true");

        jdbc.parseArgs(params);

        assertFalse(jdbc.config.createDb);
        assertTrue(jdbc.config.reCreate);
    }

    /** Reject boolean typos instead of silently interpreting them as false. */
    @Test
    public void rejectsInvalidBooleanOption() throws Exception {
        final Jdbc jdbc = new Jdbc();
        final InputParameterOptions params = parameters(jdbc, "-createdb", "yes");

        assertThrows(IllegalArgumentException.class, () -> jdbc.parseArgs(params));
    }

    private static InputParameterOptions parameters(Jdbc jdbc, String... driverArgs) throws Exception {
        final InputParameterOptions params = new SbkDriversParameters(
                "jdbc-test", new String[]{"Jdbc"}, new String[0]);
        jdbc.addArgs(params);
        final String[] args = new String[driverArgs.length + 6];
        args[0] = "-class";
        args[1] = "jdbc";
        args[2] = "-writers";
        args[3] = "1";
        args[4] = "-size";
        args[5] = "1";
        System.arraycopy(driverArgs, 0, args, 6, driverArgs.length);
        params.parseArgs(args);
        return params;
    }
}
