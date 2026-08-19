#!/usr/bin/env bash
# Copyright (c) KMG. All Rights Reserved.
# Licensed under the Apache License, Version 2.0.

set -u
set -o pipefail

ROOT=${SBK_RELEASE_ROOT:?SBK_RELEASE_ROOT is required}
PROFILE=${SBK_RELEASE_PROFILE:-local}
VERSION=${SBK_RELEASE_VERSION:?SBK_RELEASE_VERSION is required}
RECORDS=${SBK_RELEASE_RECORDS:?SBK_RELEASE_RECORDS is required}
RECORD_SIZE=${SBK_RELEASE_RECORD_SIZE:?SBK_RELEASE_RECORD_SIZE is required}
TOTAL_THROUGHPUT=${SBK_RELEASE_TOTAL_THROUGHPUT_MB_PER_SEC:?SBK_RELEASE_TOTAL_THROUGHPUT_MB_PER_SEC is required}
PROCESS_TIMEOUT=${SBK_RELEASE_PROCESS_TIMEOUT_SECONDS:?SBK_RELEASE_PROCESS_TIMEOUT_SECONDS is required}
STARTUP_TIMEOUT=${SBK_RELEASE_STARTUP_TIMEOUT_SECONDS:?SBK_RELEASE_STARTUP_TIMEOUT_SECONDS is required}
WEB_TIMEOUT_MINUTES=${SBK_RELEASE_WEB_TIMEOUT_MINUTES:?SBK_RELEASE_WEB_TIMEOUT_MINUTES is required}
KILL_GRACE_SECONDS=${SBK_RELEASE_KILL_GRACE_SECONDS:?SBK_RELEASE_KILL_GRACE_SECONDS is required}
PORT_SELECTION_ATTEMPTS=${SBK_RELEASE_PORT_SELECTION_ATTEMPTS:?SBK_RELEASE_PORT_SELECTION_ATTEMPTS is required}
PORT_RANGE_START=${SBK_RELEASE_PORT_RANGE_START:?SBK_RELEASE_PORT_RANGE_START is required}
PORT_RANGE_SIZE=${SBK_RELEASE_PORT_RANGE_SIZE:?SBK_RELEASE_PORT_RANGE_SIZE is required}
SHUTDOWN_ATTEMPTS=${SBK_RELEASE_SHUTDOWN_ATTEMPTS:?SBK_RELEASE_SHUTDOWN_ATTEMPTS is required}
SHUTDOWN_POLL_SECONDS=${SBK_RELEASE_SHUTDOWN_POLL_SECONDS:?SBK_RELEASE_SHUTDOWN_POLL_SECONDS is required}
TERMINATE_GRACE_SECONDS=${SBK_RELEASE_TERMINATE_GRACE_SECONDS:?SBK_RELEASE_TERMINATE_GRACE_SECONDS is required}
EOF_RECORDS=${SBK_RELEASE_EOF_RECORDS:?SBK_RELEASE_EOF_RECORDS is required}
EOF_BENCHMARK_SECONDS=${SBK_RELEASE_EOF_BENCHMARK_SECONDS:?SBK_RELEASE_EOF_BENCHMARK_SECONDS is required}
EOF_MAXIMUM_SECONDS=${SBK_RELEASE_EOF_MAXIMUM_SECONDS:?SBK_RELEASE_EOF_MAXIMUM_SECONDS is required}
SMOKE_BENCHMARK_SECONDS=${SBK_RELEASE_SMOKE_BENCHMARK_SECONDS:?SBK_RELEASE_SMOKE_BENCHMARK_SECONDS is required}
SBM_SETTLE_SECONDS=${SBK_RELEASE_SBM_SETTLE_SECONDS:?SBK_RELEASE_SBM_SETTLE_SECONDS is required}
MINIMUM_REUSED_WEB_RUNS=2
REPORT_DIR=${SBK_RELEASE_REPORT_DIR:-"$ROOT/build/reports/release-qualification"}
WORK_DIR="$ROOT/build/release-qualification"
LOG_DIR="$REPORT_DIR/logs"
RESULTS_FILE="$REPORT_DIR/functional-results.tsv"
FAILURES=0
PASSES=0

