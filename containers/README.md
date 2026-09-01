# SBK driver-scoped Docker build and operations guide

This directory is the source of truth for building and running SBK with
Docker. It deliberately covers Docker only; SBK does not generate Kubernetes
manifests, Helm charts, or Kubernetes release artifacts.

The build produces small, purpose-specific images instead of copying the
aggregate 52-driver distribution into every container:

| Image role | Driver scoped | Entrypoint | Purpose |
|---|---:|---|---|
| `sbk` | yes | `sbk` | CLI benchmark with one selected storage driver |
| `sbk-yal` | yes | `sbk-yal` | YML-driven benchmark with one selected driver |
| `sbm` | no | `sbm` | Central gRPC benchmark monitor/aggregator |
| `sbk-gem-yal` | yes | `sbk-gem-yal` | YML-driven distributed controller with one selected remote driver |

The Docker build is standalone and opt-in. `releasecheck`, `publish`, and the
release-publication workflow do not depend on `prepareContainerBuild` or
`verifyContainerBuild`, and therefore do not build or publish these images.

## Prerequisites

- JDK 25 and the repository Gradle wrapper
- Docker Engine 24 or later
- Docker Buildx with the `docker-container` driver for multi-platform output
- Enough local disk for the selected Java runtime base layers and BuildKit cache

Check the tools before starting:

```bash
java -version
docker version
docker buildx version
docker buildx inspect --bootstrap
```

Never put storage, SSH, registry, or management credentials in an image,
Dockerfile, build argument, tag, or committed YML file. Supply benchmark
credentials at runtime through environment variables, mounted files, or a
local secret manager.

## Build one driver family

Prepare and structurally verify the contexts without creating any image:

```bash
./gradlew verifyContainerBuild -PcontainerDrivers=minio
```

The generated context and Bake plan are written to
`build/generated/sbk-containers/`. Inspect the exact targets and arguments
before building:

```bash
docker buildx bake \
  --file build/generated/sbk-containers/docker-bake.hcl \
  --print selected
```

Build all three MinIO/S3-capable application roles plus the common SBM image
into the local Docker engine:

```bash
docker buildx bake \
  --file build/generated/sbk-containers/docker-bake.hcl \
  selected --load
```

The default local tags are:

```text
sbk-local/sbk:10.7-minio
sbk-local/sbk-yal:10.7-minio
sbk-local/sbk-gem-yal:10.7-minio
sbk-local/sbm:10.7
```

`containers/build.sh` performs the prepare-and-build sequence. Its defaults
are `minio` and `--load`:

```bash
containers/build.sh
SBK_CONTAINER_DRIVERS=file,minio containers/build.sh
```

## Build selected or all drivers

Use a comma-separated, case-insensitive selection:

```bash
./gradlew verifyContainerBuild -PcontainerDrivers=file,minio,kafka
```

Use `all` only when a complete image matrix is genuinely required:

```bash
./gradlew verifyContainerBuild -PcontainerDrivers=all
```

Preparing `all` creates contexts and metadata, not images. Building the
generated `selected` group for all drivers creates three images per driver
plus one SBM image and can consume substantial time and disk. Prefer a bounded
driver selection for development and CI.

By default, `sbk`, `sbk-yal`, and `sbm` use the Java 25 JRE image. The
`sbk-gem-yal` uses a complete Oracle OpenJDK 25 image because managed remote
Java provisioning requires `jlink` and the JDK module files when a target host
lacks Java 25. The generated image explicitly enables the launcher's
runtime-only validation for JRE roles; non-container launchers require a
complete JDK by default.

Override the local repository prefix and pin the JRE and JDK bases by digest
when preparing controlled builds:

```bash
./gradlew verifyContainerBuild \
  -PcontainerDrivers=minio \
  -PcontainerRepository=registry.example.com/perf/sbk \
  -PcontainerJreRuntimeImage=eclipse-temurin:25-jre-ubi10-minimal@sha256:REPLACE \
  -PcontainerJdkRuntimeImage=container-registry.oracle.com/java/openjdk:25@sha256:REPLACE
```

