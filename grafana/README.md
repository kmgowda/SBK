# Grafana and Prometheus assets

This directory contains a local Docker Compose monitoring stack, Grafana provisioning/configuration, Prometheus target files, dashboards, and Kubernetes examples for observing SBK and SBM metrics.

## Local Compose stack

Review ports and image tags in `docker-compose.yml`, then start from this directory:

```bash
cd grafana
docker compose up -d
docker compose ps
```

Open the Grafana address configured by the Compose file and select the provisioned SBK dashboard. Stop the stack with:

```bash
docker compose down
```

Do not use default dashboard credentials on a shared network.

## Prometheus targets

Target files under `prometheus/` separate concerns:

- `targets.json`: SBK Prometheus endpoints.
- `sbm-targets.json`: SBM Prometheus endpoints.
- `jmx-targets.json` and `sbm-jmx-targets.json`: JMX exporter targets.
- `node-exporters.json`: host metrics.
- `prometheus.yml`: scrape configuration.

Replace example addresses with endpoints reachable from the Prometheus container. Container-local `localhost` does not normally refer to the benchmark host.

## Kubernetes manifests

The Grafana and Prometheus deployment/service manifests are examples. Apply them
with `kubectl apply -k grafana/` from the repository root. Kustomize generates
ConfigMaps from the checked-in dashboards, provisioning, Grafana configuration,
and Prometheus target files, so the manifests contain no workstation-specific
host paths. Review namespaces, security context, credentials, service exposure,
resource limits, and ConfigMap size limits before production use.

## Interpreting dashboards

Dashboards visualize emitted measurements; they do not correct an invalid experiment. Correlate SBK throughput and latency with client CPU, JVM behavior, network saturation, storage load, errors, discarded latency counts, and active worker counts. Preserve the dashboard revision and Prometheus configuration with published results.

## Updating dashboards

- Keep metric names aligned with `SbkPrometheusServer` and `SbmPrometheusServer`.
- Avoid dashboard queries tied to one host or driver unless clearly labeled.
- Export changed dashboard JSON into `dashboards/` and review the diff for embedded credentials or environment-specific identifiers.
- Validate both an SBK target and, when applicable, an SBM target.

See [SBM](../sbm/README.md), [distributed architecture](../docs/ARCHITECTURE.md#distributed-flow), and [container guidance](../dockers/README.md).
