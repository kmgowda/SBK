/**
 * Copyright (c) KMG. All Rights Reserved..
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.gem.api;

import io.sbk.system.Printer;
import org.apache.sshd.client.SshClient;
import org.apache.sshd.client.session.ClientSession;
import org.apache.sshd.sftp.client.SftpClientFactory;
import org.apache.sshd.sftp.client.fs.SftpFileSystem;

import javax.annotation.concurrent.GuardedBy;
import java.io.IOException;
import java.net.ConnectException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.net.SocketTimeoutException;
import java.nio.file.FileSystem;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Lifecycle wrapper around an SSH client/session for a single connection.
 *
 * <p>Encapsulates connect, command execution, SFTP file copy, and graceful shutdown,
 * exposing async methods returning {@link CompletableFuture}s and routing connection/control,
 * transfer, and long-running command work to separate execution resources. Session state is protected by a short-held
 * lifecycle lock; network and file operations never execute while holding it.
 */
final public class SshSession {

    /**
     * A bounded operation performed against the remote Apache MINA SFTP file system.
     *
     * @param <T> operation result type
     */
    @FunctionalInterface
    public interface RemoteFileOperation<T> {
        /**
         * Execute the operation.
         *
         * @param fileSystem remote SFTP file system
         * @return operation result
         * @throws IOException on remote file-system failure
         * @throws InterruptedException when lifecycle coordination is interrupted
         */
        T execute(FileSystem fileSystem) throws IOException, InterruptedException;
    }

    /**
     * <code>public SshConnection connection</code>.
     */
    final public ConnectionConfig connection;

    /**
     * <code>private SshClient client</code>.
     */
    final private SshClient client;

    /** Bounded executor for SSH connection and control-plane operations. */
    final private ExecutorService controlExecutor;

    /** Bounded executor for SFTP copies and other deployment data movement. */
    final private ExecutorService transferExecutor;

    /** Virtual-thread executor for commands that remain active for a complete benchmark. */
    final private ExecutorService commandExecutor;

    /** Maximum stdout/stderr bytes retained for each command. */
    final private int diagnosticBytes;

    /** Short-held lock protecting session publication and shutdown. */
    final private Object sessionLock;

    /** Operations cancelled when this session is stopped. */
    final private Set<BoundedTask<?>> activeTasks;


    /**
     * <code>ClientSession session</code>.
     */
    @GuardedBy("sessionLock")
    private ClientSession session;

    /** Whether this one-shot session wrapper has been stopped. */
    @GuardedBy("sessionLock")
    private boolean stopped;

    /**
     * This Constructor initializes all values.
     *
     * @param conn      SshConnection
     * @param executor  ExecutorService
     */
    public SshSession(ConnectionConfig conn, ExecutorService executor) {
        this(conn, executor, SshResponse.DEFAULT_DIAGNOSTIC_BYTES);
    }

    /**
     * This constructor initializes all values with an explicit diagnostic limit.
     *
     * @param conn            SSH connection
     * @param executor        orchestration executor
     * @param diagnosticBytes maximum stdout/stderr bytes retained per command
     */
    public SshSession(ConnectionConfig conn, ExecutorService executor, int diagnosticBytes) {
        this(conn, executor, executor, executor, diagnosticBytes);
    }

    /**
     * This constructor assigns separate execution resources to control, transfer, and long-running commands.
     *
     * @param conn             SSH connection
     * @param controlExecutor  bounded connection and control-operation executor
     * @param transferExecutor bounded deployment transfer executor
     * @param commandExecutor  virtual-thread executor for long-running remote commands
     * @param diagnosticBytes  maximum stdout/stderr bytes retained per command
     */
    public SshSession(ConnectionConfig conn, ExecutorService controlExecutor,
                      ExecutorService transferExecutor, ExecutorService commandExecutor,
                      int diagnosticBytes) {
        this.connection = conn;
        this.controlExecutor = controlExecutor;
        this.transferExecutor = transferExecutor;
        this.commandExecutor = commandExecutor;
        this.diagnosticBytes = diagnosticBytes;
        this.sessionLock = new Object();
        this.activeTasks = ConcurrentHashMap.newKeySet();
        this.client = SshUtils.createClient(conn);
        if (!conn.isHostKeyCheck()) {
            Printer.log.warn("SBK-GEM: SSH host-key verification is disabled for host '" + conn.getHost() + "'");
        }
    }

