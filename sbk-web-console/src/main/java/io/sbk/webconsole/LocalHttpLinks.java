/**
 * Copyright (c) KMG. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.sbk.webconsole;

import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Discovers and labels local IPv4 hosts for HTTP endpoint links. */
public final class LocalHttpLinks {
    /** DNS name for the local host. */
    public static final String LOCALHOST = "localhost";
    /** Numeric IPv4 loopback address. */
    public static final String IPV4_LOOPBACK = "127.0.0.1";
    private static final String LOCALHOST_LABEL = "Localhost";
    private static final String IPV4_LOOPBACK_LABEL = "IPv4 Loopback";
    private static final String HOSTNAME_LABEL = "Hostname";
    private static final String PRIVATE_IP_LABEL = "Private IP";
    private static final String PUBLIC_IP_LABEL = "Public IP";

    private LocalHttpLinks() {
    }

    /**
     * Returns the locally discoverable, labeled IPv4 hosts.
     *
     * @return deduplicated hosts in display order
     */
    public static List<Host> localHosts() {
        return hosts(localHostname(), localAddresses());
    }

    /**
     * Returns labeled hosts from supplied discovery data.
     *
     * @param hostname local hostname
     * @param addresses local interface addresses
     * @return deduplicated hosts in display order
     */
    public static List<Host> hosts(String hostname, List<InetAddress> addresses) {
        final Map<String, String> hosts = new LinkedHashMap<>();
        hosts.put(LOCALHOST, LOCALHOST_LABEL);
        hosts.put(IPV4_LOOPBACK, IPV4_LOOPBACK_LABEL);
        if (hostname != null && !hostname.isBlank()) {
            hosts.putIfAbsent(hostname, HOSTNAME_LABEL);
        }
        addresses.stream()
                .filter(Inet4Address.class::isInstance)
                .filter(LocalHttpLinks::isUsableAddress)
                .sorted(Comparator.comparing((InetAddress address) -> !address.isSiteLocalAddress())
                        .thenComparing(InetAddress::getHostAddress))
                .forEach(address -> hosts.putIfAbsent(address.getHostAddress(),
                        address.isSiteLocalAddress() ? PRIVATE_IP_LABEL : PUBLIC_IP_LABEL));
        return hosts.entrySet().stream()
                .map(entry -> new Host(entry.getValue(), entry.getKey()))
                .toList();
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

    /**
     * A labeled host used to build a local HTTP link.
     *
     * @param label address type
     * @param address hostname or numeric IPv4 address
     */
    public record Host(String label, String address) {
    }
}
