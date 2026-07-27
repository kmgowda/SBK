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

# Storage driver guide

Drivers are the primary SBK extension point. A driver converts the harness's generic payload operations into calls to one storage API while leaving scheduling, rate control, timing, and percentile calculation to SBK and PerL.

## Runtime inventory

The aggregate distribution currently enables 53 driver projects. The source tree also contains disabled drivers and a template.

| Category | Enabled drivers |
|---|---|
| Object and blob storage | `cephs3`, `minio`, `openio`, `seaweeds3` |
| Files and distributed filesystems | `asyncfile`, `file`, `filestream`, `hdfs` |
| Streaming and messaging | `activemq`, `artemis`, `bookkeeper`, `kafka`, `nats`, `natsStream`, `nsq`, `pravega`, `pulsar`, `rabbitmq`, `redpanda`, `rocketmq` |
| Relational and SQL systems | `db2`, `derby`, `exasol`, `h2`, `hive`, `jdbc`, `mariadb`, `mssql`, `mysql`, `postgresql`, `sqlite` |
| Document, search, and analytical systems | `chromadb`, `couchbase`, `couchdb`, `dynamodb`, `elasticsearch`, `mongodb`, `solr` |
| Key-value and embedded stores | `fdbrecord`, `foundationdb`, `leveldb`, `memcached`, `redis`, `rocksdb` |
| Harness and local data structures | `atomicq`, `cassandra`, `concurrentq`, `conqueue`, `csv`, `linkedbq`, `null`, `perlbench`, `syncq` |

`ignite` is disabled in the Gradle registration files. `halodb` is disabled because its GitHub Packages dependency can be unavailable without credentials or package quota. `sbktemplate` is intentionally excluded because it is a scaffold.

The registration files—not this table—are authoritative:

- `settings-drivers.gradle` creates enabled Gradle subprojects.
- `build-drivers.gradle` adds them to the root runtime distribution.

## Driver contract

A storage class implements `io.sbk.api.Storage<T>`:

```java
public interface Storage<T> {
    void addArgs(InputOptions params);
    void parseArgs(ParameterOptions params);
    void openStorage(ParameterOptions params) throws IOException;
    void closeStorage(ParameterOptions params) throws IOException;
    DataWriter<T> createWriter(int id, ParameterOptions params) throws IOException;
    DataReader<T> createReader(int id, ParameterOptions params) throws IOException;
    DataType<T> getDataType();
}
```

`getDataType()` has a default suitable for `byte[]`; drivers using `String`, `ByteBuffer`, protobuf `ByteString`, or another representation override it.

### Lifecycle ownership

- `addArgs`: load defaults and declare driver CLI options. Do not connect to the backend.
- `parseArgs`: parse driver options and reject invalid combinations before resources are opened.
- `openStorage`: create resources shared across workers and perform required preflight work.
- `createWriter` / `createReader`: create per-worker adapters. Return `null` only when that direction is deliberately unavailable for the selected configuration.
- Writer/reader `close`: release per-worker resources.
- `closeStorage`: release shared resources and tolerate partial initialization.

### Writer contract

The simplest writer implements `Writer<T>.writeAsync(T data)`:

- Return `null` after a synchronous operation completes.
- Return a `CompletableFuture` whose completion represents the documented asynchronous completion point.
- Throw `IOException` for synchronous I/O failures.
- Implement `sync()` when the backend requires flush, commit, or transaction completion.
- Do not add a second timer; the default `Writer` methods record start and completion times.

### Reader contract

The simplest reader implements `Reader<T>.read()` and returns one payload. Backends with callback, batching, or embedded producer timestamps can use or override the specialized reader helpers. End-of-stream should use the behavior expected by the selected reader abstraction, typically `EOFException` for finite sources.

## Recommended project layout

```text
drivers/acmekv/
├── build.gradle
├── README.md
└── src/main/
    ├── java/io/sbk/driver/AcmeKv/
    │   ├── AcmeKv.java
    │   ├── AcmeKvConfig.java
    │   ├── AcmeKvWriter.java
    │   └── AcmeKvReader.java
    └── resources/acmekv.properties
```

Discovery uses simple Java class names. Keep directory, package, public class, resource name, and `-class` spelling consistent with existing drivers.

## Choose a reference driver

