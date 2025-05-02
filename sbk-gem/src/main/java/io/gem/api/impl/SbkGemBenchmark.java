/**
 * Copyright (c) KMG. All Rights Reserved..
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.gem.api.impl;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.gem.api.SshResponse;
import io.gem.api.SshSession;
import io.gem.config.GemConfig;
import io.gem.api.GemBenchmark;
import io.gem.api.RemoteResponse;
import io.gem.api.ConnectionConfig;
import io.gem.params.GemParameters;
import io.sbk.api.Benchmark;
import io.sbk.system.Printer;
import io.state.State;
import lombok.Synchronized;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;

import javax.annotation.concurrent.GuardedBy;
import java.io.File;
import java.io.IOException;
import java.net.ConnectException;
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

/**
 * Class SbkGemBenchmark.
 */
final public class SbkGemBenchmark implements GemBenchmark {
    private final Benchmark sbmBenchmark;
    private final GemConfig config;
    private final GemParameters params;
    private final String sbkArgs;
    private final CompletableFuture<RemoteResponse[]> retFuture;
    private final RemoteResponse[] remoteResults;
    private final ExecutorService executor;
    private final SshSession[] nodes;
    private final ConnectionsMap consMap;

    @GuardedBy("this")
    private State state;

    /**
     * Constructor SbkGemBenchmark is responsible for initializing all values.
     *
     * @param sbmBenchmark  Benchmark
     * @param config        NotNull GemConfig
     * @param params        NotNull GemParameters
     * @param sbkArgs       String
     */
    public SbkGemBenchmark(Benchmark sbmBenchmark, @NotNull GemConfig config, @NotNull GemParameters params, String sbkArgs) {
        this.sbmBenchmark = sbmBenchmark;
        this.config = config;
        this.config.remoteTimeoutSeconds = Long.MAX_VALUE;
        this.params = params;
        this.sbkArgs = sbkArgs;
        this.retFuture = new CompletableFuture<>();
        this.state = State.BEGIN;
        final ConnectionConfig[] connections = params.getConnections();
        if (config.fork) {
            executor = new ForkJoinPool(connections.length + 10);
        } else {
            executor = Executors.newFixedThreadPool(connections.length + 10);
        }
        this.remoteResults = new RemoteResponse[connections.length];
        this.nodes = new SshSession[connections.length];
        for (int i = 0; i < connections.length; i++) {
            nodes[i] = new SshSession(connections[i], executor);
        }
        this.consMap = new ConnectionsMap(connections);
    }

    private static int parseJavaVersion(String text) {
        if (StringUtils.isEmpty(text)) {
            return Integer.MAX_VALUE;
        }
        final String[] tmp = text.split("\"", 2);
        return Integer.parseInt(tmp[1].split("\\.")[0]);
    }

