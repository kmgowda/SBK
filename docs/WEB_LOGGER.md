<!--
Copyright (c) KMG. All Rights Reserved.

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
-->

# WebLogger and the SBK Local Web Console

WebLogger displays SBK measurements in the **SBK Local Web Console** without Docker, Prometheus, or Grafana. The
local component is intentionally named differently from the separately deployable **SBK Dashboard** project. It uses the same
periodic interval measurements delivered through `print(...)`, while `printTotal(...)` writes cumulative final totals
only to the console. Enabling WebLogger therefore does not add measurement sampling or storage-driver work. The web
console server runs as the `WebConsoleMain` process from the independent `sbk-web-console` module and is
implemented with the JDK HTTP server,
retains a bounded in-memory history, and sends
new summaries to browsers with server-sent events (SSE).
The browser also synchronizes bounded history every two seconds, so graphs recover automatically if an SSE stream
is delayed, interrupted, or unavailable through an HTTP intermediary.

Use the logger matching the application:

| Application | Logger | Displayed result |
|---|---|---|
| `sbk` or `sbk-yal` | `WebLogger` | One local SBK workload |
| `sbm` | `SbmWebLogger` | Aggregated results received from distributed SBK clients |
| `sbk-gem` or `sbk-gem-yal` | `GemWebLogger` | Cluster aggregate produced by GEM's embedded SBM |

The Local Web Console URL is <http://127.0.0.1:9720>. The server listens on `0.0.0.0` by default and prints separate
copy-paste run links for loopback, the machine hostname, and its available public/private IPv4 addresses. A browser
on another system can use one of those hostname or IP links when network and firewall policy allow the connection.
The default transport is unsecured HTTP; WebLogger does not start SSH and does not enable TLS or HTTPS.

## Quick start: filesystem read benchmark

A read benchmark needs an existing data file. First create one using records of the same size that the reader will
request. This preparation command creates a 1 GiB file from 1,048,576 records of 1,024 bytes:

```bash
./build/install/sbk/bin/sbk \
  -class file -file /tmp/sbk-weblogger.dat \
  -writers 1 -size 1024 -records 1048576
```

Then run a 60-second filesystem read benchmark with live graphs:

```bash
./build/install/sbk/bin/sbk \
  -class file -file /tmp/sbk-weblogger.dat \
  -readers 1 -size 1024 -seconds 60 \
  -out WebLogger
```

SBK starts the web console when necessary and normally opens the run URL in the default browser. On a headless host,
disable automatic browser opening and connect through an appropriate secure tunnel:

```bash
./build/install/sbk/bin/sbk \
  -class file -file /tmp/sbk-weblogger.dat \
  -readers 1 -size 1024 -seconds 60 \
  -out WebLogger -webopen false
```

The graphs show completed record rate and throughput, write/read request rates, worker and connection counts,
pending requests, timeout and invalid-latency counts, average/minimum/maximum latency, and configured latency
percentiles. Every point represents one regular SBK reporting window (five seconds by default). Cumulative
`printTotal` results produced at shutdown or by a total-buffer flush are deliberately excluded because mixing
cumulative and interval values would distort the live graphs. The last interval snapshot remains selectable after
the benchmark finishes.

## Local Web Console options

Logger options appear only after selecting the WebLogger class. Treat generated help as authoritative. The
`-web...` names identify Local Web Console behavior without implying use of the separately deployable SBK Dashboard.

```bash
./build/install/sbk/bin/sbk -out WebLogger -help
./build/install/sbk/bin/sbm -out SbmWebLogger -help
./build/install/sbk/bin/sbk-gem -out GemWebLogger -help
```

| Option | Default | Meaning |
|---|---:|---|
| `-webhost HOST` | `0.0.0.0` | Address on which the plain HTTP Local Web Console server listens |
| `-webport PORT` | `9720` | Local Web Console HTTP port |
| `-webstart true\|false` | `true` | Start a compatible Local Web Console server when none is reachable |
| `-webopen true\|false` | `true` | Ask the local desktop to open the Local Web Console run URL |
| `-webminutes N` | `180` | Minutes of interval snapshots retained for each run (three hours by default) |
| `-boardname NAME` | empty | Optional display name that identifies the benchmark board in the Local Web Console |

`-webstart false` is useful when an operator manages the web console process separately. If no compatible
server is available, SBK continues without live graphs and reports the reason. A different service or an older,
incompatible web console on the configured port is never treated as the SBK web console.

## Server ownership and shutdown

One web console server accepts one active `WebLogger`, `SbmWebLogger`, or `GemWebLogger` benchmark at a time. This
prevents unrelated runs from being presented as one active experiment. A second active benchmark exits with an
ownership error identifying the current run and occupied port, and recommends
`-webport <different-port>`. Selecting another port starts an independent `WebConsoleMain`, allowing
multiple web consoles to run in parallel without mixing their benchmark streams.

