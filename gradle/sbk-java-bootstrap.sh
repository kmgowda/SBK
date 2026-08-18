#!/bin/sh
# Copyright (c) KMG. All Rights Reserved.
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#     http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.

# This file is sourced by gradlew and by the generated Unix launchers.
# It intentionally installs into a user cache and never modifies the system JDK.

if [ -z "${SBK_JAVA_CONFIG_FILE:-}" ] || [ ! -r "$SBK_JAVA_CONFIG_FILE" ]; then
    echo "ERROR: SBK Java bootstrap configuration is unavailable: ${SBK_JAVA_CONFIG_FILE:-not set}" >&2
    return 1
fi
. "$SBK_JAVA_CONFIG_FILE" || return 1

sbk_java_error() {
    echo "ERROR: $*" >&2
    return 1
}

sbk_java_major_version() {
    "$1" -version 2>&1 |
        sed -n 's/.*version "\([0-9][0-9]*\).*/\1/p' | sed -n '1p'
}

sbk_java_normalize_home() {
    case "$(uname -s)" in
        CYGWIN*|MINGW*|MSYS*)
            if command -v cygpath >/dev/null 2>&1; then
                cygpath --unix "$1"
            else
                printf '%s\n' "$1"
            fi
            ;;
        *) printf '%s\n' "$1" ;;
    esac
}

sbk_java_valid_home() {
    sbk_java_validated_home=$(sbk_java_normalize_home "$1") || return 1
    if [ -x "$sbk_java_validated_home/bin/java" ]; then
        sbk_java_validated_java=$sbk_java_validated_home/bin/java
    elif [ -x "$sbk_java_validated_home/bin/java.exe" ]; then
        sbk_java_validated_java=$sbk_java_validated_home/bin/java.exe
    else
        return 1
    fi
    if [ ! -x "$sbk_java_validated_home/bin/javac" ] &&
        [ ! -x "$sbk_java_validated_home/bin/javac.exe" ]; then
        return 1
    fi
    [ "$(sbk_java_major_version "$sbk_java_validated_java")" = "$SBK_JAVA_MAJOR" ]
}

sbk_java_select_home() {
    if ! sbk_java_valid_home "$1"; then
        return 1
    fi
    SBK_JAVA_HOME=$sbk_java_validated_home
    JAVACMD=$sbk_java_validated_java
    export SBK_JAVA_HOME
    return 0
}

sbk_java_home_from_path() {
    sbk_java_path_command=$(command -v java 2>/dev/null) || return 1
    sbk_java_path_home=$(
        "$sbk_java_path_command" -XshowSettings:properties -version 2>&1 |
            sed -n 's/^[[:space:]]*java.home = //p' | sed -n '1p'
    )
    [ -n "$sbk_java_path_home" ] || return 1
    sbk_java_select_home "$sbk_java_path_home"
}

sbk_java_checksum() {
    if command -v sha256sum >/dev/null 2>&1; then
        printf '%s  %s\n' "$2" "$1" | sha256sum -c - >/dev/null 2>&1
    elif command -v shasum >/dev/null 2>&1; then
        [ "$(shasum -a 256 "$1" | sed 's/[[:space:]].*//')" = "$2" ]
    else
        sbk_java_error "sha256sum or shasum is required to verify the managed JDK download."
    fi
}

sbk_java_download() {
    if command -v curl >/dev/null 2>&1; then
        curl --fail --location --retry "$SBK_JAVA_DOWNLOAD_RETRIES" \
            --connect-timeout "$SBK_JAVA_CONNECT_TIMEOUT_SECONDS" \
            --max-time "$SBK_JAVA_DOWNLOAD_TIMEOUT_SECONDS" \
            --output "$2" "$1"
    elif command -v wget >/dev/null 2>&1; then
        wget --tries="$SBK_JAVA_DOWNLOAD_RETRIES" \
            --connect-timeout="$SBK_JAVA_CONNECT_TIMEOUT_SECONDS" \
            --read-timeout="$SBK_JAVA_DOWNLOAD_TIMEOUT_SECONDS" \
            --output-document="$2" "$1"
    else
        sbk_java_error "curl or wget is required to download the managed JDK."
    fi
}

