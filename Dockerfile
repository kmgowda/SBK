##
# Copyright (c) KMG. All Rights Reserved.
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#     http://www.apache.org/licenses/LICENSE-2.0
##

# Building Container

FROM openjdk:8-jre

ENV APP_NAME=sbk

USER 0

COPY ca-certificates/* /usr/local/share/ca-certificates/
RUN update-ca-certificates

COPY --chown=root:root   build/install/sbk   /opt/sbk

WORKDIR /opt/sbk

ENV APP_NAME=sbk

ENTRYPOINT ["/opt/sbk/bin/sbk"]