    @Override
    @Synchronized
    @SuppressFBWarnings("NP_NULL_ON_SOME_PATH_FROM_RETURN_VALUE")
    @SuppressWarnings("unchecked")
    public CompletableFuture<RemoteResponse[]> start() throws IOException, InterruptedException, ExecutionException,
            IllegalStateException {
        if (state != State.BEGIN) {
            if (state == State.RUN) {
                Printer.log.warn("SBK GEM Benchmark is already running..");
            } else {
                Printer.log.warn("SBK GEM Benchmark is already shutdown..");
            }
            return retFuture.toCompletableFuture();
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

        final int javaMajorVersion = Integer.parseInt(System.getProperty("java.runtime.version").
                split("\\.")[0].substring(0, 2));

        final CompletableFuture<SshResponse>[] cfResults = new CompletableFuture[nodes.length];
        final SshResponse[] sshResults = new SshResponse[cfArray.length];
        final String cmd = "java -version";
        consMap.reset();
        for (int i = 0; i < nodes.length; i++) {
            if (consMap.isVisited(nodes[i].connection)) {
                cfResults[i] = new CompletableFuture<>();
                SshResponse dummyResponse = new SshResponse(true);
                dummyResponse.returnCode = -1;
                cfResults[i].complete(dummyResponse);
            } else {
                consMap.visit(nodes[i].connection);
                cfResults[i] = nodes[i].runCommandAsync(cmd, true, config.remoteTimeoutSeconds );
            }
        }
        final CompletableFuture<Void> ret = CompletableFuture.allOf(cfResults);

        for (int i = 0; i < config.maxIterations && !ret.isDone(); i++) {
            try {
                ret.get(config.timeoutSeconds, TimeUnit.SECONDS);
            } catch (TimeoutException ex) {
                Printer.log.info("SBK-GEM [" + (i + 1) + "]: Waiting for command: " + cmd + " timeout");
            }
        }
        boolean stop = false;
        if (!ret.isDone()) {
            final String errMsg = "SBK-GEM: command: " + cmd + " time out after " + config.maxIterations + " iterations";
            Printer.log.error(errMsg);
            throw new InterruptedException(errMsg);
        } else {
            for (int i = 0; i < cfResults.length; i++) {
                sshResults[i] =  cfResults[i].get();
                if (sshResults[i] != null) {
                    String stdOut = sshResults[i].stdOutputStream.toString();
                    String stdErr = sshResults[i].errOutputStream.toString();
                    if (javaMajorVersion > parseJavaVersion(stdOut) && javaMajorVersion > parseJavaVersion(stdErr)) {
                        Printer.log.info("Java version :" + javaMajorVersion + " , mismatch at : " + nodes[i].connection.getHost());
                        stop = true;
                    }
                }
            }
            fillSshResults(sshResults);
        }

        if (stop) {
            throw new InterruptedException();
        }
        Printer.log.info("SBK-GEM: Matching Java Major Version: " + javaMajorVersion + " Success..");
        final String remoteDir = Paths.get(params.getSbkDir()).getFileName().toString();

        if (params.isCopy()) {

            if (remoteDirectoryDelete()) {
                Printer.log.info("SBK-GEM: Removing the remote directory: '" + remoteDir + "'  Success..");
            } else {
                String errStr = "SBK-GEM: Removing the remote directory: '" + remoteDir + "'  failed..";
                Printer.log.info(errStr);
                throw new InterruptedException(errStr);
            }

            consMap.reset();
            for (int i = 0; i < nodes.length; i++) {
                if (consMap.isVisited(nodes[i].connection)) {
                    cfArray[i] = new CompletableFuture<>();
                    cfArray[i].complete(null);
                } else {
                    consMap.visit(nodes[i].connection);
                    cfArray[i] = nodes[i].runCommandAsync("mkdir -p " + nodes[i].connection.getDir(),
                            true, config.remoteTimeoutSeconds );
                }
            }

            final CompletableFuture<Void> mkDirFuture = CompletableFuture.allOf(cfArray);
            for (int i = 0; !mkDirFuture.isDone(); i++) {
                try {
                    mkDirFuture.get(config.timeoutSeconds, TimeUnit.SECONDS);
                } catch (TimeoutException ex) {
                    Printer.log.info("SBK-GEM [" + (i + 1) + "]: Waiting for command 'mkdir -p' timeout");
                }
            }

            if (!mkDirFuture.isDone()) {
                final String errMsg = "SBK-GEM, command:  'mkdir' time out after " + config.maxIterations + " iterations";
                Printer.log.error(errMsg);
                throw new InterruptedException(errMsg);
            }

            Printer.log.info("SBK-GEM: Creating remote directory: '" + remoteDir + "'  Success..");
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

            Printer.log.info("Copy SBK application: '" + params.getSbkCommand() + "' to remote nodes Success..");

        } //end of copy

        // start SBM
        sbmBenchmark.start();

        // Start remote SBK instances
        final SshResponse[] sbkResults = new SshResponse[nodes.length];
        final String sbkDir = Paths.get(params.getSbkDir()).getFileName().toString();
        final String sbkCommand = sbkDir + File.separator + params.getSbkCommand() + " " + sbkArgs;
        Printer.log.info("SBK-GEM: Remote SBK command: " + sbkCommand);
        for (int i = 0; i < nodes.length; i++) {
            cfResults[i] = nodes[i].runCommandAsync(nodes[i].connection.getDir() + File.separator + sbkCommand,
                    false, config.remoteTimeoutSeconds );
        }
        final CompletableFuture<Void> sbkFuture = CompletableFuture.allOf(cfResults);
        sbkFuture.exceptionally(ex -> {
            shutdown(ex);
            return null;
        });

        sbkFuture.thenAccept(x -> {
            for (int i = 0; i < cfResults.length; i++) {
                try {
                    sbkResults[i] = cfResults[i].get();
                } catch (InterruptedException | ExecutionException e) {
                    throw new RuntimeException(e);
                }
            }
            fillSshResults(sbkResults);
            shutdown(null);
        });

        return retFuture.toCompletableFuture();
    }

    private boolean remoteDirectoryDelete() throws InterruptedException, ConnectException {
        final CompletableFuture<?>[] rmCfArray = new CompletableFuture[nodes.length];
        consMap.reset();
        for (int i = 0; i < nodes.length; i++) {
            if (consMap.isVisited(nodes[i].connection)) {
                rmCfArray[i] = new CompletableFuture<>();
                rmCfArray[i].complete(null);
            } else {
                consMap.visit(nodes[i].connection);
                rmCfArray[i] = nodes[i].runCommandAsync("rm -rf " + nodes[i].connection.getDir(),
                        true, config.remoteTimeoutSeconds );
            }
        }
        final CompletableFuture<Void> rmFuture = CompletableFuture.allOf(rmCfArray);

        for (int i = 0; i < config.maxIterations && !rmFuture.isDone(); i++) {
            try {
                rmFuture.get(config.timeoutSeconds, TimeUnit.SECONDS);
            } catch (TimeoutException | ExecutionException ex) {
                Printer.log.info("SBK-GEM [" + (i + 1) + "]: remote directory delete timeout");
            }
        }

        if (!rmFuture.isDone()) {
            final String errMsg = "SBK-GEM: command:  'rm -rf' time out after " + config.maxIterations +
                    " iterations";
            Printer.log.error(errMsg);
            throw new InterruptedException(errMsg);
        }
        return true;
    }

    @Synchronized
    @SuppressWarnings("unchecked")
    private void fillSshResults(SshResponse[] responseStreams) {
        final ConnectionConfig[] connections = params.getConnections();
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
     * @param ex Throwable exception
     */
    @Synchronized
    private void shutdown(Throwable ex) {
        if (state != State.END) {
            state = State.END;
            if (params.isDelete()) {
                try {
                    remoteDirectoryDelete();
                } catch (InterruptedException | ConnectException rmEx) {
                    rmEx.printStackTrace();
                }
            }
            for (SshSession node : nodes) {
                node.stop();
            }
            sbmBenchmark.stop();
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
        shutdown(null);
    }

    private final static class ConnectionsMap {
        private final Map<Map.Entry<String, String>, Boolean> kMap;

        public ConnectionsMap(@NotNull ConnectionConfig[] conn) {
            this.kMap = new HashMap<>();
            for (ConnectionConfig connectionConfig : conn) {
                this.kMap.put(Map.entry(connectionConfig.getHost().toLowerCase(), connectionConfig.getDir().toLowerCase()), false);
            }
        }

        void reset() {
            this.kMap.keySet().forEach(k -> this.kMap.put(k, false));
        }

        void visit(@NotNull ConnectionConfig conn) {
            this.kMap.put(Map.entry(conn.getHost().toLowerCase(), conn.getDir().toLowerCase()), true);
        }

        boolean isVisited(@NotNull ConnectionConfig conn) {
            return this.kMap.get(Map.entry(conn.getHost().toLowerCase(), conn.getDir().toLowerCase()));
        }
    }
}
