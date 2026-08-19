#!/usr/bin/env bash
# Copyright (c) KMG. All Rights Reserved.
# Licensed under the Apache License, Version 2.0.

set -euo pipefail

ROOT=${SBK_RELEASE_ROOT:?SBK_RELEASE_ROOT is required}
VERSION=${SBK_RELEASE_VERSION:?SBK_RELEASE_VERSION is required}
DOCKER_NODE_COUNT=${SBK_RELEASE_DOCKER_NODE_COUNT:?SBK_RELEASE_DOCKER_NODE_COUNT is required}
SSH_READY_ATTEMPTS=${SBK_RELEASE_DOCKER_SSH_READY_ATTEMPTS:?SBK_RELEASE_DOCKER_SSH_READY_ATTEMPTS is required}
INTERRUPTED_EXIT=130
TERMINATED_EXIT=143
SSH_CONTAINER_PORT=22
DIAGNOSTIC_TAIL_LINES=20
POLL_SECONDS=1
FIXTURE_PARENT="$ROOT/build/release-qualification"
mkdir -p "$FIXTURE_PARENT"
FIXTURE_DIR=$(mktemp -d "$FIXTURE_PARENT/docker-gem.XXXXXX")
CONTAINER_PREFIX="sbk-release-gem-${RANDOM}-$$"
CONTAINER_NAMES=("${CONTAINER_PREFIX}-1" "${CONTAINER_PREFIX}-2")
NODE_HOSTS=(127.0.0.1 127.0.0.2)
IMAGE_NAME="sbk-release-gem-fixture:${VERSION}"
SSH_USER=sbk-release
SSH_AUTH_SOCK="$FIXTURE_DIR/ssh-agent.sock"
SSH_AGENT_PID=

cleanup() {
    local status=$?
    trap - EXIT
    local container_name
    for container_name in "${CONTAINER_NAMES[@]}"; do
        docker rm --force "$container_name" >/dev/null 2>&1 || true
    done
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
trap 'exit $INTERRUPTED_EXIT' INT
trap 'exit $TERMINATED_EXIT' TERM

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
docker run --detach --name "${CONTAINER_NAMES[0]}" \
    --add-host host.docker.internal:host-gateway \
    --publish "${NODE_HOSTS[0]}::${SSH_CONTAINER_PORT}" \
    --volume "$FIXTURE_DIR/id_ed25519.pub:/run/sbk/authorized_key:ro" \
    "$IMAGE_NAME" >/dev/null

port_line=$(docker port "${CONTAINER_NAMES[0]}" "${SSH_CONTAINER_PORT}/tcp")
SSH_PORT=${port_line##*:}
if [[ ! $SSH_PORT =~ ^[0-9]+$ ]]; then
    printf 'Unable to determine the Docker fixture SSH port from: %s\n' "$port_line" >&2
    exit 1
fi

docker run --detach --name "${CONTAINER_NAMES[1]}" \
    --add-host host.docker.internal:host-gateway \
    --publish "${NODE_HOSTS[1]}:${SSH_PORT}:${SSH_CONTAINER_PORT}" \
    --volume "$FIXTURE_DIR/id_ed25519.pub:/run/sbk/authorized_key:ro" \
    "$IMAGE_NAME" >/dev/null

KNOWN_HOSTS="$FIXTURE_DIR/known_hosts"
: > "$KNOWN_HOSTS"
if [[ ${#NODE_HOSTS[@]} -ne $DOCKER_NODE_COUNT ]]; then
    printf 'Docker fixture defines %s nodes but release configuration requires %s\n' \
        "${#NODE_HOSTS[@]}" "$DOCKER_NODE_COUNT" >&2
    exit 1
fi
for node_index in "${!NODE_HOSTS[@]}"; do
    node_host=${NODE_HOSTS[$node_index]}
    container_name=${CONTAINER_NAMES[$node_index]}
    node_known_hosts="$FIXTURE_DIR/known-hosts-${node_index}"
    ssh_probe_log="$FIXTURE_DIR/ssh-probe-${node_index}.log"
    ready=false
    for ((attempt = 0; attempt < SSH_READY_ATTEMPTS; attempt++)); do
        if ssh-keyscan -p "$SSH_PORT" "$node_host" > "$node_known_hosts" 2>/dev/null \
                && [[ -s $node_known_hosts ]] \
                && ssh -p "$SSH_PORT" -o BatchMode=yes \
                    -o "UserKnownHostsFile=$node_known_hosts" \
                    "$SSH_USER@$node_host" \
                    'command -v java && java -version' > "$ssh_probe_log" 2>&1; then
            ready=true
            break
        fi
        sleep "$POLL_SECONDS"
    done
    if [[ $ready != true ]]; then
        printf 'Docker GEM fixture node %s did not become SSH/JDK ready\n' "$node_host" >&2
        if [[ -s $ssh_probe_log ]]; then
            printf '%s\n' '--- SSH probe output ---' >&2
            tail -n "$DIAGNOSTIC_TAIL_LINES" "$ssh_probe_log" >&2
        fi
        docker logs "$container_name" >&2 || true
        exit 1
    fi
    cat "$node_known_hosts" >> "$KNOWN_HOSTS"
done

INVENTORY="$FIXTURE_DIR/inventory.properties"
GEM_NODES=$(IFS=,; printf '%s' "${NODE_HOSTS[*]}")
printf 'gem.nodes=%s\n' "$GEM_NODES" > "$INVENTORY"
printf 'gem.user=%s\n' "$SSH_USER" >> "$INVENTORY"
printf 'gem.knownHosts=%s\n' "$KNOWN_HOSTS" >> "$INVENTORY"
printf 'gem.port=%s\n' "$SSH_PORT" >> "$INVENTORY"
printf 'gem.localhost=host.docker.internal\n' >> "$INVENTORY"
export SBK_RELEASE_INVENTORY="$INVENTORY"

printf 'SBK local-docker GEM fixture ready: %s@{%s}:%s (%s clients)\n' \
    "$SSH_USER" "$GEM_NODES" "$SSH_PORT" "${#NODE_HOSTS[@]}"
bash "$ROOT/scripts/release-functional-tests.sh"
