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

import io.sbk.api.impl.SbkYal;
import io.sbk.exception.HelpException;
import org.apache.commons.cli.ParseException;
import org.apache.commons.cli.UnrecognizedOptionException;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;

/**
 * Main class of SBK-YAL.
 */
public final class SbkYalMain {

    public static void main(final String[] args) {
        try {
            SbkYal.run(args, null, null, null);
        } catch (UnrecognizedOptionException | HelpException ex) {
            System.exit(2);
        } catch (ParseException | IllegalArgumentException | IOException | TimeoutException | InterruptedException |
                ExecutionException | ClassNotFoundException |  InvocationTargetException | InstantiationException |
                NoSuchMethodException | IllegalAccessException ex) {
            ex.printStackTrace();
            System.exit(1);
        }
        System.exit(0);
    }
}
