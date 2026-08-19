#!/usr/bin/env bash
# Copyright (c) KMG. All Rights Reserved.
# Licensed under the Apache License, Version 2.0.

# Run a command with a portable watchdog. macOS does not include the GNU
# coreutils timeout command, so release qualification must use Bash and the
# POSIX kill/sleep utilities that are available on both macOS and Linux.
run_with_timeout() {
    local timeout_seconds=$1
    local kill_grace_seconds=$2
    shift 2

    (
        trap - HUP INT QUIT TERM
        exec "$@"
    ) &
    local command_pid=$!
    (
        local sleep_pid=
        trap 'if [[ -n $sleep_pid ]]; then kill "$sleep_pid" 2>/dev/null; fi; exit 0' HUP INT TERM
        sleep "$timeout_seconds" &
        sleep_pid=$!
        wait "$sleep_pid" 2>/dev/null
        sleep_pid=
        if kill -0 "$command_pid" 2>/dev/null; then
            kill -INT "$command_pid" 2>/dev/null
            sleep "$kill_grace_seconds" &
            sleep_pid=$!
            wait "$sleep_pid" 2>/dev/null
            sleep_pid=
        fi
        if kill -0 "$command_pid" 2>/dev/null; then
            kill -KILL "$command_pid" 2>/dev/null
        fi
    ) &
    local watchdog_pid=$!

    wait "$command_pid" 2>/dev/null
    local status=$?
    if kill -0 "$watchdog_pid" 2>/dev/null; then
        kill "$watchdog_pid" 2>/dev/null
    fi
    wait "$watchdog_pid" 2>/dev/null
    return "$status"
}