| Need | Reference |
|---|---|
| Small synchronous local implementation | `drivers/file` or `drivers/filestream` |
| Async file API | `drivers/asyncfile` |
| S3-compatible HTTP SDK and shutdown handling | `drivers/minio` |
| Message producer/consumer | `drivers/kafka`, `drivers/pulsar`, or `drivers/rabbitmq` |
| Relational operations | `drivers/jdbc` and a concrete SQL driver |
| Custom payload type | `drivers/file`, `drivers/csv`, or `drivers/fdbrecord` |
| Callback reader | Search for implementations of `AbstractCallbackReader` |
| Minimal no-op storage baseline | `drivers/null` |
| End-to-end SBK/PerL queue comparison | `drivers/perlbench` |

Prefer the driver whose SDK and completion semantics resemble the new backend, not merely the driver with the shortest source.

## Add a driver

1. Write a driver specification using [DRIVER_SPECIFICATION.md](DRIVER_SPECIFICATION.md) for non-trivial backends.
2. Copy `drivers/sbktemplate` or a closer existing driver.
3. Add the vendor client dependency to the driver `build.gradle`.
4. Add `include 'drivers:<name>'` to `settings-drivers.gradle`.
5. Add `api project(':drivers:<name>')` to `build-drivers.gradle`.
6. Add any new top-level Java package to `checkstyle/import-control.xml`.
7. Implement storage lifecycle, writers, readers, configuration, and documentation.
8. Add focused unit tests for parsing, key generation, serialization, or error classification that do not require a live service.
9. Run module, full-build, distribution, discovery, and real-backend verification.

The exact procedure is in [AGENT_RECIPES.md](AGENT_RECIPES.md#1-add-a-new-storage-driver).

## Performance and correctness rules

- Do not add `synchronized` blocks or explicit locks to the operation path.
- Reuse clients and buffers where the SDK permits; avoid large per-record allocation.
- Document acknowledgement and durability semantics. “Write completed” can mean queued locally, acknowledged by a broker, replicated, flushed, or committed.
- Preserve payload position/state when using mutable buffers unless consumption is the documented contract.
- Use stable, collision-free keys or stream positions appropriate to concurrent workers.
- Make retry behavior explicit. SDK retries alter observed latency and operation counts.
- Never hide operation failures merely to keep a benchmark running.
- Treat interruption, dispatcher shutdown, and rejected execution during timed teardown according to the clean-shutdown pattern used by MinIO.
- Do not embed credentials in resource defaults, examples, tests, or logs.

## Configuration rules

- Put safe defaults in `src/main/resources/<driver>.properties`.
- Model those properties in a small configuration class.
- Print defaults in option descriptions when practical.
- Validate incompatible reader/writer counts, required endpoints, and numeric ranges in `parseArgs`.
- Keep secrets out of help output; prefer environment variables or secure external configuration where supported.
- Document whether a run creates, truncates, appends, deletes, or reuses backend data.

## Dependency and compatibility rules

Adding a vendor SDK may require an import allow-list entry. It also changes the distribution's pathing-JAR manifest, so use a clean rebuild before runtime testing.

The MinIO client is intentionally pinned at 8.5.17 because later checksum-header behavior can be incompatible with older S3 implementations. Do not upgrade it as a routine dependency refresh.

Do not enable HaloDB without confirming access to its GitHub Packages artifact.

## Verification

```bash
# Fast feedback
./gradlew :drivers:<name>:compileJava
./gradlew :drivers:<name>:check

# Integration with every enabled module
./gradlew check

# Runtime packaging and discovery
./gradlew clean :pathingJar installDist --rerun-tasks
./build/install/sbk/bin/sbk -class <name> -help

# Backend smoke test; use non-production data and credentials
./build/install/sbk/bin/sbk -class <name> <connection-options> \
  -writers 1 -size 1024 -seconds 15
```

When reads are supported, read the records created by the write smoke test. Exercise at least one expected failure such as an invalid endpoint or credentials and confirm that it terminates clearly.

## Driver definition of done

- Both Gradle registration files include the driver.
- Package and class naming allow discovery through `-class`.
- All supported operations have documented completion semantics.
- Configuration defaults and CLI descriptions agree.
- Shared and per-worker resources close during success, failure, and timeout.
- Checkstyle and tests pass for the driver and full project.
- The installed distribution finds the driver.
- Real-backend write/read smoke tests pass.
- The README follows [DOCUMENTATION_GUIDE.md](DOCUMENTATION_GUIDE.md).
