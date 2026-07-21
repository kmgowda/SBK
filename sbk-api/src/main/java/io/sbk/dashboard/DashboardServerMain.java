/**
 * Copyright (c) KMG. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 */
package io.sbk.dashboard;

import java.io.IOException;

/**
 * Standalone entry point for the reusable local SBK dashboard server.
 */
public abstract class DashboardServerMain {

    /**
     * Creates a dashboard server entry point.
     */
    public DashboardServerMain() {
    }

    /**
     * Starts the dashboard server and waits until the process is terminated.
     *
     * @param args {@code -host}, {@code -port}, and {@code -retention} options
     * @throws IOException if the server cannot start
     * @throws InterruptedException if the process is interrupted
     * @throws IllegalArgumentException if an option or value is invalid
     */
    public static void main(String[] args) throws IOException, InterruptedException {
        String host = "0.0.0.0";
        int port = 9720;
        int retention = 3600;
        for (int index = 0; index < args.length; index += 2) {
            if (index + 1 >= args.length) {
                throw new IllegalArgumentException("Missing dashboard option value for " + args[index]);
            }
            switch (args[index]) {
                case "-host" -> host = args[index + 1];
                case "-port" -> port = Integer.parseInt(args[index + 1]);
                case "-retention" -> retention = Integer.parseInt(args[index + 1]);
                default -> throw new IllegalArgumentException("Unknown dashboard option " + args[index]);
            }
        }
        final DashboardServer dashboardServer = new DashboardServer(host, port, retention);
        Runtime.getRuntime().addShutdownHook(new Thread(dashboardServer::close));
        dashboardServer.start();
        dashboardServer.awaitTermination();
    }
}