    private void createSession(long timeoutSeconds) throws IOException {
        Printer.log.info("SBK-GEM: Ssh Connection to host '" + connection.getHost() + "' starting...");
        ClientSession createdSession = null;
        try {
            synchronized (sessionLock) {
                if (stopped) {
                    throw new IOException("SBK-GEM: SSH session was stopped before connection to '"
                            + connection.getHost() + "'");
                }
            }
            client.start();
            createdSession = SshUtils.createSession(client, connection, timeoutSeconds);
            synchronized (sessionLock) {
                if (stopped) {
                    createdSession.close(true);
                    throw new IOException("SBK-GEM: SSH session was stopped while connecting to '"
                            + connection.getHost() + "'");
                }
                session = createdSession;
            }
            Printer.log.info("SBK-GEM: Authenticated ssh session to '" + connection.getUserName() + "@" +
                    connection.getHost() + ":" + connection.getPort() + "' established successfully.");
        } catch (IOException e) {
            synchronized (sessionLock) {
                if (session == createdSession) {
                    session = null;
                }
            }
            final String password = connection.getPassword();
            final boolean authenticationFailure = e.getMessage() != null &&
                    e.getMessage().startsWith("SSH authentication failed:");
            final boolean hostKeyFailure = e.getMessage() != null &&
                    e.getMessage().startsWith("SSH host key verification failed:");
            final String failureHint;
            if (hostKeyFailure) {
                failureHint = " Verify or replace the host entry in " +
                        (connection.getKnownHosts() == null || connection.getKnownHosts().isEmpty()
                                ? "~/.ssh/known_hosts."
                                : connection.getKnownHosts() + ".");
            } else if (authenticationFailure && (password == null || password.isEmpty())) {
                failureHint = " No password was supplied; configure -gempass, SBK_GEM_SSH_PASSWD, " +
                        "or SSH public-key authentication.";
            } else if (authenticationFailure) {
                failureHint = " Verify the configured SSH password or public-key authentication.";
            } else {
                failureHint = " Verify the remote host, SSH port, network connectivity, and known_hosts entry.";
            }
            final String error = "SBK-GEM: SSH connection or authentication failed for '" +
                    connection.getUserName() + "@" + connection.getHost() + ":" + connection.getPort() + "': " +
                    e.getMessage() + "." + failureHint;
            Printer.log.error(error);
            throw new IOException(error, e);
        }
    }


    /**
     * This method Creates Sessions.
     *
     * @param timeoutSeconds long
     * @return CompletableFuture
     */
    public CompletableFuture<Void> createSessionAsync(long timeoutSeconds) {
        return submitBounded(controlExecutor, () -> {
            createSession(timeoutSeconds);
            return null;
        }, () -> client.stop(), timeoutSeconds);
    }

    private ClientSession getSession() throws ConnectException {
        synchronized (sessionLock) {
            if (session == null || stopped) {
                String errMgs = "ssh session to host: " + connection.getHost() + " not found!";
                throw new ConnectException(errMgs);
            }
            return session;
        }
    }

    /**
     * Return the authenticated network endpoint used by this SSH session.
     *
     * <p>The numeric address lets orchestration collapse aliases such as {@code localhost}
     * and {@code 127.0.0.1} before performing physical deployment work. The configured
     * host name remains available through {@link #connection} for user-facing diagnostics.
     *
     * @return normalized numeric address, or the normalized endpoint text when unavailable
     * @throws ConnectException when the SSH session is unavailable
     */
    public String getRemoteEndpointIdentity() throws ConnectException {
        final SocketAddress connectAddress = getSession().getConnectAddress();
        if (connectAddress instanceof InetSocketAddress inetAddress) {
            if (inetAddress.getAddress() != null) {
                return inetAddress.getAddress().getHostAddress().toLowerCase(Locale.ROOT);
            }
            return inetAddress.getHostString().toLowerCase(Locale.ROOT);
        }
        return connectAddress.toString().toLowerCase(Locale.ROOT);
    }


