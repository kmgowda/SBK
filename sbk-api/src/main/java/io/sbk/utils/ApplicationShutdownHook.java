/**
 * Copyright (c) KMG. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 */
package io.sbk.utils;

import io.sbk.system.Printer;

import java.util.concurrent.TimeUnit;

/**
 * Registers a bounded JVM shutdown hook for an SBK application.
 *
 * <p>The actual application cleanup runs on a separate daemon thread. The JVM
 * hook waits for it only for a bounded interval, ensuring that a blocked
 * storage driver, SSH operation, logger, or network server cannot prevent the
 * process from terminating after {@code SIGINT} or {@code SIGTERM}.
 */
public final class ApplicationShutdownHook {
    private static final long SHUTDOWN_TIMEOUT_SECONDS = 3;

    private ApplicationShutdownHook() {
    }

    /**
     * Registers a shutdown hook for an application benchmark.
     *
     * @param applicationName application name used in lifecycle messages
     * @param cleanup cleanup operation, normally {@code benchmark.stop()}
     * @return the registered hook, which can be removed after normal completion
     */
    public static Thread register(String applicationName, Runnable cleanup) {
        final Thread hook = new Thread(
                () -> runBounded(applicationName, cleanup, SHUTDOWN_TIMEOUT_SECONDS, TimeUnit.SECONDS),
                applicationName + "-shutdown-hook");
        Runtime.getRuntime().addShutdownHook(hook);
        return hook;
    }

    /**
     * Removes a previously registered hook after normal application shutdown.
     *
     * <p>If JVM shutdown has already started, hook removal is no longer legal;
     * this method deliberately ignores that race because the hook is already
     * performing the requested cleanup.
     *
     * @param hook hook returned by {@link #register(String, Runnable)}
     */
    public static void remove(Thread hook) {
        try {
            Runtime.getRuntime().removeShutdownHook(hook);
        } catch (IllegalStateException ignored) {
            // JVM shutdown is already in progress.
        }
    }

    static void runBounded(String applicationName, Runnable cleanup, long timeout, TimeUnit unit) {
        System.out.println();
        Printer.log.info("{}: Shutdown signal received", applicationName);
        final Thread cleanupThread = Thread.ofPlatform()
                .daemon(true)
                .name(applicationName + "-shutdown-cleanup")
                .unstarted(cleanup);
        cleanupThread.start();
        try {
            cleanupThread.join(unit.toMillis(timeout));
            if (cleanupThread.isAlive()) {
                Printer.log.warn("{}: Graceful shutdown exceeded {} {}; forcing process exit",
                        applicationName, timeout, unit.toString().toLowerCase());
            }
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            Printer.log.warn("{}: Shutdown hook interrupted; forcing process exit", applicationName);
        }
    }
}
