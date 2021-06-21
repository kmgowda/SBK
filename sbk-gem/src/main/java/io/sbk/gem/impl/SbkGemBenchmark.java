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

import java.io.IOException;
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
    private final String srcDir;
    private final String dstDir;
    private final String sbkCommand;
    private final ExecutorService executor;


    public SbkGemBenchmark(Benchmark ramBenchmark, GemConfig config, GemParameters params, String srcDir, String dstDir,
                           String sbkCommand) {
        this.ramBenchmark = ramBenchmark;
        this.config = config;
        this.params = params;
        this.srcDir = srcDir;
        this.dstDir = dstDir;
        this.sbkCommand = sbkCommand;
        if (config.fork) {
            executor = new ForkJoinPool(params.getConnections().length + 10);
        } else {
            executor = Executors.newFixedThreadPool(params.getConnections().length + 10);
        }
    }


    @Override
    public CompletableFuture<Void> start() throws IOException, InterruptedException, ExecutionException, IllegalStateException {
        final int  javaMajorVersion = Integer.parseInt(System.getProperty("java.runtime.version").
                split(".", 2)[0]);
        Printer.log.info("Java Major version: "+javaMajorVersion);
        final SshConnection[] conns = params.getConnections();

        final SshResponse[] sshResults = new SshResponse[conns.length];
        for (int i = 0; i < sshResults.length; i++) {
            sshResults[i] = new SshResponse(true);
        }
        final CompletableFuture[] cfArray = new CompletableFuture[conns.length];
        final String cmd = "java -version";
        for (int i = 0; i < conns.length; i++) {
            cfArray[i] = Ssh.runCommandAsync(conns[i], config.timeoutSeconds, cmd,
                    sshResults[i], executor);
        }
        final CompletableFuture<Void> ret = CompletableFuture.allOf(cfArray);

        for (int i = 0; i < config.maxIterations && !ret.isDone(); i++) {
            try {
                ret.get(config.timeoutSeconds, TimeUnit.SECONDS);
            } catch (TimeoutException ex) {
                Printer.log.info("SBK-GEM [" + "i" + "]: Waiting for command: " + cmd + " complete");
            }
        }
        if (!ret.isDone()) {
            final String errMsg = "SBK-GEM, command: " + cmd +" time out after " + config.maxIterations + " iterations";
            Printer.log.error(errMsg);
            throw new InterruptedException(errMsg);
        } else {
            for (int i = 0; i < sshResults.length; i++) {
                Printer.log.info("[" + i+ "] , stdout : "+sshResults[i].stdOutput.toString());
                Printer.log.info("[" + i+ "] , stderr : "+sshResults[i].errOutput.toString());
            }
        }

        return null;

    }

    @Override
    public void stop() {

    }
}