    /**
     * This method is responsible for running commands but throws ConnectException if it occurs.
     *
     * @param cmd            String
     * @param isOutput       Is stdout output is required
     * @param timeoutSeconds long
     * @return CompletableFuture
     * @throws ConnectException If connection exception occurs.
     */
    public CompletableFuture<SshResponse> runCommandAsync(String cmd, Boolean isOutput, long timeoutSeconds)
            throws ConnectException {
        return runCommandAsync(cmd, new byte[0], isOutput, timeoutSeconds);
    }

    /**
     * Run a remote command with a binary stdin request.
     *
     * @param cmd command
     * @param input command standard input
     * @param isOutput whether output should be retained
     * @param timeoutSeconds command timeout
     * @return asynchronous response
     * @throws ConnectException when the SSH session is unavailable
     */
    public CompletableFuture<SshResponse> runCommandAsync(String cmd, byte[] input, Boolean isOutput,
                                                           long timeoutSeconds) throws ConnectException {
        return executeCommandAsync(controlExecutor, cmd, input, isOutput, timeoutSeconds);
    }

    /**
     * Run a remote command that remains active for the duration of a benchmark on a virtual thread.
     *
     * @param cmd command
     * @param input command standard input
     * @param isOutput whether output should be retained
     * @param timeoutSeconds command timeout
     * @return asynchronous response
     * @throws ConnectException when the SSH session is unavailable
     */
    public CompletableFuture<SshResponse> runBenchmarkCommandAsync(String cmd, byte[] input, Boolean isOutput,
                                                                    long timeoutSeconds) throws ConnectException {
        return executeCommandAsync(commandExecutor, cmd, input, isOutput, timeoutSeconds);
    }

    private CompletableFuture<SshResponse> executeCommandAsync(ExecutorService operationExecutor, String cmd,
                                                                byte[] input, Boolean isOutput,
                                                                long timeoutSeconds) throws ConnectException {
        final ClientSession sshSession = getSession();
        return submitBounded(operationExecutor, () -> {
            final SshResponse response = new SshResponse(isOutput, diagnosticBytes);
            try {
                SshUtils.runCommand(sshSession, cmd, input, timeoutSeconds, response);
            } catch (IOException e) {
                throw new SshCommandException(connection.getHost(), response, hasTimeoutCause(e), e);
            }
            return response;
        }, () -> { }, saturatedIncrement(timeoutSeconds));
    }

    private static boolean hasTimeoutCause(Throwable failure) {
        Throwable cause = failure;
        while (cause != null) {
            if (cause instanceof SocketTimeoutException) {
                return true;
            }
            cause = cause.getCause();
        }
        return false;
    }

    /**
     * Copy one local file to an exact remote path asynchronously.
     *
     * @param srcPath local source file
     * @param dstPath remote destination file
     * @param timeoutSeconds maximum copy duration in seconds
     * @return copy completion
     * @throws ConnectException when no SSH session is available
     */
    public CompletableFuture<Void> copyFileAsync(String srcPath, String dstPath, long timeoutSeconds)
            throws ConnectException {
        return runRemoteTransferOperationAsync(fileSystem -> {
            final Path destination = fileSystem.getPath(dstPath);
            Files.copy(Path.of(srcPath), destination, StandardCopyOption.REPLACE_EXISTING);
            return null;
        }, timeoutSeconds);
    }

    /**
     * Execute a remote file-system operation through Apache MINA SFTP.
     *
     * @param operation remote file-system operation
     * @param timeoutSeconds maximum operation duration
     * @param <T> result type
     * @return asynchronous operation result
     * @throws ConnectException when no SSH session is available
     */
    public <T> CompletableFuture<T> runRemoteFileOperationAsync(RemoteFileOperation<T> operation,
                                                                 long timeoutSeconds)
            throws ConnectException {
        return executeRemoteFileOperationAsync(controlExecutor, operation, timeoutSeconds);
    }

    /**
     * Execute deployment data movement through Apache MINA SFTP on the bounded transfer executor.
     *
     * @param operation remote file-system transfer operation
     * @param timeoutSeconds maximum operation duration
     * @param <T> result type
     * @return asynchronous operation result
     * @throws ConnectException when no SSH session is available
     */
    public <T> CompletableFuture<T> runRemoteTransferOperationAsync(RemoteFileOperation<T> operation,
                                                                     long timeoutSeconds)
            throws ConnectException {
        return executeRemoteFileOperationAsync(transferExecutor, operation, timeoutSeconds);
    }

