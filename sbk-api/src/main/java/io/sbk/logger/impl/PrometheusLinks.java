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

import io.sbk.logger.MetricsConfig;
import io.sbk.system.Printer;
import io.sbk.webconsole.LocalHttpLinks;

import java.net.InetAddress;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;

/** Builds and prints copy-paste URLs for the embedded Prometheus scrape endpoint. */
public final class PrometheusLinks {
    private PrometheusLinks() {
    }

    /**
     * Prints all locally discoverable URLs for a running Prometheus endpoint.
     *
     * @param component exporter name, such as {@code SBK} or {@code SBM}
     * @param config active Prometheus endpoint configuration
     */
    public static void log(String component, MetricsConfig config) {
        try {
            localLinks(config.port, config.context).forEach(link ->
                    Printer.log.info("{} Prometheus Metrics ({}): {}", component, link.label(), link.uri()));
        } catch (RuntimeException ex) {
            Printer.log.warn("{} Prometheus Metrics: unable to discover host scrape URLs: {}",
                    component, ex.getMessage());
        }
    }

    /**
     * Returns all locally discoverable URLs for a Prometheus endpoint.
     *
     * @param port HTTP port
     * @param context Prometheus scrape path
     * @return deduplicated endpoint links
     */
    public static List<Link> localLinks(int port, String context) {
        return createLinks(port, context, LocalHttpLinks.localHosts());
    }

    static List<Link> links(int port, String context, String hostname, List<InetAddress> addresses) {
        return createLinks(port, context, LocalHttpLinks.hosts(hostname, addresses));
    }

    private static List<Link> createLinks(int port, String context, List<LocalHttpLinks.Host> hosts) {
        final List<Link> links = new ArrayList<>(hosts.size());
        hosts.forEach(host -> {
            try {
                links.add(new Link(host.label(), endpointUri(host.address(), port, context)));
            } catch (IllegalArgumentException ex) {
                if (LocalHttpLinks.LOCALHOST.equals(host.address())
                        || LocalHttpLinks.IPV4_LOOPBACK.equals(host.address())) {
                    throw ex;
                }
            }
        });
        return List.copyOf(links);
    }

    private static URI endpointUri(String host, int port, String context) {
        try {
            return new URI("http", null, host, port, context, null, null);
        } catch (URISyntaxException ex) {
            throw new IllegalArgumentException("Invalid Prometheus endpoint address: " + host, ex);
        }
    }

    /**
     * A labeled, copy-paste URL for one Prometheus endpoint address.
     *
     * @param label address type
     * @param uri complete scrape URI
     */
    public record Link(String label, URI uri) {
    }
}