sbk_java_managed_platform() {
    sbk_java_os=$(uname -s)
    sbk_java_arch=$(uname -m)
    case "$sbk_java_os:$sbk_java_arch" in
        Linux:x86_64|Linux:amd64)
            SBK_JAVA_PLATFORM=linux-x64
            SBK_JAVA_SHA256=$SBK_JAVA_SHA256_LINUX_X64
            ;;
        Linux:aarch64|Linux:arm64)
            SBK_JAVA_PLATFORM=linux-aarch64
            SBK_JAVA_SHA256=$SBK_JAVA_SHA256_LINUX_AARCH64
            ;;
        Darwin:x86_64|Darwin:amd64)
            SBK_JAVA_PLATFORM=macos-x64
            SBK_JAVA_SHA256=$SBK_JAVA_SHA256_MACOS_X64
            ;;
        Darwin:arm64|Darwin:aarch64)
            SBK_JAVA_PLATFORM=macos-aarch64
            SBK_JAVA_SHA256=$SBK_JAVA_SHA256_MACOS_AARCH64
            ;;
        CYGWIN*:x86_64|MINGW*:x86_64|MSYS*:x86_64)
            SBK_JAVA_PLATFORM=windows-x64
            SBK_JAVA_SHA256=$SBK_JAVA_SHA256_WINDOWS_X64
            ;;
        *)
            sbk_java_error "automatic JDK installation is unsupported on $sbk_java_os/$sbk_java_arch. Set SBK_JAVA_HOME to a JDK 25 installation."
            ;;
    esac
}

sbk_java_install_archive() (
    sbk_java_lock=$sbk_java_target.lock
    sbk_java_lock_owned=false
    sbk_java_temp=

    sbk_java_install_cleanup() {
        if [ -n "$sbk_java_temp" ] && [ -d "$sbk_java_temp" ]; then
            rm -rf "$sbk_java_temp"
        fi
        if [ "$sbk_java_lock_owned" = true ]; then
            rm -rf "$sbk_java_lock"
        fi
    }
    trap 'sbk_java_install_cleanup; exit 130' HUP INT TERM

    sbk_java_lock_deadline=$(($(date +%s) + SBK_JAVA_LOCK_TIMEOUT_SECONDS))
    while ! mkdir "$sbk_java_lock" 2>/dev/null; do
        if sbk_java_select_home "$sbk_java_home"; then
            exit 0
        fi
        sbk_java_owner=$(sed -n '1p' "$sbk_java_lock/owner" 2>/dev/null)
        case "$sbk_java_owner" in
            ''|*[!0-9]*) ;;
            *)
                if ! kill -0 "$sbk_java_owner" 2>/dev/null; then
                    sbk_java_stale_lock=$sbk_java_lock.stale.$$
                    if mv "$sbk_java_lock" "$sbk_java_stale_lock" 2>/dev/null; then
                        rm -rf "$sbk_java_stale_lock"
                        continue
                    fi
                fi
                ;;
        esac
        sbk_java_lock_now=$(date +%s)
        if [ "$sbk_java_lock_now" -ge "$sbk_java_lock_deadline" ]; then
            sbk_java_error "timed out waiting for another SBK JDK installation: $sbk_java_lock"
            exit 1
        fi
        sbk_java_lock_remaining=$((sbk_java_lock_deadline - sbk_java_lock_now))
        sbk_java_lock_sleep=$SBK_JAVA_LOCK_POLL_SECONDS
        if [ "$sbk_java_lock_sleep" -gt "$sbk_java_lock_remaining" ]; then
            sbk_java_lock_sleep=$sbk_java_lock_remaining
        fi
        sleep "$sbk_java_lock_sleep"
    done
    sbk_java_lock_owned=true
    printf '%s\n' "$$" > "$sbk_java_lock/owner"

    if sbk_java_select_home "$sbk_java_home"; then
        sbk_java_install_cleanup
        exit 0
    fi

    sbk_java_temp=$(mktemp -d "$sbk_java_cache/.openjdk-$SBK_JAVA_VERSION.XXXXXX") || {
        sbk_java_install_cleanup
        exit 1
    }
    case "$SBK_JAVA_PLATFORM" in
        windows-*)
            sbk_java_archive=$sbk_java_temp/openjdk.zip
            sbk_java_url=$SBK_JAVA_BASE_URL/openjdk-${SBK_JAVA_VERSION}_${SBK_JAVA_PLATFORM}_bin.zip
            ;;
        *)
            sbk_java_archive=$sbk_java_temp/openjdk.tar.gz
            sbk_java_url=$SBK_JAVA_BASE_URL/openjdk-${SBK_JAVA_VERSION}_${SBK_JAVA_PLATFORM}_bin.tar.gz
            ;;
    esac
    echo "Downloading OpenJDK $SBK_JAVA_VERSION for $SBK_JAVA_PLATFORM to the SBK user cache..." >&2
    if ! sbk_java_download "$sbk_java_url" "$sbk_java_archive" ||
        ! sbk_java_checksum "$sbk_java_archive" "$SBK_JAVA_SHA256"; then
        sbk_java_install_cleanup
        sbk_java_error "managed JDK installation failed; check network/proxy access and retry."
        exit 1
    fi
    case "$SBK_JAVA_PLATFORM" in
        windows-*)
            if ! command -v unzip >/dev/null 2>&1 || ! unzip -q "$sbk_java_archive" -d "$sbk_java_temp"; then
                sbk_java_install_cleanup
                sbk_java_error "unzip is required to install OpenJDK on Cygwin/MSYS."
                exit 1
            fi
            ;;
        *)
            if ! tar -xzf "$sbk_java_archive" -C "$sbk_java_temp"; then
                sbk_java_install_cleanup
                sbk_java_error "managed JDK archive extraction failed."
                exit 1
            fi
            ;;
    esac

    case "$SBK_JAVA_PLATFORM" in
        macos-*) sbk_java_extracted=$sbk_java_temp/jdk-$SBK_JAVA_VERSION.jdk ;;
        *) sbk_java_extracted=$sbk_java_temp/jdk-$SBK_JAVA_VERSION ;;
    esac
    if ! sbk_java_valid_home "$(case "$SBK_JAVA_PLATFORM" in macos-*) printf '%s' "$sbk_java_extracted/Contents/Home" ;; *) printf '%s' "$sbk_java_extracted" ;; esac)"; then
        sbk_java_install_cleanup
        sbk_java_error "downloaded archive does not contain a valid JDK $SBK_JAVA_MAJOR."
        exit 1
    fi

    if [ -e "$sbk_java_target" ]; then
        if ! mv "$sbk_java_target" "$sbk_java_target.invalid.$$"; then
            sbk_java_install_cleanup
            exit 1
        fi
    fi
    if ! mv "$sbk_java_extracted" "$sbk_java_target"; then
        sbk_java_install_cleanup
        exit 1
    fi
    sbk_java_install_cleanup
    exit 0
)