    private <T> CompletableFuture<T> executeRemoteFileOperationAsync(ExecutorService operationExecutor,
                                                                      RemoteFileOperation<T> operation,
                                                                      long timeoutSeconds)
            throws ConnectException {
        final ClientSession sshSession = getSession();
        final AtomicReference<SftpFileSystem> activeFileSystem = new AtomicReference<>();
        return submitBounded(operationExecutor, () -> {
            try (SftpFileSystem fileSystem = SftpClientFactory.instance().createSftpFileSystem(sshSession)) {
                activeFileSystem.set(fileSystem);
                return operation.execute(fileSystem);
            } catch (IOException exception) {
                final String message = "SBK-GEM: Apache MINA SFTP operation failed on host '"
                        + connection.getHost() + ":" + connection.getPort() + "': " + exception.getMessage();
                throw new IOException(message, exception);
            } finally {
                activeFileSystem.set(null);
            }
        }, () -> closeQuietly(activeFileSystem.getAndSet(null)), timeoutSeconds);
    }

    private <T> CompletableFuture<T> submitBounded(ExecutorService operationExecutor,
                                                    Callable<T> operation, Runnable cancelAction,
                                                    long timeoutSeconds) {
        final CompletableFuture<T> completion = new CompletableFuture<>();
        final BoundedTask<T> task = new BoundedTask<>(operation, cancelAction, completion, timeoutSeconds);
        activeTasks.add(task);
        completion.whenComplete((ignored, failure) -> {
            final Throwable cause = unwrap(failure);
            if (cause instanceof TimeoutException || completion.isCancelled()) {
                task.cancel(true);
            }
        });
        try {
            operationExecutor.execute(task);
        } catch (RuntimeException exception) {
            activeTasks.remove(task);
            completion.completeExceptionally(exception);
            return completion;
        }
        return completion;
    }

    private static long saturatedIncrement(long value) {
        return value == Long.MAX_VALUE ? value : value + 1;
    }

    private static Throwable unwrap(Throwable failure) {
        Throwable cause = failure;
        while (cause instanceof CompletionException && cause.getCause() != null) {
            cause = cause.getCause();
        }
        return cause;
    }

    private static void closeQuietly(AutoCloseable resource) {
        if (resource != null) {
            try {
                resource.close();
            } catch (Exception ignored) {
                // Cancellation already owns the primary timeout or shutdown failure.
            }
        }
    }

    /** Cancel all currently running operations without permanently closing this session. */
    public void cancelActiveOperations() {
        for (BoundedTask<?> task : activeTasks.toArray(BoundedTask[]::new)) {
            task.cancel(true);
        }
    }

    private void closeSession() {
        final ClientSession activeSession;
        synchronized (sessionLock) {
            stopped = true;
            activeSession = session;
            session = null;
        }
        cancelActiveOperations();
        if (activeSession != null) {
            activeSession.close(true);
        }
    }


    /**
     * This method is responsible for closing session and stopping the client.
     */
    public void stop() {
        closeSession();
        client.stop();
    }

    private final class BoundedTask<T> extends FutureTask<Void> {
        private final Runnable cancelAction;
        private final CompletableFuture<T> completion;

        BoundedTask(Callable<T> operation, Runnable cancelAction, CompletableFuture<T> completion,
                    long timeoutSeconds) {
            super(() -> {
                completion.orTimeout(timeoutSeconds, TimeUnit.SECONDS);
                try {
                    completion.complete(operation.call());
                } catch (InterruptedException exception) {
                    Thread.currentThread().interrupt();
                    completion.completeExceptionally(exception);
                } catch (Throwable exception) {
                    completion.completeExceptionally(exception);
                }
                return null;
            });
            this.cancelAction = cancelAction;
            this.completion = completion;
        }

        @Override
        public boolean cancel(boolean mayInterruptIfRunning) {
            final boolean cancelled = super.cancel(mayInterruptIfRunning);
            if (cancelled) {
                completion.cancel(false);
                cancelAction.run();
            }
            return cancelled;
        }

        @Override
        protected void done() {
            activeTasks.remove(this);
        }
    }

}
