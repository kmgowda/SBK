/**
 * Copyright (c) KMG. All Rights Reserved..
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 */

package io.sbk.gem.impl;

import io.sbk.api.Benchmark;
import io.sbk.gem.GemConfig;
import io.sbk.gem.GemParameters;
import io.sbk.gem.Ssh;
import io.sbk.gem.SshConnection;
import io.sbk.gem.SshResponse;
import io.sbk.system.Printer;
import lombok.Synchronized;
import org.apache.commons.lang.StringUtils;

import javax.annotation.concurrent.GuardedBy;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public class SbkGemBenchmark implements Benchmark {
    private final Benchmark ramBenchmark;
    private final GemConfig config;
    private final GemParameters params;
    private final String sbkArgs;
    private final ExecutorService executor;

    @GuardedBy("this")
    private CompletableFuture<Void> retFuture;

    public SbkGemBenchmark(Benchmark ramBenchmark, GemConfig config, GemParameters params, String sbkArgs) {
        this.ramBenchmark = ramBenchmark;
        this.config = config;
        this.config.remoteTimeoutSeconds = Long.MAX_VALUE;
        this.params = params;
        this.sbkArgs = sbkArgs;
        if (config.fork) {
            executor = new ForkJoinPool(params.getConnections().length + 10);
        } else {
            executor = Executors.newFixedThreadPool(params.getConnections().length + 10);
        }
    }


    @Override
    public CompletableFuture<Void> start() throws IOException, InterruptedException, ExecutionException, IllegalStateException {
        final int  javaMajorVersion = Integer.parseInt(System.getProperty("java.runtime.version").
                split("\\.")[0]);
        final SshConnection[] conns = params.getConnections();
        final CompletableFuture[] cfArray = new CompletableFuture[conns.length];
        boolean stop = false;

        final SshResponse[] sshResults = createMultiSshResponse(conns.length, true);
        final String cmd = "java -version";
        for (int i = 0; i < conns.length; i++) {
            cfArray[i] = Ssh.runCommandAsync(conns[i], config.remoteTimeoutSeconds, cmd,
                    sshResults[i], executor);
        }
        final CompletableFuture<Void> ret = CompletableFuture.allOf(cfArray);

        for (int i = 0; i < config.maxIterations && !ret.isDone(); i++) {
            try {
                ret.get(config.timeoutSeconds, TimeUnit.SECONDS);
            } catch (TimeoutException ex) {
                Printer.log.info("SBK-GEM [" + (i + 1) + "]: Waiting for command: " + cmd + " timeout");
            }
        }

        if (!ret.isDone()) {
            final String errMsg = "SBK-GEM, command: " + cmd +" time out after " + config.maxIterations + " iterations";
            Printer.log.error(errMsg);
            throw new InterruptedException(errMsg);
        } else {
            for (int i = 0; i < sshResults.length; i++) {
                String stdOut = sshResults[i].stdOutput.toString();
                String stdErr = sshResults[i].errOutput.toString();
                if (javaMajorVersion > parseJavaVersion(stdOut) && javaMajorVersion > parseJavaVersion(stdErr)) {
                    Printer.log.info("Java version :" + javaMajorVersion+" , mismatch at : "+conns[i].getHost());
                    stop = true;
                }
            }
        }

        if (stop) {
            throw new InterruptedException();
        }
        Printer.log.info("Java version match Success..");

        final SshResponse[] results = createMultiSshResponse(conns.length, false);
        for (int i = 0; i < conns.length; i++) {
            cfArray[i] = Ssh.runCommandAsync(conns[i], config.remoteTimeoutSeconds, "rm -rf " + conns[i].getDir(),
                    results[i], executor);
        }
        final CompletableFuture<Void> rmFuture = CompletableFuture.allOf(cfArray);

        for (int i = 0; i < config.maxIterations && !rmFuture.isDone(); i++) {
            try {
                rmFuture.get(config.timeoutSeconds, TimeUnit.SECONDS);
            } catch (TimeoutException ex) {
                Printer.log.info("SBK-GEM [" + (i + 1) + "]: Waiting for command: " + cmd + " timeout");
            }
        }

        if (!rmFuture.isDone()) {
            final String errMsg = "SBK-GEM, command:  'rm -rf' time out after " + config.maxIterations + " iterations";
            Printer.log.error(errMsg);
            throw new InterruptedException(errMsg);
        }

        final SshResponse[] mkDirResults = createMultiSshResponse(conns.length, false);

        for (int i = 0; i < conns.length; i++) {
            cfArray[i] = Ssh.runCommandAsync(conns[i], config.remoteTimeoutSeconds, "mkdir -p " + conns[i].getDir(),
                    mkDirResults[i], executor);
        }

        final CompletableFuture<Void> mkDirFuture = CompletableFuture.allOf(cfArray);

        for (int i = 0; !mkDirFuture.isDone(); i++) {
            try {
                mkDirFuture.get(config.timeoutSeconds, TimeUnit.SECONDS);
            } catch (TimeoutException ex) {
                Printer.log.info("SBK-GEM [" + (i + 1) + "]: Waiting for command: " + cmd + " timeout");
            }
        }

        if (!mkDirFuture.isDone()) {
            final String errMsg = "SBK-GEM, command:  'mkdir' time out after " + config.maxIterations + " iterations";
            Printer.log.error(errMsg);
            throw new InterruptedException(errMsg);
        }

        for (int i = 0; i < conns.length; i++) {
            cfArray[i] = Ssh.copyDirectoryAsync(conns[i], config.remoteTimeoutSeconds, params.getSbkDir(),
                    conns[i].getDir(), executor);
        }
        final CompletableFuture<Void> copyCB = CompletableFuture.allOf(cfArray);

        for (int i = 0; !copyCB.isDone(); i++) {
            try {
                copyCB.get(config.timeoutSeconds, TimeUnit.SECONDS);
            } catch (TimeoutException ex) {
                Printer.log.info("SBK-GEM [" + (i + 1) + "]: Waiting for copy command to complete");
            }
        }

        if (copyCB.isCompletedExceptionally()) {
            final String errMsg = "SBK-GEM, command:  copy command failed!";
            Printer.log.error(errMsg);
            throw new InterruptedException(errMsg);
        }

        Printer.log.info("Copy command Success..");

        ramBenchmark.start();

        final SshResponse[] sbkResults = createMultiSshResponse(conns.length, true);

        final String sbkDir = Paths.get(params.getSbkDir()).getFileName().toString();
        final String sbkCommand = sbkDir + "/" + GemConfig.BIN_EXT_PATH + "/" + params.getSbkCommand()+" "+sbkArgs;
        Printer.log.info("sbk command : " +sbkCommand);
        for (int i = 0; i < conns.length; i++) {
            cfArray[i] = Ssh.runCommandAsync(conns[i], config.remoteTimeoutSeconds,
                    conns[i].getDir()+"/"+sbkCommand, sbkResults[i], executor);
        }
        final CompletableFuture<Void> sbkFuture = CompletableFuture.allOf(cfArray);
        sbkFuture.exceptionally(ex -> {
           shutdown(ex);
           return null;
        });

        sbkFuture.thenAccept(x -> {
            shutdown(null);
        });

        retFuture  = new CompletableFuture<>();
        return retFuture;
    }


    private static int parseJavaVersion( String text) {
        if (StringUtils.isEmpty(text)) {
            return Integer.MAX_VALUE;
        }
        final String[] tmp = text.split("\"", 2);
        return  Integer.parseInt(tmp[1].split("\\.")[0]);
    }


    private static SshResponse[] createMultiSshResponse(int length, boolean stdout) {
        final SshResponse[] results = new SshResponse[length];
        for (int i = 0; i < results.length; i++) {
            results[i] = new SshResponse(stdout);
        }
        return results;
    }


    /**
     * Shutdown SBK Benchmark.
     *
     * closes all writers/readers.
     * closes the storage device/client.
     *
     */
    @Synchronized
    private void shutdown(Throwable ex) {
        if (retFuture == null) {
            return;
        }

        if (retFuture.isDone()) {
            retFuture = null;
            return;
        }
        ramBenchmark.stop();

        if (ex != null) {
            Printer.log.warn("SBK GEM Shutdown with Exception "+ ex);
            retFuture.completeExceptionally(ex);
        } else {
            Printer.log.info("SBK GEM Shutdown");
            retFuture.complete(null);
        }
        retFuture = null;
    }


    @Override
    public void stop() {
        shutdown(null);
    }
}
