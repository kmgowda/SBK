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

/** Discovers copy-paste URLs for one SBK Web Console benchmark run. */
final class WebConsoleLinks {
    private static final String LOCALHOST = "localhost";
    private static final String IPV4_LOOPBACK = "127.0.0.1";

    private WebConsoleLinks() {
    }

    static List<WebConsoleClient.WebConsoleLink> localLinks(int port, String runId) {
        return links(port, runId, localHostname(), localAddresses());
    }

    static List<WebConsoleClient.WebConsoleLink> links(int port, String runId, String hostname,
            List<InetAddress> addresses) {
        final Map<String, String> hosts = new LinkedHashMap<>();
        hosts.put(LOCALHOST, "Localhost");
        hosts.put(IPV4_LOOPBACK, "IPv4 Loopback");
        if (hostname != null && !hostname.isBlank()) {
            hosts.putIfAbsent(hostname, "Hostname");
        }
        addresses.stream()
                .filter(Inet4Address.class::isInstance)
                .filter(WebConsoleLinks::isUsableAddress)
                .sorted(Comparator.comparing((InetAddress address) -> !address.isSiteLocalAddress())
                        .thenComparing(InetAddress::getHostAddress))
                .forEach(address -> hosts.putIfAbsent(address.getHostAddress(),
                        address.isSiteLocalAddress() ? "Private IP" : "Public IP"));

        final List<WebConsoleClient.WebConsoleLink> links = new ArrayList<>(hosts.size());
        hosts.forEach((host, label) -> {
            try {
                links.add(new WebConsoleClient.WebConsoleLink(label, runUri(host, port, runId)));
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

    private static URI runUri(String host, int port, String runId) {
        try {
            return new URI("http", null, host, port, "/", "run=" + runId, null);
        } catch (URISyntaxException ex) {
            throw new IllegalArgumentException("Invalid SBK Web Console address: " + host, ex);
        }
    }
}
