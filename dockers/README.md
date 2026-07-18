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

# SBK container images

This directory contains Dockerfiles for the aggregate SBK launcher, individual driver distributions, and SBM. Build context must be the repository root because the definitions copy project files outside `dockers/`.

## Build locally

From the repository root:

```bash
docker build -f dockers/sbk -t sbk:local .
docker build -f dockers/sbk-file -t sbk-file:local .
docker build -f dockers/sbm -t sbm:local .
```

Run a local-file smoke test with a mounted data directory:

```bash
docker run --rm \
  -v /tmp/sbk-data:/data \
  sbk:local \
  -class file -file /data/sbk.bin -writers 1 -size 4096 -seconds 15
```

Use explicit image tags in repeatable experiments. `latest` is convenient for local work but does not identify the code or dependencies used.

## Networking

Remote drivers must be able to resolve and reach their backend from inside the container. `localhost` inside a container refers to that container, not normally the host. For SBM, publish the gRPC and metrics ports required by the selected configuration, for example:

```bash
docker run --rm -p 9717:9717 -p 9719:9719 sbm:local
```

Only expose services on trusted interfaces unless authentication and transport security are supplied externally.

## Image publication

Building an image does not publish it. Login, tag, and push are maintainer-controlled release operations and should use the repository's release policy. Never place registry credentials in Dockerfiles, build arguments committed to source, or documentation transcripts.

Some Dockerfiles may exist for drivers disabled in the aggregate Gradle build. Confirm `settings-drivers.gradle`, `build-drivers.gradle`, and the Dockerfile's build stages before assuming an image is currently supported.

See the [root README](../README.md), [driver guide](../docs/DRIVER_GUIDE.md), and [Grafana guide](../grafana/README.md).
