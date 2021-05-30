/**
 * Copyright (c) KMG. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 */

package io.sbk.main;

import io.sbk.api.impl.SbkServer;
import org.apache.commons.cli.ParseException;

import java.io.IOException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;

/**
 * Main class of SBK Server.
 */
public class SbkServerMain {

    public static void main(final String[] args) {
        try {
            SbkServer.run(args, null, null);
        } catch (ParseException | IllegalArgumentException | IOException |
                TimeoutException | InterruptedException | ExecutionException ex) {
            ex.printStackTrace();
            System.exit(1);
        }
        System.exit(0);
    }
}
