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

# PrometheusLogger and SBK Dashboard

`PrometheusLogger` exposes SBK's latest periodic benchmark measurements through a Prometheus-compatible HTTP
endpoint. The separately deployed [SBK Dashboard](https://github.com/kmgowda/sbk-dashboard) runs official
Prometheus and Grafana processes, scrapes one or more SBK/SBM endpoints, retains their time series, and creates an
isolated Grafana dashboard for every registered `host:port`.

This path is intended for durable history, multiple benchmark endpoints, and full Grafana visualization. For one
lightweight, in-memory live view without Prometheus or Grafana, use [`WebLogger`](WEB_LOGGER.md) instead.

## Components and ports

```text
Direct benchmark
  SBK + PrometheusLogger :9718/metrics
                   |
                   v
  sbk-dashboard management UI :9721
                   |
                   +--> managed Prometheus :9090 (loopback by default)
                   +--> managed Grafana    :3000

Distributed benchmark
  remote SBK + GrpcLogger --> SBM/SBK-GEM :9717 (gRPC)
                                      |
                                      +--> Prometheus metrics :9719/metrics
                                                        |
                                                        v
                                                  sbk-dashboard
```

| Purpose | Default | Where it runs |
|---|---:|---|
| Direct SBK metrics | `9718/metrics` | The host running `sbk` or `sbk-yal` |
| SBM/SBK-GEM aggregate metrics | `9719/metrics` | The SBM host or SBK-GEM coordinator |
| SBM gRPC ingestion | `9717` | The SBM host or SBK-GEM coordinator |
| SBK Dashboard management UI/API | `9721` | The dashboard host or container |
| SBK Dashboard Grafana UI | `3000` | The dashboard host or container |
| Dashboard-managed Prometheus | `9090` | Loopback inside the dashboard deployment by default |

The exporter is active only while its SBK or SBM process is running. Prometheus retains previously scraped samples
after a benchmark exits, subject to SBK Dashboard's retention setting.

## Direct SBK quick start

Build the distribution, then run a filesystem benchmark with the logger selected explicitly:

```bash
./gradlew installDist

./build/install/sbk/bin/sbk \
  -class file -file /tmp/sbk-prometheus.dat \
  -writers 1 -size 4096 -seconds 60 \
  -out PrometheusLogger
```

The default endpoint is `http://127.0.0.1:9718/metrics`. At startup SBK prints ready-to-copy links for localhost,
IPv4 loopback, hostname, and each usable private or public IPv4 address assigned to the host. Address discovery is
best-effort and happens only during logger startup, outside the measurement hot path.

Confirm that the exporter is reachable while the benchmark is running:

```bash
curl -fsS http://127.0.0.1:9718/metrics | head
```

Choose another port or path with `-context PORT/PATH`:

```bash
./build/install/sbk/bin/sbk \
  -class file -file /tmp/sbk-prometheus.dat \
  -readers 1 -size 4096 -seconds 60 \
  -out PrometheusLogger -context 19718/sbk-metrics
```

The matching scrape endpoint is `http://<sbk-host>:19718/sbk-metrics`. Use `-context no` only when Prometheus
export is deliberately disabled. Logger-specific help is authoritative:

```bash
./build/install/sbk/bin/sbk -out PrometheusLogger -help
```

Only one process can listen on a particular address and port. Assign a distinct `-context` port when running
multiple PrometheusLogger processes on the same host.

## Distributed metrics

### Standalone SBM

Start SBM with its aggregate Prometheus logger:

```bash
./sbm/build/install/sbm/bin/sbm \
  -out SbmPrometheusLogger -class file -action r
```

Then run one or more SBK clients with `GrpcLogger`:

```bash
./build/install/sbk/bin/sbk \
  -class file -file /tmp/sbk-prometheus.dat \
  -readers 1 -size 4096 -seconds 60 \
  -out GrpcLogger -sbm <sbm-host> -sbmport 9717
```

Register `<sbm-host>:9719/metrics` in SBK Dashboard. Do not register each gRPC client as a Prometheus endpoint in
this mode; SBM owns and publishes the combined result.

### SBK-GEM and SBK-GEM-YAL

`GemPrometheusLogger` is backed by the embedded SBM. Remote SBK processes send measurements to the coordinator with
`GrpcLogger`, and the coordinator publishes the aggregate on port 9719:

```bash
./build/install/sbk/bin/sbk-gem \
  -out GemPrometheusLogger \
  -class file -nodes host1,host2 \
  -writers 2 -size 4096 -seconds 60
```

Register `<sbk-gem-host>:9719/metrics`. The metrics use `component="sbm"` because GEM delegates aggregation and
metrics ownership to SBM; `sbk-gem` is not a separate metrics component.

YML launchers accept the same logger and context options in their merged argument set. Command-line values override
YML values without duplicating the option:

```bash
./build/install/sbk/bin/sbk-gem-yal \
  -f benchmark.yml -out GemPrometheusLogger -context 9719/metrics
```

## Start SBK Dashboard

SBK Dashboard is maintained in a dedicated repository and is not started by `PrometheusLogger`. Clone it on the
monitoring host:

```bash
git clone https://github.com/kmgowda/sbk-dashboard.git
cd sbk-dashboard
```

### Docker Compose

Compose is the shortest deployment path:

```bash
docker compose pull
docker compose up --detach
```

Open <http://localhost:9721/>. Grafana links opened from that page use <http://localhost:3000/>. Registrations,
Prometheus history, and Grafana state are stored in the `sbk-dashboard-data` volume.

When SBK runs on the Docker host, register `host.docker.internal`, not `127.0.0.1`: loopback inside the container
refers to the container itself. The supplied Compose file installs the host-gateway mapping. For SBK on another
machine, register the hostname or IP address routable from the container.

### Python virtual environment

For a direct host deployment using native Prometheus and Grafana child processes:

```bash
python3 -m venv .venv
. .venv/bin/activate
python -m pip install --upgrade pip
python -m pip install .
sbk-dashboard
```

On its first start, SBK Dashboard locates or downloads checksum-pinned Prometheus and Grafana distributions. Open
the printed management URL, normally <http://localhost:9721/>. Stop the foreground process with `Ctrl+C` and wait
for its managed services to shut down before deactivating the environment.

## Register an exporter

Use the management form at port 9721 and supply:

| Field | Value |
|---|---|
| Display name | A meaningful benchmark/source name |
| Host or IP | Address reachable from the Prometheus process |
| Port | `9718` for direct SBK; `9719` for SBM or SBK-GEM |
| Metrics path | `/metrics`, unless changed with `-context` |

Registration can also use the management API:

```bash
curl --fail-with-body \
  -H 'Content-Type: application/json' \
  -d '{"name":"SBK file write","host":"127.0.0.1","port":9718,"metricsPath":"/metrics"}' \
  http://127.0.0.1:9721/api/targets
```

Endpoint identity is normalized `host:port`. The same host with a second port receives an independent dashboard;
changing only the display name or metrics path does not create another identity. After registration:

1. `pending` means the next Prometheus target refresh has not completed.
2. `up` means the latest scrape succeeded.
3. `down` means the exporter is stopped, unreachable, or returned a scrape failure.
4. **Open dashboard** opens the endpoint-scoped Grafana dashboard.

Inspect control-plane health and registered targets with:

```bash
curl -fsS http://127.0.0.1:9721/api/health
curl -fsS http://127.0.0.1:9721/api/targets
```

## Metric interpretation

SBK publishes the latest regular reporting window, which defaults to five seconds. It intentionally does not replace
those gauges with `printTotal(...)` values: a cumulative final total would distort live window throughput and
latency graphs. Prometheus stores each scraped window, so Grafana still shows the run over time.

Common tags identify the source:

| Tag | Meaning |
|---|---|
| `component` | `sbk` for direct exporters or `sbm` for standalone/embedded aggregation |
| `class` | Actual storage driver simple class name, such as `File` or `MinIO` |
| `action` | Benchmark action, such as `Reading` or `Writing` |

The dashboard includes throughput, operation counts, request pressure, pending responses, timeout/error indicators,
average/minimum/maximum latency, SLC counters, discarded/invalid latencies, and configured latency percentiles.
Treat a target that was down during the run or a run with invalid/discarded records as requiring investigation before
using its results for comparisons.

## Retention, networking, and security

SBK Dashboard keeps Prometheus history for seven days by default. Set another number of days when starting it:

```bash
sbk-dashboard -data /var/lib/sbk-dashboard -retention 14
```

The SBK and SBM embedded metrics servers listen on all interfaces. SBK Dashboard's management port 9721 and Grafana
port 3000 also bind to all IPv4 interfaces by default; its managed Prometheus port 9090 is loopback-only by default.
Neither SBK's exporter nor SBK Dashboard supplies authentication or TLS. Use them only on a trusted benchmark
network, restrict access with host/network firewalls, or place the user-facing services behind an authenticated TLS
reverse proxy. Do not expose exporter, management, Grafana, or SBM gRPC ports directly to an untrusted network.

## Troubleshooting

| Symptom | Checks |
|---|---|
| SBK cannot start PrometheusLogger | Another process may own the configured port; use `ss -ltnp` or choose another `-context` port |
| Dashboard target remains `pending` | Wait for the next target refresh and verify the registration was reconciled |
| Dashboard target is `down` | Curl the printed exporter URL from the Prometheus host or container network namespace |
| Container cannot scrape host SBK | Register `host.docker.internal`, ensure the host-gateway mapping exists, and allow the exporter port through the firewall |
| Remote endpoint cannot be scraped | Check DNS, routing, bind/firewall rules, port, and metrics path from the dashboard host |
| Grafana panels show no data | Confirm target state is `up`, the selected time range covers the run, and the expected `component`, `class`, and `action` tags exist |
| Old history disappears | Check the dashboard `-retention` value and persistent data/volume mapping |
| Grafana link uses the wrong public host | Open management UI through the desired hostname or set SBK Dashboard's `-grafana-url` explicitly |

For dashboard installation, persistence, backup, reverse-proxy, and platform details, use the
[SBK Dashboard usage guide](https://github.com/kmgowda/sbk-dashboard/blob/master/docs/USAGE.md).
