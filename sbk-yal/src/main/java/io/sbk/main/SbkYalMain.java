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
import io.sbk.config.ExitCode;
import io.sbk.webconsole.WebConsoleClient.WebConsoleBusyException;
import io.sbk.exception.HelpException;
import io.sbk.utils.SbkUtils;
import org.apache.commons.cli.ParseException;
import org.apache.commons.cli.UnrecognizedOptionException;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;

/**
 * Main class of SBK-YAL.
 */
public abstract class SbkYalMain {

    /**
     * Creates an SBK-YAL command-line entry point.
     */
    public SbkYalMain() {
    }

    static void main(final String[] args) {
        if (SbkUtils.hasVersion(args)) {
            final String version = io.sbk.api.impl.SbkYal.class.getPackage().getImplementationVersion();
            System.out.println("SBK-YAL Version: " + version);
            System.exit(ExitCode.SUCCESS);
        }
        try {
            SbkYal.run(args, null, null, null);
        } catch (WebConsoleBusyException ex) {
            System.exit(ExitCode.FAILURE);
        } catch (HelpException ex) {
            System.exit(ExitCode.SUCCESS);
        } catch (UnrecognizedOptionException ex) {
            System.exit(ExitCode.INVALID_ARGUMENT);
        } catch (ParseException | IllegalArgumentException | IOException | TimeoutException | InterruptedException |
                ExecutionException | ClassNotFoundException |  InvocationTargetException | InstantiationException |
                NoSuchMethodException | IllegalAccessException ex) {
            ex.printStackTrace();
            System.exit(ExitCode.FAILURE);
        }
        System.exit(ExitCode.SUCCESS);
    }
}
