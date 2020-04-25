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
FROM gradle:4.10-jdk8 as GradleBuilder
USER root

COPY ca-certificates/* /usr/local/share/ca-certificates/
RUN update-ca-certificates

ENV APP_NAME=sbk
ENV APP_HOME=/opt/${APP_NAME}
WORKDIR /opt/sbk

COPY --chown=root:root gradle ${APP_HOME}/gradle
COPY --chown=root:root build.gradle ${APP_HOME}/build.gradle
COPY --chown=root:root gradle.properties ${APP_HOME}/gradle.properties
COPY --chown=root:root settings.gradle ${APP_HOME}/settings.gradle
COPY --chown=root:root gradlew ${APP_HOME}/gradlew
COPY --chown=root:root checkstyle ${APP_HOME}/checkstyle
COPY --chown=root:root sbk-api ${APP_HOME}/sbk-api

# Copy the SBK storage drivers
COPY --chown=root:root driver-hdfs ${APP_HOME}/driver-hdfs
COPY --chown=root:root driver-bookkeeper ${APP_HOME}/driver-bookkeeper
COPY --chown=root:root driver-concurrentq ${APP_HOME}/driver-concurrentq
COPY --chown=root:root driver-file ${APP_HOME}/driver-file
COPY --chown=root:root driver-kafka ${APP_HOME}/driver-kafka
COPY --chown=root:root driver-pravega ${APP_HOME}/driver-pravega
COPY --chown=root:root driver-rabbitmq ${APP_HOME}/driver-rabbitmq
COPY --chown=root:root driver-rocketmq ${APP_HOME}/driver-rocketmq
COPY --chown=root:root driver-pulsar ${APP_HOME}/driver-pulsar

ENV GRADLE_USER_HOME=/opt/SBK
RUN gradle  build --no-daemon --info --stacktrace

# Runtime Container
FROM openjdk:8-jre
ENV APP_NAME=sbk
ENV APP_HOME=/opt/${APP_NAME}

COPY --from=GradleBuilder ${APP_HOME}/build/distributions/${APP_NAME}.tar /opt/${APP_NAME}.tar

RUN tar -xvf /opt/${APP_NAME}.tar -C /opt/.

ENTRYPOINT ["/opt/sbk/bin/sbk"]
