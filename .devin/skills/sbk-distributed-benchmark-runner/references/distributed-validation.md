# Distributed preflight, diagnosis, and acceptance

## Preflight checklist

- Same SBK version/commit and compatible SBP protocol on all load nodes.
- JDK 25 and intended JVM options on controller and nodes.
- Correct driver dependencies in the installed distribution.
- Trusted SSH host keys and successful non-interactive authentication.
- Writable remote working directory and adequate disk space.
- Controller resolves every node; every node resolves the advertised SBM host.
- Required SSH, SBM, Local Web Console/metrics, and backend ports are reachable.
- Every load node can authenticate to the backend and access only the dedicated
  benchmark target.
- Host clocks are synchronized for cross-host correlation.
- CPU, memory, network, and backend capacity can support the planned load.

## Scale in stages

1. ordinary SBK on one node, fixed low record count;
2. GEM/GrpcLogger path on one node, fixed low record count;
3. one-node timed run;
4. two-node fixed-count run;
5. two-node timed run;
6. full topology only after previous counts and return codes match.

This isolates driver failures from SSH, provisioning, callback, and aggregation
failures.

## Diagnose by boundary

| Symptom | Boundary to inspect |
|---|---|
| SSH connect/authentication failure | account, route, port, agent/key, `authorized_keys` |
| Host key rejected | correct user's `known_hosts`, changed/unknown server key |
| Java discovery/copy timeout | remote command exit/stderr, permissions, space, `javaversion`, `javadir` |
| SBK mismatch/copy failure | local `sbkdir`, remote permissions/space, `copy`, `delete` |
| Remote command starts but SBM sees no client | advertised `localhost`, port 9717 route/firewall, GrpcLogger |
| Some nodes return no records | per-host stdout/stderr and return code, backend reachability |
| Backend errors only at scale | throttling, connection limits, namespace collision, network saturation |
| Invalid/discarded latencies | latency bounds/unit, overload, clock/measurement errors |
| Local Web Console unavailable/busy | configured `-webport` ownership (`9720` by default); choose another port |

Do not report an SSH host-key failure as a password failure. Preserve the root
cause and the complete causal exception.

## Acceptance criteria

A distributed result is valid only when:

- all intended nodes authenticated and launched;
- all remote return codes are zero;
- aggregate connection count matches client count;
- fixed-count completions match the intended distribution;
- no client or controller reports I/O errors or timeouts;
- invalid latencies are zero and discarded latencies are explained;
- the target dataset/objects/messages match the planned operation;
- no unintended load-host bottleneck distorted a backend-performance claim;
- raw per-host and aggregate outputs are retained.

Report node count, workers per node, aggregate and per-node throughput, latency
distribution, run-to-run variability, backend/load-host utilization, and any
asymmetry. A sum across hosts is not proof of linear scaling; compare 1, 2, and
N-node results with the same per-node workload and show scaling efficiency.
