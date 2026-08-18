<!--
Copyright (c) KMG. All Rights Reserved.
Licensed under the Apache License, Version 2.0.
-->

# SBK Release Qualification

SBK has one authoritative release gate. It builds clean distributions, runs
the normal verification suite, exercises every logger family through the SBK
suite and standalone service launchers, verifies PerL concurrency, checks generated API documentation, and
records the commit and archive checksums that were qualified.

## Profiles

Use the `local` profile while developing:

```bash
./gradlew clean releaseQualification \
  -PreleaseProfile=local \
  --no-daemon --rerun-tasks
```

The `ci` profile adds Maven-local publication verification. The `release`
profile additionally runs the JMH performance gates and requires real remote
SBK-GEM hosts:

```bash
./gradlew clean releaseQualification \
  -PreleaseProfile=release \
  -PreleaseInventory=/secure/sbk-release-inventory.properties \
  --no-daemon --rerun-tasks
```

The release profile never treats unavailable mandatory infrastructure as a
successful skip. Missing inventory, hosts, credentials, or required tools fail
the gate.

The `SBK Release Qualification` GitHub Actions workflow provides the same
entry point. Its `ci` profile uses a GitHub-hosted runner. Its `release`
profile requires a self-hosted Linux runner labeled `sbk-release`, because
that machine must have network and SSH access to the private GEM inventory.

## Release inventory

Keep the inventory outside the repository. It contains locations, not secret
defaults. SSH keys and passphrases remain in the SSH agent or environment.

```properties
gem.nodes=sbk-node-1,sbk-node-2
gem.user=sbk
gem.knownHosts=/secure/sbk-release-known-hosts
```

`gem.knownHosts` is a path on the qualification runner, not the file contents.
The nodes must permit non-interactive SSH, reach the selected backend, and
connect back to the embedded SBM endpoint. SBK-GEM performs its normal version,
copy, Java, execution, aggregation, and cleanup checks.

## Automated functional coverage

The black-box harness runs binaries from the generated install trees and
checks:

- launcher presence for the complete suite, version output for SBK, SBK-YAL,
  SBM, SBK-GEM, and SBK-GEM-YAL, and the Local Web Console health contract;
- deterministic File writes with `SystemLogger`, `Sl4jLogger`, `CSVLogger`,
  `PrometheusLogger`, and `WebLogger`;
- CSV header/total output, Prometheus HTTP metrics, Web Console health/run
  registration, default board naming, and logger lifecycle messages;
- File EOF termination before the configured duration;
- invalid storage, missing gRPC host, missing YML, and malformed YML failures;
- SBK-YAL argument mapping and logger overrides;
- real `GrpcLogger` clients against child SBM processes using both
  `SbmPrometheusLogger` and `SbmWebLogger`; and
- both GEM logger families plus SBK-GEM-YAL when the release inventory is
  supplied.

All child processes have bounded startup and execution times. Logs and a test
summary are written below `build/reports/release-qualification/`.

## Configuration

Safe harness defaults live in
`gradle/release-qualification.properties`. Override them with Gradle properties
such as `-PreleaseRecords=50000`. These settings configure only the test
harness; module runtime defaults remain in their existing authoritative
properties files.

`releaseRequireCleanTree` defaults to `true`. A developer may disable it for a
local diagnostic run, but a release candidate must use the default:

```bash
./gradlew releaseFunctionalTest \
  -PreleaseProfile=local \
  -PreleaseRequireCleanTree=false
```

## Results and release rule

Successful qualification creates:

```text
build/reports/release-qualification/
├── qualification.json
├── functional-summary.json
├── functional-results.tsv
└── logs/
```

`qualification.json` contains the version, Git commit, profile, Java runtime,
and SHA-256 checksum of every application ZIP/TAR. A release is qualified only
when the command exits zero, no mandatory test was skipped, and the published
artifacts have the recorded checksums.

The deterministic gate validates the SBK framework and enabled-driver build.
Runtime certification of every enabled driver still requires its actual
backend. Do not claim all-driver runtime certification unless each enabled
driver has a recorded backend result.
