# Copyright (c) KMG. All Rights Reserved.
# Licensed under the Apache License, Version 2.0.

FROM eclipse-temurin:25-jdk-noble@sha256:735baf2edc6cd6485240144a84fa4db142b9a6f47b4eb4080f31058d200f9813

RUN apt-get update \
    && apt-get install --yes --no-install-recommends openssh-server \
    && rm -rf /var/lib/apt/lists/* \
    && useradd --create-home --shell /bin/bash sbk-release \
    && install -d -m 0755 /run/sshd \
    && ln -s "$JAVA_HOME/bin/java" /usr/local/bin/java

COPY entrypoint.sh /usr/local/bin/sbk-release-entrypoint
RUN chmod 0755 /usr/local/bin/sbk-release-entrypoint

EXPOSE 22
ENTRYPOINT ["/usr/local/bin/sbk-release-entrypoint"]