SBK="$ROOT/build/install/sbk/bin/sbk"
SBK_YAL="$ROOT/build/install/sbk/bin/sbk-yal"
SBM="$ROOT/sbm/build/install/sbm/bin/sbm"
SBK_GEM="$ROOT/build/install/sbk/bin/sbk-gem"
SBK_GEM_YAL="$ROOT/build/install/sbk/bin/sbk-gem-yal"
WEB_CONSOLE="$ROOT/sbk-web-console/build/install/sbk-web-console/bin/sbk-web-console"

mkdir -p "$WORK_DIR" "$LOG_DIR"
: > "$RESULTS_FILE"

record_result() {
    local name=$1
    local status=$2
    local detail=$3
    detail=${detail//$'\t'/ }
    detail=${detail//$'\n'/ }
    printf '%s\t%s\t%s\n' "$name" "$status" "$detail" >> "$RESULTS_FILE"
    if [[ $status == PASS ]]; then
        PASSES=$((PASSES + 1))
        printf 'PASS: %s\n' "$name"
    else
        FAILURES=$((FAILURES + 1))
        printf 'FAIL: %s -- %s\n' "$name" "$detail" >&2
    fi
}

run_expect() {
    local name=$1
    local expected=$2
    shift 2
    local log="$LOG_DIR/${name}.log"
    timeout --signal=INT --kill-after="${KILL_GRACE_SECONDS}s" "${PROCESS_TIMEOUT}s" "$@" > "$log" 2>&1
    local status=$?
    if [[ $status -ne 0 ]]; then
        record_result "$name" FAIL "exit code $status; see $log"
    elif ! grep -Eq "$expected" "$log"; then
        record_result "$name" FAIL "missing expected output '$expected'; see $log"
    else
        record_result "$name" PASS "$log"
    fi
}

run_reject() {
    local name=$1
    local expected=$2
    shift 2
    local log="$LOG_DIR/${name}.log"
    timeout --signal=INT --kill-after="${KILL_GRACE_SECONDS}s" "${PROCESS_TIMEOUT}s" "$@" > "$log" 2>&1
    local status=$?
    if [[ $status -eq 0 ]]; then
        record_result "$name" FAIL "invalid command exited zero; see $log"
    elif ! grep -Eiq "$expected" "$log"; then
        record_result "$name" FAIL "failure did not explain '$expected'; see $log"
    else
        record_result "$name" PASS "$log"
    fi
}

free_port() {
    local candidate
    local attempts=0
    while [[ $attempts -lt $PORT_SELECTION_ATTEMPTS ]]; do
        # Stay below the usual Linux ephemeral client-port range. Selecting
        # from that range can find a port with no listener that is still not
        # immediately reusable because a prior client connection is in TIME_WAIT.
        candidate=$((PORT_RANGE_START + RANDOM % PORT_RANGE_SIZE))
        if ! (exec 3<>"/dev/tcp/127.0.0.1/$candidate") 2>/dev/null; then
            printf '%s\n' "$candidate"
            return 0
        fi
        attempts=$((attempts + 1))
    done
    return 1
}

wait_for_url() {
    local url=$1
    local attempts=$STARTUP_TIMEOUT
    while [[ $attempts -gt 0 ]]; do
        if curl --fail --silent --show-error "$url" >/dev/null 2>&1; then
            return 0
        fi
        sleep "$SHUTDOWN_POLL_SECONDS"
        attempts=$((attempts - 1))
    done
    return 1
}

wait_for_port() {
    local port=$1
    local pid=$2
    local attempts=$STARTUP_TIMEOUT
    while [[ $attempts -gt 0 ]]; do
        if ! kill -0 "$pid" 2>/dev/null; then
            return 1
        fi
        if (exec 3<>"/dev/tcp/127.0.0.1/$port") 2>/dev/null; then
            exec 3>&-
            return 0
        fi
        sleep "$SHUTDOWN_POLL_SECONDS"
        attempts=$((attempts - 1))
    done
    return 1
}

stop_process() {
    local pid=$1
    if ! kill -0 "$pid" 2>/dev/null; then
        wait "$pid" 2>/dev/null
        return
    fi
    kill -INT "$pid" 2>/dev/null
    local attempts=$SHUTDOWN_ATTEMPTS
    while kill -0 "$pid" 2>/dev/null && [[ $attempts -gt 0 ]]; do
        sleep "$SHUTDOWN_POLL_SECONDS"
        attempts=$((attempts - 1))
    done
    if kill -0 "$pid" 2>/dev/null; then
        kill -TERM "$pid" 2>/dev/null
        sleep "$TERMINATE_GRACE_SECONDS"
    fi
    if kill -0 "$pid" 2>/dev/null; then
        kill -KILL "$pid" 2>/dev/null
    fi
    wait "$pid" 2>/dev/null
}

for executable in "$SBK" "$SBK_YAL" "$SBM" "$SBK_GEM" "$SBK_GEM_YAL" "$WEB_CONSOLE"; do
    if [[ ! -x $executable ]]; then
        record_result "launcher-$(basename "$executable")" FAIL "missing executable $executable"
    fi
done

run_expect launcher-sbk "SBK Version: ${VERSION}" "$SBK" -version
run_expect launcher-sbk-yal "SBK-YAL Version: ${VERSION}" "$SBK_YAL" -version
run_expect launcher-sbm "SBM Version: ${VERSION}" "$SBM" -version
run_expect launcher-sbk-gem "SBK-GEM Version: ${VERSION}" "$SBK_GEM" -version
run_expect launcher-sbk-gem-yal "SBK-GEM-YAL Version: ${VERSION}" "$SBK_GEM_YAL" -version

run_reject sbk-invalid-class "not found|invalid|class" "$SBK" -class DoesNotExist -writers 1 -records 1
run_reject grpc-missing-sbm "requires.*sbm|SBM host" "$SBK" -class file -writers 1 -records 1 -out GrpcLogger

for logger in SystemLogger Sl4jLogger; do
    data_file="$WORK_DIR/sbk-${logger}.dat"
    run_expect "sbk-${logger}" "Total File Writing|SBK Benchmark Shutdown" \
        "$SBK" -class file -file "$data_file" -writers 1 -size "$RECORD_SIZE" \
        -records "$RECORDS" -out "$logger"
done

CSV_FILE="$WORK_DIR/sbk.csv"
run_expect sbk-CSVLogger "CSV Logger Shutdown|SBK Benchmark Shutdown" \
    "$SBK" -class file -file "$WORK_DIR/sbk-csv.dat" -writers 1 -size "$RECORD_SIZE" \
    -records "$RECORDS" -out CSVLogger -csvfile "$CSV_FILE"
if [[ -s $CSV_FILE ]] && grep -q 'Total' "$CSV_FILE"; then
    record_result csv-contract PASS "$CSV_FILE"
else
    record_result csv-contract FAIL "CSV header/total is missing from $CSV_FILE"
fi

EOF_FILE="$WORK_DIR/sbk-eof.dat"
run_expect sbk-eof-prepare "Total File Writing" \
    "$SBK" -class file -file "$EOF_FILE" -writers 1 -size "$RECORD_SIZE" -records "$EOF_RECORDS"
eof_start=$(date +%s)
run_expect sbk-eof-reader "EOF|Total File Reading" \
    "$SBK" -class file -file "$EOF_FILE" -readers 1 -size "$RECORD_SIZE" -seconds "$EOF_BENCHMARK_SECONDS"
eof_elapsed=$(( $(date +%s) - eof_start ))
if [[ $eof_elapsed -lt $EOF_MAXIMUM_SECONDS ]]; then
    record_result eof-lifecycle PASS "reader exited in ${eof_elapsed}s"
else
    record_result eof-lifecycle FAIL "reader took ${eof_elapsed}s after EOF"
fi

PROM_PORT=$(free_port)
PROM_LOG="$LOG_DIR/sbk-PrometheusLogger.log"
timeout --signal=INT --kill-after="${KILL_GRACE_SECONDS}s" "${PROCESS_TIMEOUT}s" \
    "$SBK" -class file -file "$WORK_DIR/sbk-prom.dat" -writers 1 -size "$RECORD_SIZE" \
    -seconds "$SMOKE_BENCHMARK_SECONDS" -out PrometheusLogger \
    -context "$PROM_PORT/metrics" > "$PROM_LOG" 2>&1 &
prom_pid=$!
if wait_for_url "http://127.0.0.1:$PROM_PORT/metrics"; then
    curl --fail --silent "http://127.0.0.1:$PROM_PORT/metrics" > "$WORK_DIR/sbk-prometheus.metrics"
    if grep -Eq 'component="sbk"|component="SBK"' "$WORK_DIR/sbk-prometheus.metrics"; then
        record_result prometheus-endpoint PASS "$WORK_DIR/sbk-prometheus.metrics"
    else
        record_result prometheus-endpoint FAIL "SBK component label is missing"
    fi
else
    record_result prometheus-endpoint FAIL "metrics endpoint did not become ready; see $PROM_LOG"
fi
wait "$prom_pid"
prom_status=$?
if [[ $prom_status -eq 0 ]] && grep -q 'PrometheusLogger Shutdown' "$PROM_LOG"; then
    record_result sbk-PrometheusLogger PASS "$PROM_LOG"
else
    record_result sbk-PrometheusLogger FAIL "exit code $prom_status; see $PROM_LOG"
fi

WEB_PORT=$(free_port)
WEB_LOG="$LOG_DIR/sbk-WebLogger.log"
timeout --signal=INT --kill-after="${KILL_GRACE_SECONDS}s" "${PROCESS_TIMEOUT}s" \
    "$SBK" -class file -file "$WORK_DIR/sbk-web.dat" -writers 1 -size "$RECORD_SIZE" \
    -seconds "$SMOKE_BENCHMARK_SECONDS" -out WebLogger -webopen false -webport "$WEB_PORT" \
    -webtimeoutminutes "$WEB_TIMEOUT_MINUTES" > "$WEB_LOG" 2>&1 &
web_pid=$!
if wait_for_url "http://127.0.0.1:$WEB_PORT/api/v1/health"; then
    web_attempts=$STARTUP_TIMEOUT
    while [[ $web_attempts -gt 0 ]]; do
        curl --fail --silent "http://127.0.0.1:$WEB_PORT/api/v1/runs" > "$WORK_DIR/web-runs.json"
        if grep -q 'SBK File' "$WORK_DIR/web-runs.json"; then
            break
        fi
        sleep "$SHUTDOWN_POLL_SECONDS"
        web_attempts=$((web_attempts - 1))
    done
    if grep -q 'SBK File' "$WORK_DIR/web-runs.json"; then
        record_result web-console-contract PASS "$WORK_DIR/web-runs.json"
    else
        record_result web-console-contract FAIL "default SBK File board was not registered"
    fi
else
    record_result web-console-contract FAIL "Web Console did not become ready; see $WEB_LOG"
fi
wait "$web_pid"
web_status=$?
if [[ $web_status -eq 0 ]] && grep -Eq 'Starting a new SBK Web Console|Using the existing SBK Web Console' "$WEB_LOG"; then
    record_result sbk-WebLogger PASS "$WEB_LOG"
else
    record_result sbk-WebLogger FAIL "exit code $web_status or missing lifecycle message; see $WEB_LOG"
fi

WEB_REUSE_LOG="$LOG_DIR/sbk-WebLogger-reuse.log"
timeout --signal=INT --kill-after="${KILL_GRACE_SECONDS}s" "${PROCESS_TIMEOUT}s" \
    "$SBK" -class file -file "$WORK_DIR/sbk-web-reuse.dat" -writers 1 -size "$RECORD_SIZE" \
    -records "$RECORDS" -out WebLogger -webopen false -webport "$WEB_PORT" \
    -webtimeoutminutes "$WEB_TIMEOUT_MINUTES" > "$WEB_REUSE_LOG" 2>&1
web_reuse_status=$?
curl --fail --silent "http://127.0.0.1:$WEB_PORT/api/v1/runs" > "$WORK_DIR/web-reused-runs.json"
web_run_count=$(grep -o '"runId"' "$WORK_DIR/web-reused-runs.json" | wc -l)
if [[ $web_reuse_status -eq 0 ]] \
        && grep -q 'Using the existing SBK Web Console' "$WEB_REUSE_LOG" \
        && [[ $web_run_count -ge $MINIMUM_REUSED_WEB_RUNS ]]; then
    record_result web-console-reuse PASS "$WORK_DIR/web-reused-runs.json"
else
    record_result web-console-reuse FAIL "status=$web_reuse_status runs=$web_run_count; see $WEB_REUSE_LOG"
fi

YAL_FILE="$WORK_DIR/sbk-release.yml"
printf 'sbkArgs:\n  class: file\n  file: %s\n  writers: 1\n  size: %s\n  records: %s\n' \
    "$WORK_DIR/sbk-yal.dat" "$RECORD_SIZE" "$RECORDS" > "$YAL_FILE"
for logger in SystemLogger Sl4jLogger CSVLogger; do
    extra=()
    if [[ $logger == CSVLogger ]]; then
        extra=(-csvfile "$WORK_DIR/sbk-yal.csv")
    fi
    run_expect "sbk-yal-${logger}" "Merged YAML.*arguments|SBK Benchmark Shutdown" \
        "$SBK_YAL" -f "$YAL_FILE" -out "$logger" "${extra[@]}"
done

YAL_PROM_PORT=$(free_port)
YAL_PROM_LOG="$LOG_DIR/sbk-yal-PrometheusLogger.log"
timeout --signal=INT --kill-after="${KILL_GRACE_SECONDS}s" "${PROCESS_TIMEOUT}s" \
    "$SBK_YAL" -f "$YAL_FILE" -seconds "$SMOKE_BENCHMARK_SECONDS" -out PrometheusLogger \
    -context "$YAL_PROM_PORT/metrics" > "$YAL_PROM_LOG" 2>&1 &
yal_prom_pid=$!
if wait_for_url "http://127.0.0.1:$YAL_PROM_PORT/metrics"; then
    record_result sbk-yal-PrometheusLogger PASS "$YAL_PROM_LOG"
else
    record_result sbk-yal-PrometheusLogger FAIL "metrics endpoint did not become ready; see $YAL_PROM_LOG"
fi
wait "$yal_prom_pid"

YAL_WEB_LOG="$LOG_DIR/sbk-yal-WebLogger.log"
timeout --signal=INT --kill-after="${KILL_GRACE_SECONDS}s" "${PROCESS_TIMEOUT}s" \
    "$SBK_YAL" -f "$YAL_FILE" -out WebLogger -webopen false -webport "$WEB_PORT" \
    -webtimeoutminutes "$WEB_TIMEOUT_MINUTES" > "$YAL_WEB_LOG" 2>&1
yal_web_status=$?
if [[ $yal_web_status -eq 0 ]] && grep -q 'Using the existing SBK Web Console' "$YAL_WEB_LOG"; then
    record_result sbk-yal-WebLogger PASS "$YAL_WEB_LOG"
else
    record_result sbk-yal-WebLogger FAIL "exit=$yal_web_status or Web Console was not reused; see $YAL_WEB_LOG"
fi

run_reject sbk-yal-missing "not found|No such file" "$SBK_YAL" -f "$WORK_DIR/missing.yml"
printf 'sbkArgs:\n  class: [invalid\n' > "$WORK_DIR/invalid.yml"
run_reject sbk-yal-invalid "deserialize|MismatchedInput|parse|mapping|yaml|YAML" \
    "$SBK_YAL" -f "$WORK_DIR/invalid.yml"

run_sbm_case() {
    local logger=$1
    local name=$2
    local client_mode=${3:-direct}
    local sbm_port
    local metrics_port
    local web_port
    sbm_port=$(free_port)
    metrics_port=$(free_port)
    web_port=$(free_port)
    local sbm_log="$LOG_DIR/${name}-sbm.log"
    local client_log="$LOG_DIR/${name}-client.log"
    local logger_args=()
    local readiness_url
    if [[ $logger == SbmPrometheusLogger ]]; then
        logger_args=(-context "$metrics_port/metrics")
        readiness_url="http://127.0.0.1:$metrics_port/metrics"
    else
        logger_args=(-webopen false -webport "$web_port" -webtimeoutminutes "$WEB_TIMEOUT_MINUTES")
        readiness_url="http://127.0.0.1:$web_port/api/v1/health"
    fi
    "$SBM" -out "$logger" -class file -action w -port "$sbm_port" "${logger_args[@]}" \
        > "$sbm_log" 2>&1 &
    local sbm_pid=$!
    if ! wait_for_url "$readiness_url"; then
        record_result "$name" FAIL "SBM did not become ready; see $sbm_log"
        stop_process "$sbm_pid"
        return
    fi
    if ! wait_for_port "$sbm_port" "$sbm_pid"; then
        record_result "$name" FAIL "SBM gRPC port did not become ready; see $sbm_log"
        stop_process "$sbm_pid"
        return
    fi
    if [[ $client_mode == yal ]]; then
        timeout --signal=INT --kill-after="${KILL_GRACE_SECONDS}s" "${PROCESS_TIMEOUT}s" \
            "$SBK_YAL" -f "$YAL_FILE" -out GrpcLogger -sbm 127.0.0.1 -sbmport "$sbm_port" \
            > "$client_log" 2>&1
    else
        timeout --signal=INT --kill-after="${KILL_GRACE_SECONDS}s" "${PROCESS_TIMEOUT}s" \
            "$SBK" -class file -file "$WORK_DIR/${name}.dat" -writers 1 -size "$RECORD_SIZE" \
            -records "$RECORDS" -out GrpcLogger -sbm 127.0.0.1 -sbmport "$sbm_port" \
            > "$client_log" 2>&1
    fi
    local client_status=$?
    sleep "$SBM_SETTLE_SECONDS"
    stop_process "$sbm_pid"
    if [[ $client_status -eq 0 ]] \
            && grep -Eiq 'GRPC Logger Shutdown' "$client_log" \
            && grep -Eq 'SBM .*Logger Started|SBM Started' "$sbm_log"; then
        record_result "$name" PASS "$sbm_log; $client_log"
    else
        record_result "$name" FAIL "client=$client_status; see $sbm_log and $client_log"
    fi
}

run_sbm_case SbmPrometheusLogger sbm-prometheus-grpc
run_sbm_case SbmWebLogger sbm-web-grpc
run_sbm_case SbmPrometheusLogger sbk-yal-GrpcLogger yal

if [[ $PROFILE == release || $PROFILE == local-docker ]]; then
    INVENTORY=${SBK_RELEASE_INVENTORY:?${PROFILE} profile requires SBK_RELEASE_INVENTORY}
    inventory_value() {
        local key=$1
        sed -n "s/^[[:space:]]*${key}[[:space:]]*=[[:space:]]*//p" "$INVENTORY" | tail -n 1
    }
    GEM_NODES=$(inventory_value gem.nodes)
    GEM_USER=$(inventory_value gem.user)
    GEM_KNOWN_HOSTS=$(inventory_value gem.knownHosts)
    GEM_PORT=$(inventory_value gem.port)
    GEM_LOCAL_HOST=$(inventory_value gem.localhost)
    GEM_PORT=${GEM_PORT:-22}
    GEM_NODE_COUNT=$(printf '%s\n' "$GEM_NODES" | tr ',[:space:]' '\n' | sed '/^$/d' | wc -l)
    GEM_NODE_COUNT=${GEM_NODE_COUNT//[[:space:]]/}
    if [[ $PROFILE == local-docker && $GEM_NODE_COUNT -ne $SBK_RELEASE_DOCKER_NODE_COUNT ]]; then
        printf 'The local-docker profile requires exactly two GEM nodes; found %s\n' \
            "$GEM_NODE_COUNT" >&2
        exit 1
    fi
    GEM_SUCCESS_PATTERN="expected nodes: ${GEM_NODE_COUNT}; successful nodes: ${GEM_NODE_COUNT}; "
    GEM_SUCCESS_PATTERN+="failed nodes: 0; maximum SBM registrations: ${GEM_NODE_COUNT}/${GEM_NODE_COUNT}"
    for logger in GemPrometheusLogger GemWebLogger; do
        gem_args=(-nodes "$GEM_NODES" -gemuser "$GEM_USER" -knownhosts "$GEM_KNOWN_HOSTS"
                  -gemport "$GEM_PORT" -class file -writers 1 -size "$RECORD_SIZE")
        if [[ $logger == GemPrometheusLogger ]]; then
            gem_args+=(-totalrecords "$RECORDS" -seconds "$SMOKE_BENCHMARK_SECONDS")
        else
            gem_args+=(-records "$RECORDS" -totalthroughput "$TOTAL_THROUGHPUT")
        fi
        gem_args+=(-out "$logger")
        if [[ -n $GEM_LOCAL_HOST ]]; then
            gem_args+=(-localhost "$GEM_LOCAL_HOST")
        fi
        if [[ $logger == GemWebLogger ]]; then
            gem_args+=(-webopen false -webport "$(free_port)" -webtimeoutminutes "$WEB_TIMEOUT_MINUTES")
        else
            gem_args+=(-context "$(free_port)/metrics")
        fi
        run_expect "sbk-gem-${logger}" "$GEM_SUCCESS_PATTERN" \
            "$SBK_GEM" "${gem_args[@]}"
    done

    GEM_YAL_FILE="$WORK_DIR/sbk-gem-release.yml"
    printf 'sbkGemArgs:\n  nodes: %s\n  gemuser: %s\n  knownhosts: %s\n  gemport: %s\n' \
        "$GEM_NODES" "$GEM_USER" "$GEM_KNOWN_HOSTS" "$GEM_PORT" > "$GEM_YAL_FILE"
    if [[ -n $GEM_LOCAL_HOST ]]; then
        printf '  localhost: %s\n' "$GEM_LOCAL_HOST" >> "$GEM_YAL_FILE"
    fi
    printf '  class: file\n  writers: 1\n  size: %s\n  totalrecords: %s\n' \
        "$RECORD_SIZE" "$RECORDS" >> "$GEM_YAL_FILE"
    printf '  totalthroughput: %s\n  out: GemPrometheusLogger\n' \
        "$TOTAL_THROUGHPUT" >> "$GEM_YAL_FILE"
    run_expect sbk-gem-yal-release "$GEM_SUCCESS_PATTERN" \
        "$SBK_GEM_YAL" -f "$GEM_YAL_FILE"
else
    record_result sbk-gem-external PASS "not mandatory in ${PROFILE} profile; release profile requires inventory"
    record_result sbk-gem-yal-external PASS "not mandatory in ${PROFILE} profile; release profile requires inventory"
fi

SUMMARY_FILE="$REPORT_DIR/functional-summary.json"
printf '{\n  "profile": "%s",\n  "passed": %s,\n  "failed": %s,\n  "status": "%s"\n}\n' \
    "$PROFILE" "$PASSES" "$FAILURES" "$([[ $FAILURES -eq 0 ]] && printf PASSED || printf FAILED)" \
    > "$SUMMARY_FILE"

if [[ $FAILURES -ne 0 ]]; then
    printf 'SBK functional release qualification failed: %s passed, %s failed\n' "$PASSES" "$FAILURES" >&2
    exit 1
fi

printf 'SBK functional release qualification passed: %s tests\n' "$PASSES"
