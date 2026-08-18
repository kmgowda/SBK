<!--
Copyright (c) KMG. All Rights Reserved.
Licensed under the Apache License, Version 2.0.
-->

# SBK Kubernetes examples

This directory contains an example RabbitMQ benchmark manifest and helper script. They are starting points for a controlled benchmark namespace, not production-ready deployment templates.

Files:

- [`sbk-rabbitmq-k8-sample.yaml`](sbk-rabbitmq-k8-sample.yaml): example Pod/workload configuration.
- [`sbk-rabbitmq-k8-sample.sh`](sbk-rabbitmq-k8-sample.sh): helper commands for that example.

## Before applying

1. Review the manifest and image tag.
2. Replace backend addresses and topic names.
3. Put credentials in an appropriate Kubernetes Secret rather than the manifest or command line.
4. Set CPU and memory requests/limits deliberately; throttling changes benchmark results.
5. Select nodes, storage classes, and network paths that match the experiment.
6. Use a disposable namespace and backend data set.
7. Confirm the pod can reach the backend and, for `GrpcLogger`, the SBM service.
8. Deploy RabbitMQ as a Service named `rabbitmq` in the benchmark namespace, or
   replace the `sbk-rabbitmq` ConfigMap value with the selected broker address.

```bash
kubectl create namespace sbk-benchmark
kubectl -n sbk-benchmark apply -f kubernetes/sbk-rabbitmq-k8-sample.yaml
kubectl -n sbk-benchmark logs -f <pod-name>
```

The helper script uses `sbk-benchmark` by default. Override it with
`NAMESPACE=<namespace> kubernetes/sbk-rabbitmq-k8-sample.sh` when required.

Inspect the manifest's actual resource name before replacing `<pod-name>`.

## One-off run

For a short local-file smoke test:

```bash
kubectl -n sbk-benchmark run sbk-file \
  --restart=Never \
  --image=kmgowda/sbk:latest \
  -- \
  -class file -file /tmp/sbk.bin -writers 1 -size 4096 -seconds 15
```

Pin an immutable image version or digest for recorded experiments. Ephemeral container files disappear with the pod; mount a volume when data must survive for a later read test.

## Benchmark validity

Record pod requests/limits, node type, placement, container runtime, CNI, service routing, image digest, JVM settings, and backend locality. Check CPU throttling, restarts, eviction, and network errors before accepting results. A Kubernetes service mesh or proxy can materially change latency.

Clean up the dedicated namespace only after preserving required logs and results:

```bash
kubectl delete namespace sbk-benchmark
```

See [container guidance](../dockers/README.md), [Grafana/Prometheus](../grafana/README.md), and the [reproducibility checklist](../docs/sbk-internals.md#134-reproducibility-checklist-for-an-sbk-based-study).
