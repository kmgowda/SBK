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
import java.util.Arrays;
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
 * Coordinates remote SBK execution and local SBM lifecycle.
 *
 * <p>Responsibilities:
 * - Establish SSH sessions to all nodes and validate remote Java versions.
 * - Reconcile the expected SBK version on every remote host.
 * - Start SBM locally, then execute SBK remotely and collect results.
 * - Aggregate remote outputs into {@link io.gem.api.RemoteResponse}[] and shutdown cleanly.
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
            final String errMsg = "SBK-GEM: command: " + cmd + " time out after " + config.maxIterations +
                    " iterations! Check ssh user name and password or network connection";
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
        final boolean[] copyTargets = findCopyTargets(remoteDir);
        if (hasSelectedTarget(copyTargets)) {
            copySbkToRemoteTargets(copyTargets, remoteDir);
        } else {
            Printer.log.info("SBK-GEM: SBK version " + config.sbkVersion + " is already available on every host");
        }

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

    @SuppressWarnings("unchecked")
    private boolean[] findCopyTargets(String remoteDir) throws ConnectException, InterruptedException,
            ExecutionException {
        final boolean[] copyTargets = new boolean[nodes.length];
        if (params.isCopy()) {
            Arrays.fill(copyTargets, true);
            Printer.log.info("SBK-GEM: Force-copy requested; skipping remote SBK version checks");
            return copyTargets;
        }

        final CompletableFuture<SshResponse>[] probes = new CompletableFuture[nodes.length];
        for (int i = 0; i < nodes.length; i++) {
            final String remoteCommand = nodes[i].connection.getDir() + "/" + remoteDir + "/" +
                    params.getSbkCommand();
            probes[i] = nodes[i].runCommandAsync(RemoteSbkDeployment.versionProbeCommand(remoteCommand), true,
                    config.remoteTimeoutSeconds);
        }
        waitFor(CompletableFuture.allOf(probes), "remote SBK version checks");

        for (int i = 0; i < nodes.length; i++) {
            final SshResponse response = probes[i].get();
            copyTargets[i] = RemoteSbkDeployment.requiresCopy(false, response, config.sbkVersion);
            if (copyTargets[i]) {
                Printer.log.info("SBK-GEM: Host '" + nodes[i].connection.getHost() + "' is missing SBK version " +
                        config.sbkVersion + " or has a different version; scheduling copy");
            } else {
                Printer.log.info("SBK-GEM: Host '" + nodes[i].connection.getHost() + "' already has SBK version " +
                        config.sbkVersion + "; skipping copy");
            }
        }
        return copyTargets;
    }

    @SuppressWarnings("unchecked")
    private void copySbkToRemoteTargets(boolean[] copyTargets, String remoteDir) throws ConnectException,
            InterruptedException, ExecutionException {
        if (!remoteDirectoryDelete(copyTargets)) {
            final String errMsg = "SBK-GEM: Removing remote directory '" + remoteDir + "' failed";
            Printer.log.error(errMsg);
            throw new InterruptedException(errMsg);
        }
        Printer.log.info("SBK-GEM: Removing selected remote SBK directories Success..");

        final CompletableFuture<SshResponse>[] mkdirFutures = new CompletableFuture[nodes.length];
        consMap.reset();
        for (int i = 0; i < nodes.length; i++) {
            if (!copyTargets[i] || consMap.isVisited(nodes[i].connection)) {
                mkdirFutures[i] = CompletableFuture.completedFuture(new SshResponse(true));
            } else {
                consMap.visit(nodes[i].connection);
                final String command = "mkdir -p " + RemoteSbkDeployment.shellQuote(nodes[i].connection.getDir());
                mkdirFutures[i] = nodes[i].runCommandAsync(command, true, config.remoteTimeoutSeconds);
            }
        }
        waitFor(CompletableFuture.allOf(mkdirFutures), "remote directory creation");
        for (CompletableFuture<SshResponse> mkdirFuture : mkdirFutures) {
            if (mkdirFuture.get().returnCode != 0) {
                throw new InterruptedException("SBK-GEM: Creating a remote SBK directory failed");
            }
        }
        Printer.log.info("SBK-GEM: Creating selected remote directories Success..");

        final CompletableFuture<?>[] copyFutures = new CompletableFuture[nodes.length];
        consMap.reset();
        for (int i = 0; i < nodes.length; i++) {
            if (!copyTargets[i] || consMap.isVisited(nodes[i].connection)) {
                copyFutures[i] = CompletableFuture.completedFuture(null);
            } else {
                consMap.visit(nodes[i].connection);
                copyFutures[i] = nodes[i].copyDirectoryAsync(params.getSbkDir(), nodes[i].connection.getDir());
            }
        }
        waitFor(CompletableFuture.allOf(copyFutures), "SBK copy");
        Printer.log.info("SBK-GEM: Copying SBK application to selected remote hosts Success..");
    }

    private void waitFor(CompletableFuture<?> future, String operation) throws InterruptedException,
            ExecutionException {
        for (int i = 0; i < config.maxIterations && !future.isDone(); i++) {
            try {
                future.get(config.timeoutSeconds, TimeUnit.SECONDS);
            } catch (TimeoutException ex) {
                Printer.log.info("SBK-GEM [" + (i + 1) + "]: Waiting for " + operation + " timeout");
            }
        }
        if (!future.isDone()) {
            final String errMsg = "SBK-GEM: " + operation + " timed out after " + config.maxIterations +
                    " iterations";
            Printer.log.error(errMsg);
            throw new InterruptedException(errMsg);
        }
        future.get();
    }

    private static boolean hasSelectedTarget(boolean[] selected) {
        for (boolean value : selected) {
            if (value) {
                return true;
            }
        }
        return false;
    }

    @SuppressWarnings("unchecked")
    private boolean remoteDirectoryDelete(boolean[] deleteTargets) throws InterruptedException, ConnectException,
            ExecutionException {
        final CompletableFuture<SshResponse>[] rmCfArray = new CompletableFuture[nodes.length];
        consMap.reset();
        for (int i = 0; i < nodes.length; i++) {
            if (!deleteTargets[i] || consMap.isVisited(nodes[i].connection)) {
                rmCfArray[i] = CompletableFuture.completedFuture(new SshResponse(true));
            } else {
                consMap.visit(nodes[i].connection);
                rmCfArray[i] = nodes[i].runCommandAsync("rm -rf " +
                                RemoteSbkDeployment.shellQuote(nodes[i].connection.getDir()),
                        true, config.remoteTimeoutSeconds );
            }
        }
        final CompletableFuture<Void> rmFuture = CompletableFuture.allOf(rmCfArray);
        waitFor(rmFuture, "remote directory deletion");
        for (CompletableFuture<SshResponse> rmResult : rmCfArray) {
            if (rmResult.get().returnCode != 0) {
                return false;
            }
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
                    final boolean[] deleteTargets = new boolean[nodes.length];
                    Arrays.fill(deleteTargets, true);
                    remoteDirectoryDelete(deleteTargets);
                } catch (InterruptedException | ConnectException | ExecutionException rmEx) {
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

    /**
     * Tracks visited (host, remoteDir) combinations to avoid duplicate operations
     * when multiple connections point to the same remote target.
     */
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
