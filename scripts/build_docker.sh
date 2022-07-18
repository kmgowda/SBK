#! /bin/bash
##
# Copyright (c) KMG. All Rights Reserved.
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#     http://www.apache.org/licenses/LICENSE-2.0
##

set -ex

: ${DOCKER_REPOSITORY?"You must export DOCKER_REPOSITORY"}
: ${IMAGE_TAG?"You must export IMAGE_TAG"}
ROOT_DIR=$(dirname $0)/..

docker build -f ${ROOT_DIR}/Dockerfile ${ROOT_DIR} --tag ${IMAGE_TAG}

docker push ${DOCKER_REPOSITORY}/sbk:${IMAGE_TAG}

# Example
# tag the image first
#docker tag sbk:0.991 kmgowda/sbk:0.991
# push the image docker hub
#docker push kmgowda/sbk:0.991