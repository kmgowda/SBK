#!/usr/bin/env bash
# Copyright (c) KMG. All Rights Reserved.
# Licensed under the Apache License, Version 2.0.

# Stop and reap a watchdog, including its cancellable sleep child.
_release_timeout_stop_watchdog() {
    local watchdog_pid=$1
    if [[ -n $watchdog_pid ]] && kill -0 "$watchdog_pid" 2>/dev/null; then
        kill -TERM "$watchdog_pid" 2>/dev/null
    fi
    if [[ -n $watchdog_pid ]]; then
        wait "$watchdog_pid" 2>/dev/null
    fi
}

# Apply the configured graceful and forced shutdown sequence, then reap.
_release_timeout_stop_command() {
    local command_pid=$1
    local kill_grace_seconds=$2
    if [[ -n $command_pid ]] && kill -0 "$command_pid" 2>/dev/null; then
        kill -INT "$command_pid" 2>/dev/null
        sleep "$kill_grace_seconds"
    fi
    if [[ -n $command_pid ]] && kill -0 "$command_pid" 2>/dev/null; then
        kill -KILL "$command_pid" 2>/dev/null
    fi
    if [[ -n $command_pid ]]; then
        wait "$command_pid" 2>/dev/null
    fi
}

_release_timeout_cleanup_signal() {
    local signal_status=$1
    local command_pid=$2
    local watchdog_pid=$3
    local kill_grace_seconds=$4
    trap '' HUP INT QUIT TERM
    _release_timeout_stop_watchdog "$watchdog_pid"
    _release_timeout_stop_command "$command_pid" "$kill_grace_seconds"
    exit "$signal_status"
}

_release_timeout_restore_trap() {
    local signal_name=$1
    local saved_trap=$2
    trap - "$signal_name"
    if [[ -n $saved_trap ]]; then
        eval "$saved_trap"
    fi
}

# Run a command with a portable watchdog. macOS does not include the GNU
# coreutils timeout command, so release qualification must use Bash and the
# POSIX kill/sleep utilities that are available on both macOS and Linux.
run_with_timeout() {
    local timeout_seconds=$1
    local kill_grace_seconds=$2
    local command_pid=
    local watchdog_pid=
    local saved_hup_trap
    local saved_int_trap
    local saved_quit_trap
    local saved_term_trap
    shift 2

    saved_hup_trap=$(trap -p HUP)
    saved_int_trap=$(trap -p INT)
    saved_quit_trap=$(trap -p QUIT)
    saved_term_trap=$(trap -p TERM)
    trap '_release_timeout_cleanup_signal 129 "$command_pid" "$watchdog_pid" "$kill_grace_seconds"' HUP
    trap '_release_timeout_cleanup_signal 130 "$command_pid" "$watchdog_pid" "$kill_grace_seconds"' INT
    trap '_release_timeout_cleanup_signal 131 "$command_pid" "$watchdog_pid" "$kill_grace_seconds"' QUIT
    trap '_release_timeout_cleanup_signal 143 "$command_pid" "$watchdog_pid" "$kill_grace_seconds"' TERM

    (
        trap - HUP INT QUIT TERM
        exec "$@"
    ) &
    command_pid=$!
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
    watchdog_pid=$!

    wait "$command_pid" 2>/dev/null
    local status=$?
    _release_timeout_stop_watchdog "$watchdog_pid"
    _release_timeout_restore_trap HUP "$saved_hup_trap"
    _release_timeout_restore_trap INT "$saved_int_trap"
    _release_timeout_restore_trap QUIT "$saved_quit_trap"
    _release_timeout_restore_trap TERM "$saved_term_trap"
    return "$status"
}