sbk_java_install_managed() {
    sbk_java_managed_platform || return 1
    if [ -n "${SBK_JAVA_CACHE_DIR:-}" ]; then
        sbk_java_cache=$SBK_JAVA_CACHE_DIR
    elif [ -n "${XDG_CACHE_HOME:-}" ]; then
        sbk_java_cache=$XDG_CACHE_HOME/sbk/jdks
    else
        sbk_java_cache=$HOME/.cache/sbk/jdks
    fi

    sbk_java_target=$sbk_java_cache/openjdk-$SBK_JAVA_VERSION-$SBK_JAVA_PLATFORM
    sbk_java_home=$sbk_java_target
    case "$SBK_JAVA_PLATFORM" in
        macos-*) sbk_java_home=$sbk_java_target/Contents/Home ;;
    esac
    if sbk_java_select_home "$sbk_java_home"; then
        return 0
    fi
    if [ "${SBK_JAVA_INSTALL:-true}" != "true" ]; then
        return 1
    fi

    mkdir -p "$sbk_java_cache" || return 1
    sbk_java_install_archive || return 1
    sbk_java_select_home "$sbk_java_home"
}

if [ -n "${SBK_JAVA_HOME:-}" ]; then
    sbk_java_select_home "$SBK_JAVA_HOME" ||
        sbk_java_error "SBK_JAVA_HOME must point to a complete JDK $SBK_JAVA_MAJOR installation: $SBK_JAVA_HOME"
elif [ -n "${JAVA_HOME:-}" ]; then
    sbk_java_select_home "$JAVA_HOME" ||
        sbk_java_error "JAVA_HOME must point to a complete JDK $SBK_JAVA_MAJOR installation: $JAVA_HOME"
elif sbk_java_home_from_path; then
    :
elif sbk_java_install_managed; then
    echo "Using managed OpenJDK $SBK_JAVA_VERSION from $SBK_JAVA_HOME" >&2
else
    sbk_java_error "no usable JDK $SBK_JAVA_MAJOR was found. Set SBK_JAVA_HOME or JAVA_HOME."
fi
