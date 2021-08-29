/**
 * Copyright (c) KMG. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.sbk.main;

import io.sbk.ram.impl.SbkRam;
import org.apache.commons.cli.ParseException;
import org.apache.commons.cli.UnrecognizedOptionException;

import java.io.IOException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;

/**
 * Main class of SBK Server.
 */
public class SbkRamMain {

    public static void main(final String[] args) {
        try {
            SbkRam.run(args, null, null);
        } catch (UnrecognizedOptionException ex) {
            System.exit(2);
        } catch (ParseException | IllegalArgumentException | IOException |
                TimeoutException | InterruptedException | ExecutionException | InstantiationException ex) {
            ex.printStackTrace();
            System.exit(1);
        }
        System.exit(0);
    }
}
