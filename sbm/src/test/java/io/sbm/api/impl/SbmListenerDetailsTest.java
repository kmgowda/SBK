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
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

/** Verifies complete and non-redundant SBM listener endpoint reporting. */
final class SbmListenerDetailsTest {
    @Test
    void reportsAllDetectedAddressTypesWithTheListeningPort() {
        final List<SbmListenerDetails.Detail> details = SbmListenerDetails.details(9717, List.of(
                new LocalHttpLinks.Host(LocalHttpLinks.LOCALHOST_LABEL, "localhost"),
                new LocalHttpLinks.Host(LocalHttpLinks.IPV4_LOOPBACK_LABEL, "127.0.0.1"),
                new LocalHttpLinks.Host(LocalHttpLinks.HOSTNAME_LABEL, "sbm-host"),
                new LocalHttpLinks.Host(LocalHttpLinks.PRIVATE_IP_LABEL, "10.20.30.40"),
                new LocalHttpLinks.Host(LocalHttpLinks.PRIVATE_IP_LABEL, "192.168.1.20"),
                new LocalHttpLinks.Host(LocalHttpLinks.PUBLIC_IP_LABEL, "203.0.113.10")));

        assertEquals(List.of(
                new SbmListenerDetails.Detail("Localhost", "localhost:9717"),
                new SbmListenerDetails.Detail("IPv4 Loopback", "127.0.0.1:9717"),
                new SbmListenerDetails.Detail("Hostname", "sbm-host:9717"),
                new SbmListenerDetails.Detail("Private IP", "10.20.30.40:9717, 192.168.1.20:9717"),
                new SbmListenerDetails.Detail("Public IP", "203.0.113.10:9717")), details);
    }

    @Test
    void identifiesAddressTypesThatAreNotLocallyAvailable() {
        final List<SbmListenerDetails.Detail> details = SbmListenerDetails.details(8123, List.of(
                new LocalHttpLinks.Host(LocalHttpLinks.LOCALHOST_LABEL, "localhost"),
                new LocalHttpLinks.Host(LocalHttpLinks.IPV4_LOOPBACK_LABEL, "127.0.0.1")));

        assertEquals(List.of(
                new SbmListenerDetails.Detail("Localhost", "localhost:8123"),
                new SbmListenerDetails.Detail("IPv4 Loopback", "127.0.0.1:8123"),
                new SbmListenerDetails.Detail("Hostname", "not detected on local interfaces"),
                new SbmListenerDetails.Detail("Private IP", "not detected on local interfaces"),
                new SbmListenerDetails.Detail("Public IP", "not detected on local interfaces")), details);
    }
}
