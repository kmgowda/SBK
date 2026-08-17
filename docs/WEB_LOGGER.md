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
console server runs as the `SbkWebConsoleMain` process from the independent `sbk-web-console` module and is
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

The server listens on all IPv4 interfaces at port 9720. At benchmark start and completion, the logger prints the
run-specific URLs for `localhost`, IPv4 loopback, the hostname, and every usable private or public IPv4 address on
the Web Console host. A remote browser can use a printed hostname or IP URL when routing and firewall rules permit.

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
disable automatic browser opening and copy one of the printed URLs into a browser:

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
| `-webport PORT` | `9720` | Web Console HTTP port on all IPv4 interfaces |
| `-webopen true\|false` | `true` | Ask the local desktop to open the Local Web Console run URL |
| `-websnapshotminutes N` | `180` | Minutes of interval snapshots retained for each run (three hours by default) |
| `-webtimeoutminutes N` | `1` | Idle minutes without an active benchmark or browser before the Local Web Console exits |
| `-boardname NAME` | `<application> <storage>` | Optional display name that identifies the benchmark board in the Local Web Console |

SBK, SBM, and SBK-GEM always probe `127.0.0.1` on the configured port. They reuse a compatible console already
listening there and otherwise start `SbkWebConsoleMain` automatically. A different service or an older, incompatible
web console on the configured port is never treated as the SBK web console. When a compatible console is reused, its
existing idle timeout remains in effect; changing `-webtimeoutminutes` requires starting a new console process,
typically on a different `-webport` or after the old process exits.

When `-boardname` is omitted or blank, the logger generates a readable name from the application and storage class,
such as `SBK File`, `SBM MinIO`, or `SBK-GEM Kafka`. Every run also has an independent UUID used by its direct URL,
history, heartbeat, and event stream, so display names do not need to be unique.

## Concurrent runs and shutdown

One web console server accepts multiple active `WebLogger`, `SbmWebLogger`, and `GemWebLogger` benchmarks on the same
port. Each run is isolated by its UUID and appears separately in the browser's automatically refreshed benchmark
selector. Its logged `/?run=<uuid>` URL opens that run directly. Selecting another port still starts an independent
`SbkWebConsoleMain` when a separate console process is desired.

The server lifecycle is:

1. The first WebLogger application starts a server when one is not already reachable.
2. A compatible server is reused even while other benchmarks are active; another server process is not started.
3. Every active logger independently renews its run lease every 15 seconds. Publishing a measurement snapshot also
   renews that run's lease, so normal reporting traffic needs no separate heartbeat.
4. When the benchmark finishes normally, its bounded interval history remains in server memory. Completion is
   tracked as run metadata rather than as a cumulative performance snapshot.
5. If a benchmark process disappears without completing its run, the configured idle timeout without a snapshot
   or logger heartbeat marks only that run as abandoned. Other active runs continue unaffected.
6. An open browser renews a separate lightweight lease every 15 seconds, keeping completed or abandoned graphs
   available.
7. If a browser connects during the idle grace period, the pending shutdown is cancelled while that
   browser remains connected.
8. If SBK, SBM, or SBK-GEM connects during the grace period, its new run cancels the pending shutdown.
9. The server exits only after every run is completed or abandoned and there has been no browser activity for the
   configured timeout. If the last active run expires with no attached browser, the server can exit immediately
   because that run has already been inactive for the timeout. The default is one minute.

Closing a browser releases its lease. If a browser or network disappears without a clean close, the lease expires
from its last renewal, so a dead TCP connection cannot keep the process alive indefinitely. The logger and browser
leases are independent: an attached browser preserves old graphs, but it cannot keep a dead benchmark run active.

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

The Local Web Console uses plain HTTP and binds to `0.0.0.0`, so it is reachable through the console host's usable
IPv4 interfaces when routing and firewall rules permit. It provides neither authentication nor TLS. Run it only on
a trusted benchmark network, restrict port 9720 with host or network firewall rules, or use an SSH tunnel:

```bash
ssh -L 9720:127.0.0.1:9720 user@benchmark-host
```

Then open the printed forwarded loopback URL locally. The WebLogger does not create the tunnel itself.

## Troubleshooting

| Symptom | Resolution |
|---|---|
| Browser does not open | Copy the printed URL manually, or use `-webopen false` on headless systems |
| Local Web Console unavailable | Check `-webport` and whether another local process is using the port |
| Port is incompatible | Stop the unrelated/older service or select another `-webport` |
| Local Web Console remains on an older UI after upgrading SBK | Close every web console browser tab, wait for the configured idle timeout for the old server to exit, and retry |
| Local Web Console reports an abandoned run | The logger stopped publishing snapshots and heartbeats for the configured idle timeout, usually because its SBK, SBM, or SBK-GEM process was killed or lost connectivity; correct the failure and start a new benchmark |
| Read benchmark reports no useful data | Create and verify the input file first; use the same record size for preparation and reading |
| Graph disappears after completion | Keep a browser page connected; otherwise the server intentionally exits after the configured idle timeout |
| Remote browser cannot connect | Use a printed hostname/IP URL and verify routing plus firewall access to `-webport`; otherwise create an SSH tunnel to `127.0.0.1:9720` |

The reusable runtime is under `sbk-web-console/src/main/java/io/sbk/webconsole`; the SBK-specific
`WebConsoleLoggerSupport` adapter remains under `sbk-api`. `WebConsoleServer` owns run registration, bounded
histories, browser leases, SSE, and idle shutdown, while `WebLogger`, `SbmWebLogger`, and `GemWebLogger` remain in
their application modules.
