# Driver and topology selection

## The decision that prevents most topology mistakes

Ask two independent questions:

1. Does the driver need an external storage service?
2. How many load-generator hosts are required?

They are not the same question. A single SBK process can benchmark a remote
database, object store, broker, or file service. SBK-GEM is required when SBK
load generation itself must run on multiple hosts.

| Storage location | Load generators | Application |
|---|---:|---|
| In-process or local host | 1 | SBK or SBK-YAL |
| Remote storage service | 1 | SBK or SBK-YAL |
| Any storage reachable by several load hosts | 2+ | SBK-GEM or SBK-GEM-YAL |
| Manually launched SBK clients needing one aggregate | 2+ | standalone SBM plus SBK `GrpcLogger` clients |

## Drivers that do not require an external storage service

These are useful for framework checks, queue/PerL measurements, or local
filesystem and embedded-engine benchmarks.

| Category | Drivers | Local prerequisite |
|---|---|---|
| No-op / framework | `Null`, `PerlBench` | CPU and memory only |
| In-process queues | `ConcurrentQ`, `Conqueue`, `Atomicq`, `Syncq`, `Linkedbq` | CPU and memory only |
| Local files | `File`, `FileStream`, `AsyncFile`, `CSV` | Writable path and enough local disk |
| Embedded stores | `LevelDB`, `RocksDB`, `SQLite` | Writable database path and enough local disk |
| Embedded by default | `Derby`, `H2` | Their defaults are local; driver options may select a server instead |

`Null` confirms harness scheduling and reporting but does not measure storage.
`PerlBench` deliberately stresses SBK's measurement path. Queue drivers measure
their queue implementation, not a network storage system. File and embedded
drivers measure the load-generator host's local resources unless their
configuration explicitly points elsewhere.

## Drivers that require an external service or cluster

The service may run on the same machine, but it must exist independently and be
reachable using driver-specific options.

| Category | Drivers |
|---|---|
| Object/S3 and cloud APIs | `MinIO`, `CephS3`, `OpenIO`, `SeaweedS3`, `Dynamodb` |
| Messaging and streaming | `Activemq`, `Artemis`, `BookKeeper`, `Kafka`, `Nats`, `NatsStream`, `Nsq`, `Pravega`, `Pulsar`, `RabbitMQ`, `RedPanda`, `RocketMQ` |
| Databases and records | `Cassandra`, `Couchbase`, `CouchDB`, `Db2`, `Exasol`, `FdbRecord`, `FoundationDB`, `Jdbc`, `MariaDB`, `MongoDB`, `MsSql`, `MySQL`, `PostgreSQL`, `Redis` |
| Distributed files/query | `HDFS`, `Hive` |
| Search/vector/cache | `ChromaDB`, `Elasticsearch`, `Memcached`, `Solr` |

This classification describes the usual/default deployment. Confirm current
behavior with:

```bash
./build/install/sbk/bin/sbk -class <driver> -help
```

Then read `drivers/<driver>/README.md` and
`drivers/<driver>/src/main/resources/*.properties`. Do not assume option names
are shared between similar backends.

## Before touching an external service

- Resolve the endpoint and test network reachability from every load host.
- Use a dedicated benchmark account with only the required permissions.
- Use an isolated bucket, namespace, topic, table, collection, or prefix.
- Determine whether the driver creates, recreates, truncates, or deletes data.
- Estimate generated data: operation rate multiplied by record size and time.
- Record replication, durability, compression, encryption, and consistency
  settings because they materially change results.
- Obtain explicit authorization before destructive setup or cleanup.
