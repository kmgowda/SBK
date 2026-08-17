/**
 * Copyright (c) KMG. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 */
package io.sbk.webconsole;

import java.io.IOException;

/**
 * Compatibility entry point for the standalone Local Web Console server.
 *
 * @deprecated use {@link WebConsoleMain}
 */
@Deprecated(forRemoval = false)
public abstract class SbkWebConsoleMain {

    /**
     * Creates a Local Web Console server entry point.
     */
    public SbkWebConsoleMain() {
    }

    /**
     * Starts the Local Web Console server and waits until the process is terminated.
     *
     * @param args {@code -host}, {@code -port}, and {@code -minutes} options
     * @throws IOException if the server cannot start
     * @throws InterruptedException if the process is interrupted
     * @throws IllegalArgumentException if an option or value is invalid
     */
    public static void main(String[] args) throws IOException, InterruptedException {
        WebConsoleMain.main(args);
    }
}
