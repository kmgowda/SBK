#!/usr/bin/env bash
# Copyright (c) KMG. All Rights Reserved.
# Licensed under the Apache License, Version 2.0.

set -euo pipefail

if [[ $# -lt 1 || $# -gt 3 ]]; then
    echo "Usage: $0 <base-revision> [head-revision] [--print]" >&2
    echo "       printf 'path\n' | $0 --stdin [--print]" >&2
    exit 2
fi

base_revision=$1
head_revision=${2:-HEAD}
print_only=false
if [[ ${2:-} == "--print" ]]; then
    head_revision=HEAD
    print_only=true
elif [[ ${3:-} == "--print" ]]; then
    print_only=true
fi

if [[ $base_revision == "--stdin" ]]; then
    changed_files=$(cat)
else
    changed_files=$(git diff --name-only "${base_revision}...${head_revision}")
fi

declare -A selected_tasks=()
full_check=false

add_task() {
    selected_tasks["$1"]=1
}

while IFS= read -r changed_file; do
    [[ -z $changed_file ]] && continue
    case "$changed_file" in
        build.gradle|settings.gradle|settings-*.gradle|build-drivers.gradle|gradle.properties|\
        gradle/*.gradle|gradle/wrapper/*|checkstyle/*|config/*|lombok.config)
            full_check=true
            ;;
        perl/*)
            add_task :perl:check
            ;;
        sbk-web-console/*)
            add_task :sbk-web-console:check
            ;;
        sbk-api/*)
            add_task :sbk-api:check
            ;;
        sbm/*)
            add_task :sbm:check
            ;;
        sbk-gem/*)
            add_task :sbk-gem:check
            ;;
        sbk-yal/*)
            add_task :sbk-yal:check
            ;;
        sbk-gem-yal/*)
            add_task :sbk-gem-yal:check
            ;;
        drivers/*/*)
            driver_name=${changed_file#drivers/}
            driver_name=${driver_name%%/*}
            add_task ":drivers:${driver_name}:check"
            ;;
        docs/*|*.md|.github/*)
            ;;
        *)
            full_check=true
            ;;
    esac
done <<< "$changed_files"

if $full_check; then
    tasks=(check)
elif (( ${#selected_tasks[@]} == 0 )); then
    tasks=(help)
else
    mapfile -t tasks < <(printf '%s\n' "${!selected_tasks[@]}" | sort)
fi

echo "Affected Gradle tasks: ${tasks[*]}"
if ! $print_only; then
    exec ./gradlew "${tasks[@]}"
fi
