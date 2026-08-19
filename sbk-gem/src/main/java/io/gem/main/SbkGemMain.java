/**
 * Copyright (c) KMG. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.gem.main;

import io.gem.api.impl.SbkGem;
import io.sbk.config.ExitCode;
import io.gem.config.GemConfig;
import io.gem.exception.SbkGemParameterException;
import io.sbk.webconsole.WebConsoleClient.WebConsoleBusyException;
import io.sbk.utils.SbkUtils;
import org.apache.commons.cli.ParseException;
import org.apache.commons.cli.UnrecognizedOptionException;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;

/**
 * Class SbkGemMain.
 */
public abstract class SbkGemMain {

    /**
     * Creates an SBK-GEM command-line entry point.
     */
    public SbkGemMain() {
    }

    /**
     * This method is the main method of Sbk-Gem module.
     *
     * @param args String[]
     */
    static void main(final String[] args) {
        if (SbkUtils.hasVersion(args)) {
            final String version = io.gem.api.impl.SbkGem.class.getPackage().getImplementationVersion();
            System.out.println(GemConfig.NAME.toUpperCase() + " Version: " + version);
            System.exit(ExitCode.SUCCESS);
        }
        try {
            SbkGem.run(args, null, null, null);
        } catch (WebConsoleBusyException ex) {
            System.exit(ExitCode.FAILURE);
        } catch (UnrecognizedOptionException ex) {
            System.exit(ExitCode.INVALID_ARGUMENT);
        } catch (SbkGemParameterException ex) {
            System.exit(ExitCode.FAILURE);
        } catch (ParseException | IllegalArgumentException | IOException | TimeoutException | InterruptedException |
                 ExecutionException | InstantiationException | ClassNotFoundException | InvocationTargetException |
                 NoSuchMethodException | IllegalAccessException ex) {
            ex.printStackTrace();
            System.exit(ExitCode.FAILURE);
        }
        System.exit(ExitCode.SUCCESS);
    }
}
