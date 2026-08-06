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

import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Builds and prints copy-paste URLs for the embedded Prometheus scrape endpoint. */
public final class PrometheusLinks {
    private static final String LOCALHOST = "localhost";
    private static final String IPV4_LOOPBACK = "127.0.0.1";

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
        return links(port, context, localHostname(), localAddresses());
    }

    static List<Link> links(int port, String context, String hostname, List<InetAddress> addresses) {
        final Map<String, String> hosts = new LinkedHashMap<>();
        hosts.put(LOCALHOST, "Localhost");
        hosts.put(IPV4_LOOPBACK, "IPv4 Loopback");
        if (hostname != null && !hostname.isBlank()) {
            hosts.putIfAbsent(hostname, "Hostname");
        }
        addresses.stream()
                .filter(Inet4Address.class::isInstance)
                .filter(PrometheusLinks::isUsableAddress)
                .sorted(Comparator.comparing((InetAddress address) -> !address.isSiteLocalAddress())
                        .thenComparing(InetAddress::getHostAddress))
                .forEach(address -> hosts.putIfAbsent(address.getHostAddress(),
                        address.isSiteLocalAddress() ? "Private IP" : "Public IP"));

        final List<Link> links = new ArrayList<>(hosts.size());
        hosts.forEach((host, label) -> {
            try {
                links.add(new Link(label, endpointUri(host, port, context)));
            } catch (IllegalArgumentException ex) {
                if (LOCALHOST.equals(host) || IPV4_LOOPBACK.equals(host)) {
                    throw ex;
                }
            }
        });
        return List.copyOf(links);
    }

    private static boolean isUsableAddress(InetAddress address) {
        return !address.isAnyLocalAddress() && !address.isLoopbackAddress()
                && !address.isLinkLocalAddress() && !address.isMulticastAddress();
    }

    private static String localHostname() {
        try {
            return InetAddress.getLocalHost().getHostName();
        } catch (UnknownHostException ex) {
            return "";
        }
    }

    private static List<InetAddress> localAddresses() {
        final List<InetAddress> addresses = new ArrayList<>();
        try {
            final Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
            if (interfaces == null) {
                return addresses;
            }
            while (interfaces.hasMoreElements()) {
                final NetworkInterface networkInterface = interfaces.nextElement();
                if (!networkInterface.isUp()) {
                    continue;
                }
                final Enumeration<InetAddress> interfaceAddresses = networkInterface.getInetAddresses();
                while (interfaceAddresses.hasMoreElements()) {
                    addresses.add(interfaceAddresses.nextElement());
                }
            }
        } catch (SocketException ex) {
            return List.of();
        }
        return addresses;
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
