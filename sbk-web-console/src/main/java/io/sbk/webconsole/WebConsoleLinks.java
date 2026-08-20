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

import java.net.InetAddress;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;

/** Discovers copy-paste URLs for one SBK Web Console benchmark run. */
final class WebConsoleLinks {
    private WebConsoleLinks() {
    }

    static List<WebConsoleClient.WebConsoleLink> localLinks(int port, String runId) {
        return createLinks(port, runId, LocalHttpLinks.localHosts());
    }

    static List<WebConsoleClient.WebConsoleLink> links(int port, String runId, String hostname,
            List<InetAddress> addresses) {
        return createLinks(port, runId, LocalHttpLinks.hosts(hostname, addresses));
    }

    private static List<WebConsoleClient.WebConsoleLink> createLinks(int port, String runId,
            List<LocalHttpLinks.Host> hosts) {
        final List<WebConsoleClient.WebConsoleLink> links = new ArrayList<>(hosts.size());
        hosts.forEach(host -> {
            try {
                links.add(new WebConsoleClient.WebConsoleLink(host.label(),
                        runUri(host.address(), port, runId)));
            } catch (IllegalArgumentException ex) {
                if (LocalHttpLinks.LOCALHOST.equals(host.address())
                        || LocalHttpLinks.IPV4_LOOPBACK.equals(host.address())) {
                    throw ex;
                }
            }
        });
        return List.copyOf(links);
    }

    private static URI runUri(String host, int port, String runId) {
        try {
            return new URI("http", null, host, port, "/", "run=" + runId, null);
        } catch (URISyntaxException ex) {
            throw new IllegalArgumentException("Invalid SBK Web Console address: " + host, ex);
        }
    }
}
