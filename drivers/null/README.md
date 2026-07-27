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

# Null driver

`-class null` is SBK's idle, pending-operation, and timeout test driver. It
does not access an external storage system.

The default writer does **not** complete an operation. It returns an incomplete
`CompletableFuture` with the configured timeout, which lets tests verify:

- periodic reporting when a window contains no completed records;
- idle `ElasticWait` behavior;
- shutdown while an operation is pending;
- future timeout and error propagation.

The reader sleeps for the configured timeout and then returns `null`. Normal
timed shutdown can interrupt that sleep.

## Options

| Option | Default | Meaning |
|---|---:|---|
| `-n` | `0` | When greater than zero, execute an empty loop with this iteration bound and then complete the write synchronously. At zero, return an incomplete future. |
| `-timeout` | `2147483647` ms | Timeout applied to the pending writer future and sleep duration used by the reader. |

The empty `-n` loop is only a control-flow workload. A JIT compiler may
optimize it, so it is not a calibrated CPU-delay mechanism and should not be
used for latency claims.

## Idle reporting example

This run should print periodic zero-record windows and then stop at the
configured duration:

```bash
./build/install/sbk/bin/sbk -class null -writers 1 -size 1024 -seconds 15
```

## Synchronous control-flow example

Setting `-n` above zero makes each write complete synchronously:

```bash
./build/install/sbk/bin/sbk \
  -class null -writers 1 -size 1024 -records 1000 -n 100
```

This can smoke-test completed-operation control flow, but it is not the
recommended queue-performance workload.

## Null versus PerlBench

Both drivers are synthetic, but they deliberately exercise opposite states:

| Property | `Null` | [`PerlBench`](../perlbench/README.md) |
|---|---|---|
| Default write | Remains pending | Completes immediately |
| Read | Sleeps, then returns `null` | Immediately returns a reusable payload |
| Expected default result | Idle windows with zero completed records | High-rate completed records and latency samples |
| Primary purpose | Idle reporting, timeout, interruption, and shutdown behavior | End-to-end SBK/PerL throughput, allocation, and timestamp-queue comparison |
| Suitable for `-mpscqueue` A/B performance tests | No | Yes |

They should not be merged: the distinction between "no completion" and
"completion as fast as possible" is the behavior each driver exists to test.

Null-driver results are not storage results and should not be compared with
durable backend operations. Use `-class null -help` for current defaults.
