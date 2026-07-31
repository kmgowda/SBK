# SBK and SBK-YAL workflows

JDK 25 is required for both Gradle and the generated applications. Confirm
`java -version` reports JDK 25, then run `./gradlew installDist` to produce
`./build/install/sbk/bin/`.

## Discover, then run

```bash
./build/install/sbk/bin/sbk -help
./build/install/sbk/bin/sbk -class file -help
./build/install/sbk/bin/sbk -class file -out WebLogger -help
```

The first command lists common options and enabled drivers. The second adds
driver options. The third adds logger options.

## Safe harness smoke test

This exercises workers, PerL, and reporting without storage I/O:

```bash
./build/install/sbk/bin/sbk \
  -class null -writers 1 -size 100 -records 10000 -time ns
```

Do not use `Null` results as storage-performance results.

## Local file write and matching read

Write a dedicated file:

```bash
./build/install/sbk/bin/sbk \
  -class file -file /tmp/sbk-agent-benchmark.dat \
  -writers 1 -size 4096 -seconds 30 -time mcs
```

Read the same file after the write completes:

```bash
./build/install/sbk/bin/sbk \
  -class file -file /tmp/sbk-agent-benchmark.dat \
  -readers 1 -size 4096 -seconds 30 -time mcs
```

Confirm the path is disposable before deleting it. File read results can be
dominated by the operating-system page cache; state whether the cache was warm
or cold and never silently drop caches on a shared host.

## SBK-YAL

YML uses the top-level `sbkArgs` map. Run a bundled example:

```bash
./build/install/sbk/bin/sbk-yal \
  -f .devin/skills/sbk-benchmark-runner/references/example-sbk-file-write.yml
```

Create one unique directory and override the template path for both commands:

```bash
SBK_EXAMPLE_DIR=$(mktemp -d /tmp/sbk-agent-benchmark.XXXXXX)
SBK_EXAMPLE_FILE="$SBK_EXAMPLE_DIR/data.dat"

./build/install/sbk/bin/sbk-yal \
  -f .devin/skills/sbk-benchmark-runner/references/example-sbk-file-write.yml \
  -file "$SBK_EXAMPLE_FILE"

./build/install/sbk/bin/sbk-yal \
  -f .devin/skills/sbk-benchmark-runner/references/example-sbk-file-read.yml \
  -file "$SBK_EXAMPLE_FILE"
```

The write creates exactly 256 records; the matching read consumes exactly 256
records from the same per-run file. Remove the directory only after confirming
the path is disposable. Use the timed CLI examples above for throughput
measurement.

Command-line values override YML values without duplicating the option:

```bash
./build/install/sbk/bin/sbk-yal \
  -f .devin/skills/sbk-benchmark-runner/references/example-sbk-file-write.yml \
  -records 512 -writers 2
```

Use `sbk-yal -p -f <file>` to print the effective SBK option help. The YAL
process delegates to the same SBK engine; it does not change measurement
semantics.

## External backend example: MinIO/S3

Generate current options and read `drivers/minio/README.md` first:

```bash
./build/install/sbk/bin/sbk -class minio -help
```

Use an approved secret manager or protected launcher to populate
`SBK_S3_ACCESS_KEY` and `SBK_S3_SECRET_KEY` in the process environment. When
`-key` or `-secret` is explicitly supplied it takes precedence, but placing
credentials in command arguments exposes them through the process argument
list.

Use HTTPS for credential-bearing remote endpoints and a dedicated
bucket/prefix:

```bash
./build/install/sbk/bin/sbk \
  -class minio \
  -url https://s3-benchmark.example.com:9000 \
  -bucket sbk-agent-isolated \
  -prefix trial-001/ \
  -writers 1 -size 4096 -records 100 -time mcs
```

Plain HTTP remains supported for local or isolated trusted-lab endpoints where
TLS is deliberately unavailable; do not send credentials over an untrusted
network. Confirm bucket creation, recreation, versioning, cleanup, and delete
options before enabling them.

## Choose workload termination

- `-seconds N`: stop the timed workload after approximately N seconds; useful
  for throughput and steady-state windows.
- `-records N` without `-seconds`: complete exactly the requested operation
  count; useful for correctness and fixed-work comparisons.
- With `-seconds`, `-records` is a rate target rather than a total count.
- `-throughput > 0` controls data rate in MB/s; `0` uses record-rate behavior;
  `-1` requests maximum throughput.

Start with one writer or reader. Increase workers until the intended component
is saturated, while watching load-host CPU, heap/GC, disk, network, and backend
health.

## Select output

| Logger | Use |
|---|---|
| `SystemLogger` | Console periodic windows and final result |
| `CSVLogger` | Machine-readable result file; generate its help before choosing file options |
| `PrometheusLogger` | Prometheus endpoint for external Prometheus/Grafana |
| `WebLogger` | Built-in plain-HTTP Local Web Console; no Docker required |
| `GrpcLogger` | Send measurements to standalone SBM; not a normal standalone display logger |

For WebLogger:

```bash
./build/install/sbk/bin/sbk \
  -class file -file /tmp/sbk-agent-benchmark.dat \
  -writers 1 -size 4096 -seconds 30 \
  -out WebLogger -webopen false
```

Use the Local Web Console URLs printed by SBK. It listens on `0.0.0.0:9720` by default,
uses unencrypted HTTP, and should be exposed only on a trusted benchmark
network. See `docs/WEB_LOGGER.md`.
