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

import org.apache.sshd.client.future.ConnectFuture;
import org.apache.sshd.client.session.ClientSession;

import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.atomic.AtomicBoolean;

/** Cancels one node's in-flight SSH connection without stopping a shared Apache SSHD client. */
final class SshConnectionAttempt {
    private final AtomicBoolean canceled = new AtomicBoolean();
    private final AtomicReference<ConnectFuture> connectFuture = new AtomicReference<>();
    private final AtomicReference<ClientSession> session = new AtomicReference<>();

    void connecting(ConnectFuture future) {
        connectFuture.set(future);
        if (canceled.get() && connectFuture.compareAndSet(future, null)) {
            future.cancel();
        }
    }

    void connected(ClientSession connectedSession) {
        session.set(connectedSession);
        connectFuture.set(null);
        if (canceled.get() && session.compareAndSet(connectedSession, null)) {
            connectedSession.close(true);
        }
    }

    void complete() {
        connectFuture.set(null);
    }

    void cancel() {
        canceled.set(true);
        final ConnectFuture activeConnect = connectFuture.getAndSet(null);
        if (activeConnect != null) {
            activeConnect.cancel();
        }
        final ClientSession activeSession = session.getAndSet(null);
        if (activeSession != null) {
            activeSession.close(true);
        }
    }
}
