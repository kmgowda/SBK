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
import io.sbk.gem.GemBenchmark;
import io.sbk.gem.GemConfig;
import io.sbk.gem.GemParameters;
import io.sbk.gem.SshConnection;
import io.sbk.gem.RemoteResponse;
import io.sbk.perl.State;
import io.sbk.system.Printer;
import lombok.Synchronized;
import org.apache.commons.lang.StringUtils;

import javax.annotation.concurrent.GuardedBy;
import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public class SbkGemBenchmark implements GemBenchmark {
    private final Benchmark ramBenchmark;
    private final GemConfig config;
    private final GemParameters params;
    private final String sbkArgs;
    private final CompletableFuture<RemoteResponse[]> retFuture;
    private final RemoteResponse[] remoteResults;
    private final ExecutorService executor;
    private final SbkSsh[] nodes;
    private final ConnectionsMap consMap;

    @GuardedBy("this")
    private State state;

    public SbkGemBenchmark(Benchmark ramBenchmark, GemConfig config, GemParameters params, String sbkArgs) {
        this.ramBenchmark = ramBenchmark;
        this.config = config;
        this.config.remoteTimeoutSeconds = Long.MAX_VALUE;
        this.params = params;
        this.sbkArgs = sbkArgs;
        this.retFuture =  new CompletableFuture<>();
        this.state = State.BEGIN;
        final SshConnection[] connections = params.getConnections();
        if (config.fork) {
            executor = new ForkJoinPool(connections.length + 10);
        } else {
            executor = Executors.newFixedThreadPool(connections.length + 10);
        }
        this.remoteResults = new RemoteResponse[connections.length];
        this.nodes = new SbkSsh[connections.length];
        for (int i = 0; i < connections.length; i++) {
            nodes[i] =  new SbkSsh(connections[i],  executor);
        }
        this.consMap = new ConnectionsMap(connections);
    }

    private final static class ConnectionsMap {
        private final Map<Map.Entry<String, String>, Boolean>  kMap;

        public ConnectionsMap(SshConnection[] conn) {
            this.kMap = new HashMap<>();
            for (SshConnection sshConnection : conn) {
                this.kMap.put(Map.entry(sshConnection.getHost().toLowerCase(), sshConnection.getDir().toLowerCase()), false);
            }
        }

        void reset() {
            this.kMap.keySet().forEach(k -> this.kMap.put(k, false));
        }

        void visit(SshConnection conn) {
            this.kMap.put(Map.entry(conn.getHost().toLowerCase(), conn.getDir().toLowerCase()), true);
        }

        boolean isVisited(SshConnection conn) {
            return this.kMap.get(Map.entry(conn.getHost().toLowerCase(), conn.getDir().toLowerCase()));
        }
    }

    @Override
    @Synchronized
    public CompletableFuture<RemoteResponse[]> start() throws IOException, InterruptedException, ExecutionException,
            IllegalStateException {
        if (state != State.BEGIN) {
            if (state == State.RUN) {
                Printer.log.warn("SBK GEM Benchmark is already running..");
            } else {
                Printer.log.warn("SBK GEM Benchmark is already shutdown..");
            }
            return retFuture;
        }
        state = State.RUN;
        Printer.log.info("SBK GEM Benchmark Started");
        final CompletableFuture<?>[] cfArray = new CompletableFuture[nodes.length];

        for (int i = 0; i < nodes.length; i++) {
            cfArray[i] = nodes[i].createSessionAsync(config.remoteTimeoutSeconds);
        }
        final CompletableFuture<Void> connsFuture = CompletableFuture.allOf(cfArray);

        for (int i = 0; i < config.maxIterations && !connsFuture.isDone(); i++) {
            try {
                connsFuture.get(config.timeoutSeconds, TimeUnit.SECONDS);
            } catch (TimeoutException ex) {
                Printer.log.info("SBK-GEM [" + (i + 1) + "]: Waiting for ssh session to remote hosts timeout");
            }
        }
        if (!connsFuture.isDone() || connsFuture.isCompletedExceptionally()) {
            final String errMsg = "SBK-GEM, remote session failed after " + config.maxIterations + " iterations";
            Printer.log.error(errMsg);
            throw new InterruptedException(errMsg);
        }
        Printer.log.info("SBK-GEM: Ssh session establishment Success..");

        final int  javaMajorVersion = Integer.parseInt(System.getProperty("java.runtime.version").
                split("\\.")[0]);

        final SshResponseStream[] sshResults = createMultiSshResponseStream(nodes.length, true);
        final String cmd = "java -version";
        consMap.reset();
        for (int i = 0; i < nodes.length; i++) {
            if (consMap.isVisited(nodes[i].connection)) {
                cfArray[i] = new CompletableFuture<>();
                cfArray[i].complete(null);
            } else {
                consMap.visit(nodes[i].connection);
                cfArray[i] = nodes[i].runCommandAsync(cmd, config.remoteTimeoutSeconds, sshResults[i]);
            }
        }
        final CompletableFuture<Void> ret = CompletableFuture.allOf(cfArray);

        for (int i = 0; i < config.maxIterations && !ret.isDone(); i++) {
            try {
                ret.get(config.timeoutSeconds, TimeUnit.SECONDS);
            } catch (TimeoutException ex) {
                Printer.log.info("SBK-GEM [" + (i + 1) + "]: Waiting for command: " + cmd + " timeout");
            }
        }
        boolean stop = false;
        if (!ret.isDone()) {
            final String errMsg = "SBK-GEM: command: " + cmd +" time out after " + config.maxIterations + " iterations";
            Printer.log.error(errMsg);
            throw new InterruptedException(errMsg);
        } else {
            for (int i = 0; i < sshResults.length; i++) {
                String stdOut = sshResults[i].stdOutputStream.toString();
                String stdErr = sshResults[i].errOutputStream.toString();
                if (javaMajorVersion > parseJavaVersion(stdOut) && javaMajorVersion > parseJavaVersion(stdErr)) {
                    Printer.log.info("Java version :" + javaMajorVersion+" , mismatch at : "+ nodes[i].connection.getHost());
                    stop = true;
                }
            }
            fillSshResults(sshResults);
        }

        if (stop) {
            throw new InterruptedException();
        }
        Printer.log.info("SBK-GEM: Matching Java Major Version: " +javaMajorVersion +" Success..");

        if (params.isCopy()) {

            final SshResponseStream[] results = createMultiSshResponseStream(nodes.length, false);
            consMap.reset();
            for (int i = 0; i < nodes.length; i++) {
                if (consMap.isVisited(nodes[i].connection)) {
                    cfArray[i] = new CompletableFuture<>();
                    cfArray[i].complete(null);
                } else {
                    consMap.visit(nodes[i].connection);
                    cfArray[i] = nodes[i].runCommandAsync("rm -rf " + nodes[i].connection.getDir(),
                            config.remoteTimeoutSeconds, results[i]);
                }
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
                final String errMsg = "SBK-GEM: command:  'rm -rf' time out after " + config.maxIterations +
                        " iterations";
                Printer.log.error(errMsg);
                throw new InterruptedException(errMsg);
            }
            final String remoteSBKdir = Paths.get(params.getSbkDir()).getFileName().toString();
            Printer.log.info("SBK-GEM: Removing older version of remote directory: '" + remoteSBKdir + "'  Success..");

            final SshResponseStream[] mkDirResults = createMultiSshResponseStream(nodes.length, false);
            consMap.reset();
            for (int i = 0; i < nodes.length; i++) {
                if (consMap.isVisited(nodes[i].connection)) {
                    cfArray[i] = new CompletableFuture<>();
                    cfArray[i].complete(null);
                } else {
                    consMap.visit(nodes[i].connection);
                    cfArray[i] = nodes[i].runCommandAsync("mkdir -p " + nodes[i].connection.getDir(),
                            config.remoteTimeoutSeconds, mkDirResults[i]);
                }
            }

            final CompletableFuture<Void> mkDirFuture = CompletableFuture.allOf(cfArray);

            for (int i = 0; !mkDirFuture.isDone(); i++) {
                try {
                    mkDirFuture.get(config.timeoutSeconds, TimeUnit.SECONDS);
                } catch (TimeoutException ex) {
                    Printer.log.info("SBK-GEM [" + (i + 1) + "]: Waiting for command '" + cmd + "' timeout");
                }
            }

            if (!mkDirFuture.isDone()) {
                final String errMsg = "SBK-GEM, command:  'mkdir' time out after " + config.maxIterations + " iterations";
                Printer.log.error(errMsg);
                throw new InterruptedException(errMsg);
            }

            Printer.log.info("SBK-GEM: Creating remote directory: '" + remoteSBKdir + "'  Success..");
            consMap.reset();
            for (int i = 0; i < nodes.length; i++) {
                if (consMap.isVisited(nodes[i].connection)) {
                    cfArray[i] = new CompletableFuture<>();
                    cfArray[i].complete(null);
                } else {
                    consMap.visit(nodes[i].connection);
                    cfArray[i] = nodes[i].copyDirectoryAsync(params.getSbkDir(), nodes[i].connection.getDir());
                }
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

            Printer.log.info("Copy SBK application: '"+ params.getSbkCommand() +"' to remote nodes Success..");

        } //end of copy

        // start SBK RAM
        ramBenchmark.start();

        // Start remote SBK instances
        final SshResponseStream[] sbkResults = createMultiSshResponseStream(nodes.length, true);
        final String sbkDir = Paths.get(params.getSbkDir()).getFileName().toString();
        final String sbkCommand = sbkDir + File.separator + GemConfig.BIN_DIR + File.separator + params.getSbkCommand()+" "+sbkArgs;
        Printer.log.info("SBK-GEM: Remote SBK command: " +sbkCommand);
        for (int i = 0; i < nodes.length; i++) {
            cfArray[i] = nodes[i].runCommandAsync(nodes[i].connection.getDir()+ File.separator + sbkCommand,
                    config.remoteTimeoutSeconds, sbkResults[i]);
        }
        final CompletableFuture<Void> sbkFuture = CompletableFuture.allOf(cfArray);
        sbkFuture.exceptionally(ex -> {
           shutdown(ex);
           return null;
        });

        sbkFuture.thenAccept(x -> {
            fillSshResults(sbkResults);
            shutdown(null);
        });

        return retFuture;
    }


    private static int parseJavaVersion( String text) {
        if (StringUtils.isEmpty(text)) {
            return Integer.MAX_VALUE;
        }
        final String[] tmp = text.split("\"", 2);
        return  Integer.parseInt(tmp[1].split("\\.")[0]);
    }


    private static SshResponseStream[] createMultiSshResponseStream(int length, boolean stdout) {
        final SshResponseStream[] results = new SshResponseStream[length];
        for (int i = 0; i < results.length; i++) {
            results[i] = new SshResponseStream(stdout);
        }
        return results;
    }

    @Synchronized
    private void fillSshResults(SshResponseStream[] responseStreams) {
        final SshConnection[] connections = params.getConnections();
        for (int i = 0; i < remoteResults.length; i++) {
            remoteResults[i] = new RemoteResponse(responseStreams[i].returnCode, responseStreams[i].stdOutputStream.toString(),
                    responseStreams[i].errOutputStream.toString(), connections[i].getHost());
        }
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
        if (state != State.END) {
            state = State.END;
            ramBenchmark.stop();
            if (ex != null) {
                Printer.log.warn("SBK GEM Benchmark Shutdown with Exception " + ex);
                retFuture.completeExceptionally(ex);
            } else {
                Printer.log.info("SBK GEM Benchmark Shutdown");
                retFuture.complete(remoteResults);
            }
        }
    }


    @Override
    public void stop() {
        for (SbkSsh node: nodes) {
            node.stop();
        }
        shutdown(null);
    }
}
