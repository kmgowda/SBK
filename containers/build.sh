#!/bin/sh
# Copyright (c) KMG. All Rights Reserved.
# Licensed under the Apache License, Version 2.0.

set -eu

drivers=${SBK_CONTAINER_DRIVERS:-minio}
output=${SBK_CONTAINER_OUTPUT:---load}

case "$output" in
    --load|--push)
        ;;
    *)
        echo "SBK_CONTAINER_OUTPUT must be --load or --push" >&2
        exit 64
        ;;
esac

./gradlew prepareContainerBuild -PcontainerDrivers="$drivers"
docker buildx bake --file build/generated/sbk-containers/docker-bake.hcl selected $output "$@"