`-PcontainerRuntimeImage=<image>` remains available when every role must use
one common base, and takes precedence over both role-specific properties. When
a custom base uses a different Java home, set `-PcontainerJreJavaHome=<path>`
or `-PcontainerJdkJavaHome=<path>`; `-PcontainerJavaHome=<path>` overrides both.

Building with `--push` publishes images and requires explicit maintainer
authorization and registry authentication. It is intentionally not part of
the SBK release process.

## Build a single target

The target names are deterministic:

```text
sbk-<driver>
sbk-yal-<driver>
sbk-gem-yal-<driver>
sbm
```

Examples:

```bash
docker buildx bake -f build/generated/sbk-containers/docker-bake.hcl sbk-minio --load
docker buildx bake -f build/generated/sbk-containers/docker-bake.hcl sbk-yal-file --load
docker buildx bake -f build/generated/sbk-containers/docker-bake.hcl sbm --load
```

## Run direct SBK

Show the installed driver and logger options:

```bash
docker run --rm sbk-local/sbk:10.7-minio -class minio -help
```

Run a MinIO/ECS write benchmark. `/data` is the non-root working directory;
mount it when results or generated files must survive the container:

```bash
docker run --rm \
  -v "$PWD/results:/data/results" \
  -e AWS_ACCESS_KEY_ID \
  -e AWS_SECRET_ACCESS_KEY \
  sbk-local/sbk:10.7-minio \
  -class minio \
  -url http://10.236.66.181:9020 \
  -accesskey "$AWS_ACCESS_KEY_ID" \
  -secretkey "$AWS_SECRET_ACCESS_KEY" \
  -bucket sbk-docker-test \
  -prefix qualification/ \
  -writers 2 -size 1048576 -records 100 \
  -endpoint-metrics true
```

For the complete ECS/ObjectScale qualification and workload procedure, use
[`../drivers/minio/docs/ECS_OBS_BENCHMARK_RUNBOOK.md`](../drivers/minio/docs/ECS_OBS_BENCHMARK_RUNBOOK.md).

## Run SBK-YAL

The YML file must describe the driver built into the image:

```bash
docker run --rm \
  -v "$PWD/benchmark.yml:/work/benchmark.yml:ro" \
  -v "$PWD/results:/data/results" \
  -e AWS_ACCESS_KEY_ID \
  -e AWS_SECRET_ACCESS_KEY \
  sbk-local/sbk-yal:10.7-minio /work/benchmark.yml
```

An image does not dynamically gain another driver from the YML file. A
`minio` image cannot execute `-class kafka`; build the matching driver image.

## Run SBM

Publish the gRPC and metrics ports explicitly:

```bash
docker run --rm --name sbm \
  -p 9717:9717 -p 9719:9719 \
  sbk-local/sbm:10.7 \
  -class File -action w -port 9717 -context 9719/metrics
```

Remote SBK clients must use a host/address reachable from their own network;
container loopback is not the Docker host.

## Run SBK-GEM-YAL

Mount the benchmark YML and SSH material read-only. The controller image holds
only the driver selected at build time and deploys that same driver closure to
remote nodes:

```bash
docker run --rm \
  -v "$PWD/drivers/minio/examples/sbk-gem-ecs.yml:/work/sbk-gem.yml:ro" \
  -v "$HOME/.ssh:/home/default/.ssh:ro" \
  -e AWS_ACCESS_KEY_ID \
  -e AWS_SECRET_ACCESS_KEY \
  sbk-local/sbk-gem-yal:10.7-minio /work/sbk-gem.yml
```

Driver-scoped GEM images support the default minimal deployment policy. Do
not use `-fullcopy true`: a full aggregate SBK distribution is intentionally
absent from these images. The remote nodes must be able to connect back to the
advertised SBM address; use the existing `-sbmaddress` override when Docker's
selected route is not reachable by the nodes.

## Stopping containers and preserving results

Prefer bounded `-records` or `-seconds` workloads and allow SBK to finish on
its own. Normal benchmark completion is the only reliable way to guarantee the
final aggregate report.

`docker stop` sends `SIGTERM` to the Java process because the container
entrypoint uses `exec`. Stopping an active benchmark is therefore an aborted
run, not a successful early completion. SBK enforces its absolute five-second
cleanup deadline; if workers, drivers, loggers, or pending measurements do not
finish within that deadline, the final aggregate result can be absent or
incomplete and the container can exit with status 143.