The server lifecycle is:

1. The first WebLogger application starts a server when one is not already reachable.
2. A compatible idle server is reused; another server process is not started.
3. The active logger renews its run lease every 15 seconds. Publishing a measurement snapshot also renews the same
   lease, so normal reporting traffic needs no separate heartbeat.
4. When the benchmark finishes normally, its bounded interval history remains in server memory. Completion is
   tracked as run metadata rather than as a cumulative performance snapshot.
5. If the benchmark process disappears without completing its run, one minute without a snapshot or logger
   heartbeat marks the run as abandoned and releases web console ownership. A new benchmark can then register.
6. An open browser renews a separate lightweight lease every 15 seconds, keeping completed or abandoned graphs
   available.
7. If a browser connects during the one-minute idle grace period, the pending shutdown is cancelled while that
   browser remains connected.
8. If SBK, SBM, or SBK-GEM connects during the grace period, it becomes the active run and cancels the pending
   shutdown.
9. When an abandoned run has no attached browser, the server exits as soon as the one-minute run lease expires.
   In every other idle state, the server exits after one minute with neither benchmark nor browser activity.

Closing a browser releases its lease. If a browser or network disappears without a clean close, the lease expires
from its last renewal, so a dead TCP connection cannot keep the process alive indefinitely. The logger and browser
leases are independent: an attached browser preserves old graphs, but it cannot retain ownership for a dead
benchmark.

## Distributed WebLogger modes

For manually launched distributed clients, start SBM with `SbmWebLogger`:

```bash
./build/install/sbk/bin/sbm \
  -out SbmWebLogger -class file -action r
```

Then point each SBK client at the SBM host. Remote clients use `GrpcLogger`; only SBM owns the web console:

```bash
./build/install/sbk/bin/sbk \
  -class file -file /tmp/sbk-weblogger.dat \
  -readers 1 -size 1024 -seconds 60 \
  -out GrpcLogger -sbm <sbm-host> -sbmport 9717
```

For SSH-orchestrated benchmarking, select `GemWebLogger` on SBK-GEM:

```bash
./build/install/sbk/bin/sbk-gem \
  -out GemWebLogger -nodes host1,host2 \
  -class file -file /tmp/sbk-weblogger.dat \
  -readers 1 -size 1024 -seconds 60
```

GEM starts an embedded SBM. Remote SBK processes send SBP/gRPC measurements to that aggregator, and
`GemWebLogger` publishes only the combined cluster result to the browser.

## Network and security

The default `0.0.0.0` binding accepts unsecured HTTP connections on every network interface. WebLogger neither
starts SSH nor enables TLS/HTTPS. A remote browser can therefore connect directly:

```text
http://<benchmark-host>:9720
```

The web console has no authentication or encryption. Use the default only on an isolated, trusted benchmark network
protected by host and network firewall rules. To restrict access to the benchmark host, set
`-webhost 127.0.0.1`.

An SSH tunnel remains an optional security measure when the web console is bound to loopback:

```bash
ssh -L 9720:127.0.0.1:9720 user@benchmark-host
```

Then open <http://127.0.0.1:9720> locally.

## Troubleshooting

| Symptom | Resolution |
|---|---|
| Browser does not open | Copy the printed URL manually, or use `-webopen false` on headless systems |
| Local Web Console unavailable | Check `-webhost`, `-webport`, local firewall rules, and whether startup is disabled |
| Port is incompatible | Stop the unrelated/older service or select another `-webport` |
| Local Web Console remains on an older UI after upgrading SBK | Close every web console browser tab, wait one idle minute for the old server to exit, and retry |
| Local Web Console already in use | Wait for the named active benchmark to finish; do not combine independent experiments |
| Local Web Console reports an abandoned run | The logger stopped publishing snapshots and heartbeats for one minute, usually because its SBK, SBM, or SBK-GEM process was killed or lost connectivity; correct the failure and start a new benchmark |
| Read benchmark reports no useful data | Create and verify the input file first; use the same record size for preparation and reading |
| Graph disappears after completion | Keep a browser page connected; otherwise the server intentionally exits after one idle minute |
| Remote browser cannot connect | Verify port 9720 is allowed by the benchmark host firewall and use `http://<benchmark-host>:9720` |

The reusable runtime is under `sbk-web-console/src/main/java/io/sbk/webconsole`; the SBK-specific
`WebConsoleLoggerSupport` adapter remains under `sbk-api`. `WebConsoleServer` owns run registration, bounded
histories, browser leases, SSE, and idle shutdown, while `WebLogger`, `SbmWebLogger`, and `GemWebLogger` remain in
their application modules.
