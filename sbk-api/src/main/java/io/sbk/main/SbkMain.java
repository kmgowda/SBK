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

import io.sbk.api.impl.Sbk;
import io.sbk.config.ExitCode;
import io.sbk.config.Config;
import io.sbk.webconsole.WebConsoleClient.WebConsoleBusyException;
import io.sbk.utils.SbkUtils;
import org.apache.commons.cli.ParseException;
import org.apache.commons.cli.UnrecognizedOptionException;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;

/**
 * Main class of SBK.
 */
public abstract class SbkMain {

    /**
     * Creates an SBK command-line entry point.
     */
    public SbkMain() {
    }

    static void main(final String[] args) {
        if (SbkUtils.hasVersion(args)) {
            final String version = io.sbk.api.impl.Sbk.class.getPackage().getImplementationVersion();
            System.out.println(Config.NAME.toUpperCase() + " Version: " + version);
            System.exit(ExitCode.SUCCESS);
        }
        try {
            Sbk.run(args, null, null, null);
        } catch (WebConsoleBusyException ex) {
            System.exit(ExitCode.FAILURE);
        } catch (UnrecognizedOptionException ex) {
            System.exit(ExitCode.INVALID_ARGUMENT);
        } catch (ParseException | IllegalArgumentException ex) {
            System.err.println(ex.getMessage() == null ? ex.getClass().getSimpleName() : ex.getMessage());
            System.exit(ExitCode.INVALID_ARGUMENT);
        } catch (IOException | TimeoutException | InterruptedException |
                ExecutionException | InstantiationException | ClassNotFoundException | InvocationTargetException |
                NoSuchMethodException | IllegalAccessException ex) {
            ex.printStackTrace();
            System.exit(ExitCode.FAILURE);
        }
        System.exit(ExitCode.SUCCESS);
    }
}
