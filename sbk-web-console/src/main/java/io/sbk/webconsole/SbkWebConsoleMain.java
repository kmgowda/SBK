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
import java.time.Duration;

/**
 * Standalone entry point for the reusable SBK Local Web Console server.
 */
public final class SbkWebConsoleMain {
    private SbkWebConsoleMain() {
    }

    /**
     * Starts the Local Web Console server and waits until the process is terminated.
     *
     * @param args {@code -port}, {@code -websnapshotminutes}, and {@code -webtimeoutminutes} options
     * @throws IOException if the server cannot start
     * @throws InterruptedException if the process is interrupted
     * @throws IllegalArgumentException if an option or value is invalid
     */
    public static void main(String[] args) throws IOException, InterruptedException {
        final WebConsoleConfig config = WebConsoleConfig.load();
        int port = config.port;
        int minutes = config.snapshotMinutes;
        int timeout = config.timeoutMinutes;
        for (int index = 0; index < args.length; index += 2) {
            if (index + 1 >= args.length) {
                throw new IllegalArgumentException("Missing Local Web Console option value for " + args[index]);
            }
            switch (args[index]) {
                case "-port" -> port = Integer.parseInt(args[index + 1]);
                case "-websnapshotminutes" -> minutes = Integer.parseInt(args[index + 1]);
                case "-webtimeoutminutes" -> timeout = Integer.parseInt(args[index + 1]);
                default -> throw new IllegalArgumentException("Unknown Local Web Console option " + args[index]);
            }
        }
        final WebConsoleServer webConsoleServer = new WebConsoleServer(port,
                retentionSnapshots(minutes, config.reportingIntervalSeconds), Duration.ofMinutes(timeout));
        Runtime.getRuntime().addShutdownHook(new Thread(webConsoleServer::close));
        webConsoleServer.start();
        webConsoleServer.awaitTermination();
    }

    /**
     * Converts a web console history duration to the bounded number of periodic snapshots stored in memory.
     *
     * @param minutes history duration in minutes
     * @return snapshot capacity at SBK's default reporting interval
     * @throws IllegalArgumentException if the duration is not positive or exceeds the supported capacity
     */
    static int retentionSnapshots(int minutes) {
        return retentionSnapshots(minutes, WebConsoleConfig.load().reportingIntervalSeconds);
    }

    static int retentionSnapshots(int minutes, int reportingIntervalSeconds) {
        if (minutes < 1) {
            throw new IllegalArgumentException("Local Web Console history minutes must be greater than zero");
        }
        final long retention = Math.ceilDiv(Math.multiplyExact((long) minutes, 60L),
                reportingIntervalSeconds);
        if (retention > Integer.MAX_VALUE) {
            throw new IllegalArgumentException("Local Web Console history minutes are too large: " + minutes);
        }
        return (int) retention;
    }
}
