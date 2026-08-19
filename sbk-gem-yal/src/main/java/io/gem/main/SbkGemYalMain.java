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

import io.gem.api.impl.SbkGemYal;
import io.sbk.config.ExitCode;
import io.gem.exception.SbkGemParameterException;
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
 * Class SbkGemYalMain.
 */
public abstract class SbkGemYalMain {

    /**
     * Creates an SBK-GEM-YAL command-line entry point.
     */
    public SbkGemYalMain() {
    }

    /**
     * The main Method of sbk-gem-yal module.
     *
     * @param args String[]
     */
    static void main(final String[] args) {
        if (SbkUtils.hasVersion(args)) {
            final String version = io.gem.api.impl.SbkGemYal.class.getPackage().getImplementationVersion();
            System.out.println("SBK-GEM-YAL Version: " + version);
            System.exit(ExitCode.SUCCESS);
        }
        try {
            SbkGemYal.run(args, null, null, null);
        } catch (WebConsoleBusyException ex) {
            System.exit(ExitCode.FAILURE);
        } catch (HelpException ex) {
            System.exit(ExitCode.SUCCESS);
        } catch (UnrecognizedOptionException ex) {
            System.exit(ExitCode.INVALID_ARGUMENT);
        } catch (SbkGemParameterException ex) {
            System.exit(ExitCode.FAILURE);
        } catch (ParseException | IllegalArgumentException | IOException | TimeoutException | InterruptedException |
                 ExecutionException | ClassNotFoundException | InvocationTargetException | InstantiationException |
                 NoSuchMethodException | IllegalAccessException ex) {
            ex.printStackTrace();
            System.exit(ExitCode.FAILURE);
        }
        System.exit(ExitCode.SUCCESS);
    }
}
