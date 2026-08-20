# Copyright (c) KMG. All Rights Reserved.
# Licensed under the Apache License, Version 2.0.

ARG RUNTIME_IMAGE=SBK_RUNTIME_IMAGE
FROM ${RUNTIME_IMAGE}

ARG APPLICATION_VERSION
ARG VCS_REF
ARG BUILD_DATE

LABEL org.opencontainers.image.title="Storage Benchmark Kit" \
      org.opencontainers.image.description="Storage performance benchmarking toolkit" \
      org.opencontainers.image.url="https://github.com/kmgowda/SBK" \
      org.opencontainers.image.source="https://github.com/kmgowda/SBK" \
      org.opencontainers.image.version="${APPLICATION_VERSION}" \
      org.opencontainers.image.revision="${VCS_REF}" \
      org.opencontainers.image.created="${BUILD_DATE}" \
      org.opencontainers.image.licenses="Apache-2.0"

ENV SBK_HOME=/opt/sbk \
    SBK_JAVA_HOME=${JAVA_HOME}

COPY sbk/ /opt/sbk/

RUN mkdir -p /data && chown -R 1001:0 /opt/sbk /data

USER 1001
WORKDIR /data

EXPOSE 9718 9720

ENTRYPOINT ["/opt/sbk/bin/sbk"]
CMD ["-help"]
