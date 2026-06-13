# SBK Benchmark Runner Skill

> Helps AI agents run SBK performance benchmarks and interpret results for storage systems.

## When to use this skill

Invoke this skill when:
- Running a performance benchmark against a storage backend
- Testing a new or modified driver against a real system
- Comparing performance between different configurations
- Interpreting SBK benchmark output and metrics
- Debugging benchmark failures or unexpected results

## What this skill provides

### Context
- SBK benchmark command structure and required parameters
- Available drivers and their driver-specific flags
- PerL measurement methodology (lock-free, no sampling, percentile math)
- How to construct valid benchmark scenarios
- What the output metrics mean and how to interpret them

### Guidance
- How to build the correct benchmark command for a given scenario
- Common benchmark parameters (writers, readers, size, duration)
- Driver-specific configuration (endpoints, credentials, bucket names)
- How to run smoke tests vs production benchmarks
- How to interpret latency percentiles, throughput, and error rates

### Permissions granted
- Exec permissions: `./build/install/sbk/bin/sbk`
- Read access to: `drivers/<name>/README.md` (for driver-specific examples)
- Write access to: Temporary directories for benchmark output (if needed)

## SBK benchmark command structure

### Basic command pattern

```bash
./build/install/sbk/bin/sbk \
  -class <driver> \
  [driver-specific flags...] \
  -writers <N> \
  [-readers <N>] \
  -size <bytes> \
  -seconds <duration> \
  [-out <logger>]
```

### Common global parameters

| Parameter | Description | Example |
|-----------|-------------|---------|
| `-class` | Driver name (case-insensitive) | `-class minio` |
| `-writers` | Number of concurrent writer threads | `-writers 8` |
| `-readers` | Number of concurrent reader threads | `-readers 4` |
| `-size` | Record size in bytes | `-size 1048576` (1 MiB) |
| `-seconds` | Benchmark duration in seconds | `-seconds 60` |
| `-out` | Logger for output (default: SystemLogger) | `-out CSVLogger` |

## Driver-specific examples

### MinIO / S3-compatible

```bash
# Public MinIO sandbox (no credentials needed)
./build/install/sbk/bin/sbk \
  -class minio \
  -writers 1 \
  -size 100 \
  -seconds 30

# Custom S3 endpoint
./build/install/sbk/bin/sbk \
  -class minio \
  -url https://s3.example.com \
  -bucket my-bucket \
  -key AKIAIOSFODNN7EXAMPLE \
  -secret wJalrXUtnFEMI/K7MDENG/bPxRfiCYEXAMPLEKEY \
  -writers 8 \
  -size 1048576 \
  -seconds 60 \
  -recreate true

# With advanced S3 features
./build/install/sbk/bin/sbk \
  -class minio \
  -url https://s3.example.com \
  -bucket my-bucket \
  -key AKIA... \
  -secret ... \
  -writers 4 \
  -size 1048576 \
  -seconds 60 \
  -part-size 5242880 \
  -checksum crc32c \
  -tagging-enabled \
  -sse-enabled
```

### File system

```bash
# Write benchmark
./build/install/sbk/bin/sbk \
  -class file \
  -file /tmp/sbk.bin \
  -writers 8 \
  -size 1048576 \
  -seconds 60

# Read benchmark
./build/install/sbk/bin/sbk \
  -class file \
  -file /tmp/sbk.bin \
  -readers 4 \
  -size 1048576 \
  -seconds 60
```

### Redis

```bash
./build/install/sbk/bin/sbk \
  -class redis \
  -host localhost \
  -port 6379 \
  -writers 8 \
  -size 1024 \
  -seconds 60
```

## Benchmark scenarios

### Smoke test (quick validation)

Purpose: Verify driver works, no performance claims

```bash
./build/install/sbk/bin/sbk \
  -class <driver> \
  -writers 1 \
  -size 1024 \
  -seconds 15
```

### Throughput test (max performance)

Purpose: Measure maximum throughput under load

```bash
./build/install/sbk/bin/sbk \
  -class <driver> \
  -writers 16 \
  -size 1048576 \
  -seconds 120
```

### Latency test (tail latency focus)

Purpose: Measure tail latencies with moderate load

```bash
./build/install/sbk/bin/sbk \
  -class <driver> \
  -writers 4 \
  -size 4096 \
  -seconds 180
```

### Mixed read/write

Purpose: Test concurrent read/write workload

