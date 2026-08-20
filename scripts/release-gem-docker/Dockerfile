# Copyright (c) KMG. All Rights Reserved.
# Licensed under the Apache License, Version 2.0.

ARG SBK_RELEASE_JDK_IMAGE
FROM ${SBK_RELEASE_JDK_IMAGE}

ARG SBK_RELEASE_SSH_USER
ARG SBK_RELEASE_SSH_PORT
ENV SBK_RELEASE_SSH_USER=${SBK_RELEASE_SSH_USER}
ENV SBK_RELEASE_SSH_PORT=${SBK_RELEASE_SSH_PORT}

RUN apt-get update \
    && apt-get install --yes --no-install-recommends openssh-server \
    && rm -rf /var/lib/apt/lists/* \
    && useradd --create-home --shell /bin/bash "$SBK_RELEASE_SSH_USER" \
    && install -d -m 0755 /run/sshd \
    && ln -s "$JAVA_HOME/bin/java" /usr/local/bin/java

COPY entrypoint.sh /usr/local/bin/sbk-release-entrypoint
RUN chmod 0755 /usr/local/bin/sbk-release-entrypoint

EXPOSE ${SBK_RELEASE_SSH_PORT}
ENTRYPOINT ["/usr/local/bin/sbk-release-entrypoint"]
