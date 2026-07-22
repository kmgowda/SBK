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

import io.perl.config.PerlConfig;

import java.io.IOException;

/**
 * Standalone entry point for the reusable local SBK dashboard server.
 */
public abstract class SbkDashboardServerMain {

    /**
     * Creates a dashboard server entry point.
     */
    public SbkDashboardServerMain() {
    }

    /**
     * Starts the dashboard server and waits until the process is terminated.
     *
     * @param args {@code -host}, {@code -port}, and {@code -minutes} options
     * @throws IOException if the server cannot start
     * @throws InterruptedException if the process is interrupted
     * @throws IllegalArgumentException if an option or value is invalid
     */
    public static void main(String[] args) throws IOException, InterruptedException {
        String host = "0.0.0.0";
        int port = 9720;
        int minutes = 180;
        for (int index = 0; index < args.length; index += 2) {
            if (index + 1 >= args.length) {
                throw new IllegalArgumentException("Missing dashboard option value for " + args[index]);
            }
            switch (args[index]) {
                case "-host" -> host = args[index + 1];
                case "-port" -> port = Integer.parseInt(args[index + 1]);
                case "-minutes" -> minutes = Integer.parseInt(args[index + 1]);
                default -> throw new IllegalArgumentException("Unknown dashboard option " + args[index]);
            }
        }
        final DashboardServer dashboardServer = new DashboardServer(host, port, retentionSnapshots(minutes));
        Runtime.getRuntime().addShutdownHook(new Thread(dashboardServer::close));
        dashboardServer.start();
        dashboardServer.awaitTermination();
    }

    /**
     * Converts a dashboard history duration to the bounded number of periodic snapshots stored in memory.
     *
     * @param minutes history duration in minutes
     * @return snapshot capacity at SBK's default reporting interval
     * @throws IllegalArgumentException if the duration is not positive or exceeds the supported capacity
     */
    static int retentionSnapshots(int minutes) {
        if (minutes < 1) {
            throw new IllegalArgumentException("Dashboard history minutes must be greater than zero");
        }
        final long retention = Math.ceilDiv(Math.multiplyExact((long) minutes, 60L),
                PerlConfig.DEFAULT_PRINTING_INTERVAL_SECONDS);
        if (retention > Integer.MAX_VALUE) {
            throw new IllegalArgumentException("Dashboard history minutes are too large: " + minutes);
        }
        return (int) retention;
    }
}
