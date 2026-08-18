#!/usr/bin/env bash
# Copyright (c) KMG. All Rights Reserved.
# Licensed under the Apache License, Version 2.0.

set -euo pipefail

ROOT=${SBK_RELEASE_ROOT:?SBK_RELEASE_ROOT is required}
VERSION=${SBK_RELEASE_VERSION:?SBK_RELEASE_VERSION is required}
FIXTURE_PARENT="$ROOT/build/release-qualification"
mkdir -p "$FIXTURE_PARENT"
FIXTURE_DIR=$(mktemp -d "$FIXTURE_PARENT/docker-gem.XXXXXX")
CONTAINER_NAME="sbk-release-gem-${RANDOM}-$$"
IMAGE_NAME="sbk-release-gem-fixture:${VERSION}"
SSH_USER=sbk-release
SSH_AUTH_SOCK="$FIXTURE_DIR/ssh-agent.sock"
SSH_AGENT_PID=

cleanup() {
    local status=$?
    trap - EXIT
    docker rm --force "$CONTAINER_NAME" >/dev/null 2>&1 || true
    if [[ -n ${SSH_AGENT_PID:-} ]]; then
        kill "$SSH_AGENT_PID" >/dev/null 2>&1 || true
    fi
    case "$FIXTURE_DIR" in
        "$FIXTURE_PARENT"/docker-gem.*)
            rm -rf -- "$FIXTURE_DIR"
            ;;
    esac
    return "$status"
}
trap cleanup EXIT
trap 'exit 130' INT
trap 'exit 143' TERM

ssh-keygen -q -t ed25519 -N '' -C sbk-release-qualification -f "$FIXTURE_DIR/id_ed25519"

agent_output=$(ssh-agent -a "$SSH_AUTH_SOCK" -s)
SSH_AGENT_PID=$(printf '%s\n' "$agent_output" \
    | sed -n 's/^SSH_AGENT_PID=\([0-9][0-9]*\);.*/\1/p')
if [[ -z $SSH_AGENT_PID ]]; then
    printf 'Unable to start the release qualification SSH agent\n' >&2
    exit 1
fi
export SSH_AUTH_SOCK SSH_AGENT_PID
ssh-add "$FIXTURE_DIR/id_ed25519" >/dev/null

docker build --tag "$IMAGE_NAME" "$ROOT/scripts/release-gem-docker"
docker run --detach --name "$CONTAINER_NAME" \
    --add-host host.docker.internal:host-gateway \
    --publish 127.0.0.1::22 \
    --volume "$FIXTURE_DIR/id_ed25519.pub:/run/sbk/authorized_key:ro" \
    "$IMAGE_NAME" >/dev/null

port_line=$(docker port "$CONTAINER_NAME" 22/tcp)
SSH_PORT=${port_line##*:}
if [[ ! $SSH_PORT =~ ^[0-9]+$ ]]; then
    printf 'Unable to determine the Docker fixture SSH port from: %s\n' "$port_line" >&2
    exit 1
fi

KNOWN_HOSTS="$FIXTURE_DIR/known_hosts"
SSH_PROBE_LOG="$FIXTURE_DIR/ssh-probe.log"
ready=false
for ((attempt = 0; attempt < 60; attempt++)); do
    if ssh-keyscan -p "$SSH_PORT" 127.0.0.1 > "$KNOWN_HOSTS" 2>/dev/null \
            && [[ -s $KNOWN_HOSTS ]] \
            && ssh -p "$SSH_PORT" -o BatchMode=yes \
                -o "UserKnownHostsFile=$KNOWN_HOSTS" \
                "$SSH_USER@127.0.0.1" \
                'command -v java && java -version' > "$SSH_PROBE_LOG" 2>&1; then
        ready=true
        break
    fi
    sleep 1
done
if [[ $ready != true ]]; then
    printf 'Docker GEM fixture did not become SSH/JDK ready\n' >&2
    if [[ -s $SSH_PROBE_LOG ]]; then
        printf '%s\n' '--- SSH probe output ---' >&2
        tail -n 20 "$SSH_PROBE_LOG" >&2
    fi
    docker logs "$CONTAINER_NAME" >&2 || true
    exit 1
fi

INVENTORY="$FIXTURE_DIR/inventory.properties"
printf 'gem.nodes=127.0.0.1\n' > "$INVENTORY"
printf 'gem.user=%s\n' "$SSH_USER" >> "$INVENTORY"
printf 'gem.knownHosts=%s\n' "$KNOWN_HOSTS" >> "$INVENTORY"
printf 'gem.port=%s\n' "$SSH_PORT" >> "$INVENTORY"
printf 'gem.localhost=host.docker.internal\n' >> "$INVENTORY"
export SBK_RELEASE_INVENTORY="$INVENTORY"

printf 'SBK local-docker GEM fixture ready: %s@127.0.0.1:%s\n' "$SSH_USER" "$SSH_PORT"
bash "$ROOT/scripts/release-functional-tests.sh"
