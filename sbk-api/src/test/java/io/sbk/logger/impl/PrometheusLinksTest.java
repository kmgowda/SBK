/**
 * Copyright (c) KMG. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 */
package io.sbk.logger.impl;

import org.junit.jupiter.api.Test;

import java.net.InetAddress;
import java.net.URI;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

/** Verifies deterministic Prometheus endpoint discovery and address classification. */
final class PrometheusLinksTest {

    @Test
    void createsLocalHostnamePrivateAndPublicLinks() throws Exception {
        final List<PrometheusLinks.Link> links = PrometheusLinks.links(9718, "/metrics", "sbk-host",
                List.of(InetAddress.getByName("127.0.0.1"), InetAddress.getByName("10.20.30.40"),
                        InetAddress.getByName("8.8.8.8"), InetAddress.getByName("10.20.30.40")));

        assertEquals(List.of(
                new PrometheusLinks.Link("Localhost", uri("localhost")),
                new PrometheusLinks.Link("IPv4 Loopback", uri("127.0.0.1")),
                new PrometheusLinks.Link("Hostname", uri("sbk-host")),
                new PrometheusLinks.Link("Private IP", uri("10.20.30.40")),
                new PrometheusLinks.Link("Public IP", uri("8.8.8.8"))), links);
    }

    private static URI uri(String host) {
        return URI.create("http://" + host + ":9718/metrics");
    }
}
