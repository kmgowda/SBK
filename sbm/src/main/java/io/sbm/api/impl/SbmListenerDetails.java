/**
 * Copyright (c) KMG. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 */
package io.sbm.api.impl;

import io.sbk.webconsole.LocalHttpLinks;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/** Formats the network addresses of the SBM gRPC performance-data listener. */
final class SbmListenerDetails {
    private static final String NOT_DETECTED = "not detected on local interfaces";

    private SbmListenerDetails() {
    }

    /**
     * Discover the listener addresses available on this host.
     *
     * <p>Public addresses are limited to addresses assigned to local network interfaces.
     * An address translated by an external NAT gateway cannot be discovered locally.</p>
     *
     * @param port actual bound gRPC port
     * @return complete listener details
     */
    static List<Detail> localDetails(int port) {
        return details(port, LocalHttpLinks.localHosts());
    }

    /**
     * Format supplied host addresses for deterministic verification.
     *
     * @param port actual bound gRPC port
     * @param hosts labeled local host addresses
     * @return complete listener details in display order
     */
    static List<Detail> details(int port, List<LocalHttpLinks.Host> hosts) {
        final Map<String, List<String>> endpoints = hosts.stream()
                .collect(Collectors.groupingBy(LocalHttpLinks.Host::label, LinkedHashMap::new,
                        Collectors.mapping(host -> endpoint(host.address(), port), Collectors.toList())));
        return List.of(
                detail(LocalHttpLinks.LOCALHOST_LABEL, endpoints),
                detail(LocalHttpLinks.IPV4_LOOPBACK_LABEL, endpoints),
                detail(LocalHttpLinks.HOSTNAME_LABEL, endpoints),
                detail(LocalHttpLinks.PRIVATE_IP_LABEL, endpoints),
                detail(LocalHttpLinks.PUBLIC_IP_LABEL, endpoints));
    }

    private static Detail detail(String label, Map<String, List<String>> endpoints) {
        return new Detail(label, String.join(", ", endpoints.getOrDefault(label, List.of(NOT_DETECTED))));
    }

    private static String endpoint(String address, int port) {
        return address + ":" + port;
    }

    /**
     * One labeled listener endpoint display value.
     *
     * @param label local address type
     * @param endpoint one or more endpoints, or an unavailable-address explanation
     */
    record Detail(String label, String endpoint) {
    }
}
