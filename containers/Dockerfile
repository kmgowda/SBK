# Copyright (c) KMG. All Rights Reserved.
# Licensed under the Apache License, Version 2.0.

ARG SBK_RUNTIME_IMAGE=eclipse-temurin:25-jdk-ubi10-minimal
FROM ${SBK_RUNTIME_IMAGE}

ARG SBK_ROLE
ARG SBK_DRIVER
ARG SBK_VERSION
ARG SBK_REVISION=unknown
ARG SBK_JAVA_REQUIRE_JDK=true
ARG SBK_JAVA_HOME_PATH=/opt/java/openjdk

LABEL org.opencontainers.image.title="SBK ${SBK_ROLE} (${SBK_DRIVER})" \
      org.opencontainers.image.description="Driver-scoped Storage Benchmark Kit runtime" \
      org.opencontainers.image.source="https://github.com/kmgowda/SBK" \
      org.opencontainers.image.version="${SBK_VERSION}" \
      org.opencontainers.image.revision="${SBK_REVISION}" \
      org.opencontainers.image.licenses="Apache-2.0"

ENV SBK_ROLE=${SBK_ROLE} \
    SBK_JAVA_HOME=${SBK_JAVA_HOME_PATH} \
    SBK_JAVA_REQUIRE_JDK=${SBK_JAVA_REQUIRE_JDK} \
    SBK_JAVA_INSTALL=false

COPY common/ /opt/sbk/
COPY roles/${SBK_ROLE}/ /opt/sbk/
COPY drivers/${SBK_DRIVER}/ /opt/sbk/
COPY launchers/${SBK_ROLE}/${SBK_DRIVER}/ /opt/sbk/
COPY container-entrypoint.sh /usr/local/bin/sbk-container-entrypoint

RUN chmod 0555 /usr/local/bin/sbk-container-entrypoint /opt/sbk/bin/* \
    && mkdir -p /data /opt/sbk/.cache \
    && chown -R 1001:0 /data /opt/sbk/.cache \
    && chmod -R g=u /data /opt/sbk/.cache

USER 1001
WORKDIR /data
VOLUME ["/data"]
EXPOSE 9717 9718 9719 9720

ENTRYPOINT ["/usr/local/bin/sbk-container-entrypoint"]