Use a Docker stop timeout of at least six seconds so SBK has time to print its
cleanup diagnostics:

```bash
docker stop --timeout 6 <container-name>
```

Increasing Docker's timeout does **not** extend SBK's internal five-second
cleanup deadline and does not make an interrupted result valid. Reject the run
if the log reports forced exit, incomplete aggregate results, or lacks the
`Total` result line. Re-run it with a bounded workload instead.

## Multi-platform images

`--load` supports only one local platform. Validate a multi-platform build
without publishing by exporting an OCI archive:

```bash
docker buildx bake -f build/generated/sbk-containers/docker-bake.hcl \
  sbk-minio \
  --set sbk-minio.platform=linux/amd64,linux/arm64 \
  --set sbk-minio.output=type=oci,dest=build/sbk-minio-10.7.oci.tar
```

Publish only after explicit authorization:

```bash
docker login registry.example.com
docker buildx bake -f build/generated/sbk-containers/docker-bake.hcl \
  sbk-minio \
  --set sbk-minio.platform=linux/amd64,linux/arm64 \
  --push
```

## Layer and size model

`prepareContainerBuild` derives every driver closure from Gradle's verified
`worker-runtime/<driver>.properties` metadata. The generated context separates:

1. `common/`: libraries shared by every enabled driver;
2. `roles/`: SBK-YAL, SBM, or GEM-YAL application dependencies;
3. `drivers/`: only the selected driver's remaining dependencies and GEM
   worker-runtime metadata; and
4. `launchers/`: a generated pathing JAR for the exact role/driver union.

This prevents unrelated database, messaging, and object-store SDKs from
entering a driver image while preserving reusable layers across images built
from the same context. `verifyContainerBuild` reads every generated pathing
manifest and proves that each named dependency exists in the assembled layer
set.

## Validation checklist

Before sharing an image or using it for benchmark evidence:

```bash
./gradlew verifyContainerBuild -PcontainerDrivers=<drivers>
docker buildx bake -f build/generated/sbk-containers/docker-bake.hcl --print selected
docker buildx bake -f build/generated/sbk-containers/docker-bake.hcl selected --load
docker run --rm <sbk-image> -class <driver> -help
docker run --rm <sbk-yal-image> /work/smoke.yml
docker run --rm <sbm-image> -help
docker run --rm <sbk-gem-yal-image> /work/smoke.yml
docker image inspect <image> --format '{{.Config.User}} {{.Config.WorkingDir}}'
```

Then run the controlled backend qualification required by the selected driver.
An image that starts and prints help is packaging evidence, not storage
correctness or performance evidence.

## Troubleshooting

| Symptom | Cause and action |
|---|---|
| Driver not found | The image and `-class`/YML driver differ; build the matching driver target. |
| `NoClassDefFoundError` | Regenerate with `./gradlew clean verifyContainerBuild --rerun-tasks`; do not manually copy JARs into the context. |
| Permission denied under `/data` | Mount a host directory writable by UID 1001 or use a Docker-managed volume. |
| SBM clients cannot register | Publish port 9717 and advertise an address reachable outside the container network. |
| GEM cannot use SSH keys | Mount keys and known-hosts read-only at the home expected by the image user, or configure the YML explicitly. |
| GEM rejects `-fullcopy true` | Use the default minimal-copy policy or the aggregate non-container distribution. |
| Buildx reports multiple platforms with `--load` | Export OCI output or push; the local Docker image store loads a single platform. |
| `docker stop` exits 143 or has no `Total` line | The benchmark was interrupted; reject its results and re-run with bounded `-records` or `-seconds`. |
| Role override reports a missing launcher | The selected role is not present in that image; run the role-specific tag generated by Bake. |

## Maintainer invariants

- Do not add these tasks to `releasecheck`, `publish`, or release workflows.
- Do not copy the aggregate `build/install/sbk/lib` directory wholesale.
- Do not add credentials to build inputs or generated metadata.
- Preserve UID 1001, the `/data` working directory, and explicit port mapping.
- Keep the JRE and JDK base images on Java 25 and pin them by digest in controlled CI.
- Do not add Kubernetes files as part of this Docker procedure.
