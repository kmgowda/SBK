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

import io.sbk.api.impl.Sbk;
import org.apache.commons.cli.ParseException;

import java.io.IOException;
import java.util.concurrent.ExecutionException;

/**
 * Main class of SBK.
 */
public class SbkMain {

    public static void main(final String[] args) {
        try {
            Sbk.run(args);
        } catch (ParseException | IllegalArgumentException | IOException |
                InterruptedException | ExecutionException ex) {
            ex.printStackTrace();
            System.exit(1);
        }
        System.exit(0);
    }
}
