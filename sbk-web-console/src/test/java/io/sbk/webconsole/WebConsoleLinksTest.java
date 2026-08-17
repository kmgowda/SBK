/**
 * Copyright (c) KMG. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 */
package io.sbk.webconsole;

import org.junit.jupiter.api.Test;

import java.net.InetAddress;
import java.net.URI;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

/** Verifies deterministic Web Console run-link discovery and address classification. */
final class WebConsoleLinksTest {

    @Test
    void createsLocalhostHostnamePrivateAndPublicLinks() throws Exception {
        final List<WebConsoleClient.WebConsoleLink> links = WebConsoleLinks.links(9720, "test-run", "sbk-host",
                List.of(InetAddress.getByName("127.0.0.1"), InetAddress.getByName("10.20.30.40"),
                        InetAddress.getByName("8.8.8.8"), InetAddress.getByName("10.20.30.40")));

        assertEquals(List.of(
                new WebConsoleClient.WebConsoleLink("Localhost", uri("localhost")),
                new WebConsoleClient.WebConsoleLink("IPv4 Loopback", uri("127.0.0.1")),
                new WebConsoleClient.WebConsoleLink("Hostname", uri("sbk-host")),
                new WebConsoleClient.WebConsoleLink("Private IP", uri("10.20.30.40")),
                new WebConsoleClient.WebConsoleLink("Public IP", uri("8.8.8.8"))), links);
    }

    @Test
    void skipsInvalidHostnameWithoutDiscardingValidLinks() {
        final List<WebConsoleClient.WebConsoleLink> links = WebConsoleLinks.links(
                9720, "test-run", "invalid_host", List.of());

        assertEquals(List.of(
                new WebConsoleClient.WebConsoleLink("Localhost", uri("localhost")),
                new WebConsoleClient.WebConsoleLink("IPv4 Loopback", uri("127.0.0.1"))), links);
    }

    private static URI uri(String host) {
        return URI.create("http://" + host + ":9720/?run=test-run");
    }
}
