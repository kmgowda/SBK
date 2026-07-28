# Distributed SBK topology

## Application roles

```text
SBK / SBK-YAL
  One load-generator process. It performs storage operations and measures them.

SBM
  Aggregator only. It receives SBP/gRPC measurements from manually launched
  SBK clients. It does not execute storage I/O and does not launch processes.

SBK-GEM / SBK-GEM-YAL
  Controller and SSH orchestrator. It launches SBK on remote load hosts and
  owns an embedded SBM for aggregate results.
```

## GEM flow

```text
Operator
   |
   v
SBK-GEM controller ----------------------+
   | SSH                                 | embedded SBM, default TCP 9717
   +--> load host A: SBK + GrpcLogger ---+
   +--> load host B: SBK + GrpcLogger ---+
              |                          |
              +------> target storage <--+
```

Required paths:

- controller -> each node: SSH, normally TCP 22;
- each node -> target storage: driver-specific endpoint/ports;
- each node -> controller: SBM gRPC, normally TCP 9717;
- operator/browser -> controller: optional WebLogger HTTP, normally TCP 9720;
- Prometheus -> controller: optional metrics endpoint/context.

`-localhost <address>` is the SBM callback address embedded in each remote SBK
command. Use a controller hostname or IP resolvable and reachable from all
nodes. `127.0.0.1` or `localhost` is correct only when the SBK client is on the
same host as SBM.

## When GEM is and is not needed

Use GEM when the requested workload needs coordinated load from several hosts.
Do not use GEM merely because MinIO, Kafka, PostgreSQL, or another backend is
remote; one normal SBK process can connect directly to that service.

Use standalone SBM when:

- Kubernetes, a scheduler, or another orchestrator launches clients;
- SSH is forbidden or inappropriate;
- clients intentionally use different commands but need one aggregate;
- the operator wants explicit control over process placement.

Use GEM when:

- the same SBK workload should run across an SSH host list;
- the controller should check/copy compatible Java and SBK installations;
- one command/YML should manage launch, aggregation, and remote responses.

## Distributed result meaning

SBM aggregates measurement records from clients. Aggregate throughput is the
sum observed across connected load generators. Verify that the connection count
matches the node/process plan. SBM cannot make mismatched client versions,
different backend configurations, network asymmetry, or unsynchronized clocks
equivalent; record and validate those separately.
