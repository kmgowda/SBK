/**
 * Copyright (c) KMG. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 */
package io.perl.logger.impl;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Verifies ownership and shutdown of the embedded Prometheus HTTP executor. */
final class PrometheusServerTest {

    @Test
    void stopShutsDownTheOwnedExecutor() throws IOException {
        final PrometheusServer server = new PrometheusServer(0, "/metrics", List.of());
        assertFalse(server.isExecutorShutdown());

        server.start();
        server.stop();

        assertTrue(server.isExecutorShutdown());
    }
}