```bash
./build/install/sbk/bin/sbk \
  -class <driver> \
  -writers 8 \
  -readers 4 \
  -size 1048576 \
  -seconds 60
```

## Interpreting output

### Key metrics

SBK outputs the following metrics:

- **Records/sec**: Throughput (operations per second)
- **MB/sec**: Throughput in megabytes per second
- **Avg Latency**: Average operation latency in microseconds
- **Min/Max Latency**: Minimum and maximum observed latencies
- **Percentiles**: p50, p75, p90, p95, p99, p99.9, p99.99 (17+ percentiles total)
- **Invalid latencies**: Count of measurements discarded as invalid
- **Discarded ranges**: Count of measurements below/above discard thresholds

### Percentile interpretation

- **p50 (median)**: Half of operations completed faster than this
- **p95**: 95% of operations completed faster than this (common SLA target)
- **p99**: 99% of operations completed faster than this (strict SLA target)
- **p99.9**: 99.9% of operations completed faster than this (tail latency focus)
- **p99.99**: 99.99% of operations completed faster than this (extreme tail)

**Good benchmarks**: p99 latency is within 2-3x of p50 latency (predictable tail)
**Problematic benchmarks**: p99 latency is 10x+ of p50 latency (unpredictable tail)

### Throughput vs latency tradeoff

- Higher writer count → higher throughput, potentially higher latency
- Larger record size → higher MB/sec, potentially lower records/sec
- Longer duration → more stable measurements, less warmup noise

## Common benchmark failures

### Connection refused / timeout

**Cause**: Wrong endpoint, port, or network issue

**Fix**:
- Verify endpoint is correct: `curl -sk https://<host>:<port>/`
- Check firewall rules
- For S3: verify port (common: 443, 9000, 9021 for Dell ECS)

### Authentication failed

**Cause**: Wrong credentials, expired credentials, or permission issue

**Fix**:
- Verify access key and secret key
- Check IAM/user permissions (for AWS S3)
- For S3: verify region setting

### Bucket/table not found

**Cause**: Bucket doesn't exist or wrong name

**Fix**:
- Use `-recreate true` to auto-create (if driver supports it)
- Pre-create bucket via vendor UI
- Verify bucket name spelling

### Driver not found

**Cause**: Driver not in distribution or misspelled

**Fix**:
- Run `./gradlew installDist` to rebuild distribution
- Check `sbk -help` for available drivers
- Verify driver name spelling

### Out of memory

**Cause**: JVM heap too small for workload

**Fix**:
- Increase heap: `export JAVA_OPTS="-Xmx4g"`
- Reduce writer count or record size

## Best practices

### Before benchmarking

1. **Build fresh distribution**: `./gradlew clean installDist`
2. **Verify driver loads**: `sbk -help | grep <driver>`
3. **Test with small workload first**: `-writers 1 -size 1024 -seconds 15`
4. **Check backend health**: Verify storage system is running and responsive

### During benchmarking

1. **Use appropriate duration**: 30-60 seconds for smoke tests, 120+ seconds for production benchmarks
2. **Monitor system resources**: CPU, memory, network, disk I/O
3. **Isolate the system**: Avoid running other workloads during benchmark
4. **Run multiple iterations**: Take median of 3+ runs for stability

### After benchmarking

1. **Check error rate**: Should be 0 or very low (< 0.1%)
2. **Review percentiles**: Look for tail latency outliers
3. **Compare to baseline**: If available, compare to previous runs
4. **Document configuration**: Record all parameters for reproducibility

## Loggers

### SystemLogger (default)

Prints to stdout with periodic updates and final summary.

### CSVLogger

Writes results to CSV file for analysis:

```bash
./build/install/sbk/bin/sbk \
  -class minio \
  -writers 8 \
  -size 1048576 \
  -seconds 60 \
  -out CSVLogger \
  -output /tmp/benchmark-results.csv
```

### PrometheusLogger

Exports metrics via JMX for Prometheus scraping (requires `-jmxExport=true` configuration).

## Related documentation

- <ref_file file="/root/projects/SBK/AGENTS.md" /> - §2: Build, run, and verify
- <ref_file file="/root/projects/SBK/README.md" /> - End-user manual
- <ref_file file="/root/projects/SBK/docs/sbk-internals.md" /> - PerL measurement methodology
- <ref_file file="/root/projects/SBK/drivers/minio/README.md" /> - Example driver documentation
