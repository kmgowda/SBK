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

# WebLogger: local live benchmark graphs

WebLogger displays SBK measurements in a local browser without Docker, Prometheus, or Grafana. It uses the same
periodic and total measurements printed by SBK, so enabling it does not add measurement sampling or storage-driver
work. The dashboard server is implemented with the JDK HTTP server, retains a bounded in-memory history, and sends
new summaries to browsers with server-sent events (SSE).

Use the logger matching the application:

| Application | Logger | Displayed result |
|---|---|---|
| `sbk` or `sbk-yal` | `WebLogger` | One local SBK workload |
| `sbm` | `SbmWebLogger` | Aggregated results received from distributed SBK clients |
| `sbk-gem` or `sbk-gem-yal` | `GemWebLogger` | Cluster aggregate produced by GEM's embedded SBM |

The local dashboard URL is <http://127.0.0.1:9720>. The server listens on `0.0.0.0` by default, so a browser on
another system can use `http://<benchmark-host>:9720`. The exact local run URL is printed when the logger starts.
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

SBK starts the dashboard when necessary and normally opens the run URL in the default browser. On a headless host,
disable automatic browser opening and connect through an appropriate secure tunnel:

```bash
./build/install/sbk/bin/sbk \
  -class file -file /tmp/sbk-weblogger.dat \
  -readers 1 -size 1024 -seconds 60 \
  -out WebLogger -dashboardopen false
```

The graphs show completed record rate and throughput, write/read request rates, worker and connection counts,
pending requests, timeout and invalid-latency counts, average/minimum/maximum latency, and configured latency
percentiles. The final total snapshot remains selectable after the benchmark finishes.

## Dashboard options

Logger options appear only after selecting the WebLogger class. Treat generated help as authoritative:

```bash
./build/install/sbk/bin/sbk -out WebLogger -help
./build/install/sbk/bin/sbm -out SbmWebLogger -help
./build/install/sbk/bin/sbk-gem -out GemWebLogger -help
```

| Option | Default | Meaning |
|---|---:|---|
| `-dashboardhost HOST` | `0.0.0.0` | Address on which the plain HTTP server listens |
| `-dashboardport PORT` | `9720` | Dashboard HTTP port |
| `-dashboardstart true\|false` | `true` | Start a compatible server when none is reachable |
| `-dashboardopen true\|false` | `true` | Ask the local desktop to open the run URL |
| `-dashboardretention N` | `3600` | Maximum snapshots retained for each run |
| `-dashboardname NAME` | empty | Human-readable name shown for the run |

`-dashboardstart false` is useful when an operator manages the dashboard process separately. If no compatible
server is available, SBK continues without live graphs and reports the reason. A different service or an older,
incompatible dashboard on the configured port is never treated as the SBK dashboard.

## Server ownership and shutdown

One dashboard server accepts one active `WebLogger`, `SbmWebLogger`, or `GemWebLogger` benchmark at a time. This
prevents unrelated runs from being presented as one active experiment. A second active benchmark exits with an
ownership error identifying the current run.

The server lifecycle is:

1. The first WebLogger application starts a server when one is not already reachable.
2. A compatible idle server is reused; another server process is not started.
3. When the benchmark finishes, its bounded history and final snapshot remain in server memory.
4. An open browser renews a lightweight lease every 15 seconds, keeping the completed graphs available.
5. If a browser connects during the one-minute idle grace period, the pending shutdown is cancelled while that
   browser remains connected.
6. If SBK, SBM, or SBK-GEM connects during the grace period, it becomes the active run and cancels the pending
   shutdown.
7. When no benchmark is active and no browser lease is present for one minute, the server exits gracefully.

Closing a browser releases its lease. If a browser or network disappears without a clean close, the lease expires
from its last renewal, so a dead TCP connection cannot keep the process alive indefinitely.

## Distributed WebLogger modes

For manually launched distributed clients, start SBM with `SbmWebLogger`:

```bash
./build/install/sbk/bin/sbm \
  -out SbmWebLogger -class file -action r
```

Then point each SBK client at the SBM host. Remote clients use `GrpcLogger`; only SBM owns the dashboard:

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

The dashboard has no authentication or encryption. Use the default only on an isolated, trusted benchmark network
protected by host and network firewall rules. To restrict access to the benchmark host, set
`-dashboardhost 127.0.0.1`.

An SSH tunnel remains an optional security measure when the dashboard is bound to loopback:

```bash
ssh -L 9720:127.0.0.1:9720 user@benchmark-host
```

Then open <http://127.0.0.1:9720> locally.

## Troubleshooting

| Symptom | Resolution |
|---|---|
| Browser does not open | Copy the printed URL manually, or use `-dashboardopen false` on headless systems |
| Dashboard unavailable | Check `-dashboardhost`, `-dashboardport`, local firewall rules, and whether startup is disabled |
| Port is incompatible | Stop the unrelated/older service or select another `-dashboardport` |
| Dashboard already in use | Wait for the named active benchmark to finish; do not combine independent experiments |
| Read benchmark reports no useful data | Create and verify the input file first; use the same record size for preparation and reading |
| Graph disappears after completion | Keep a browser page connected; otherwise the server intentionally exits after one idle minute |
| Remote browser cannot connect | Verify port 9720 is allowed by the benchmark host firewall and use `http://<benchmark-host>:9720` |

The implementation is under `sbk-api/src/main/java/io/sbk/dashboard`. `DashboardLoggerSupport` is shared by SBK,
SBM, and SBK-GEM; `DashboardServer` owns run registration, bounded histories, browser leases, SSE, and idle shutdown.
