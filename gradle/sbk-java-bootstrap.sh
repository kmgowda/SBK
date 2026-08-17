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

SBK_JAVA_VERSION=25.0.2
SBK_JAVA_MAJOR=25
SBK_JAVA_BASE_URL=https://download.java.net/java/GA/jdk25.0.2/b1e0dfa218384cb9959bdcb897162d4e/10/GPL

sbk_java_error() {
    echo "ERROR: $*" >&2
    return 1
}

sbk_java_major_version() {
    "$1" -version 2>&1 | sed -n '1s/.*version "\([0-9][0-9]*\).*/\1/p'
}

sbk_java_valid_home() {
    [ -x "$1/bin/java" ] &&
        [ -x "$1/bin/javac" ] &&
        [ "$(sbk_java_major_version "$1/bin/java")" = "$SBK_JAVA_MAJOR" ]
}

sbk_java_select_home() {
    if ! sbk_java_valid_home "$1"; then
        return 1
    fi
    SBK_JAVA_HOME=$1
    JAVACMD=$SBK_JAVA_HOME/bin/java
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
        curl --fail --location --retry 3 --output "$2" "$1"
    elif command -v wget >/dev/null 2>&1; then
        wget --tries=3 --output-document="$2" "$1"
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
            SBK_JAVA_SHA256=555ce0821e4fe175ea50d54518cd6fbece9663c1998de529bc6ce429534457df
            ;;
        Linux:aarch64|Linux:arm64)
            SBK_JAVA_PLATFORM=linux-aarch64
            SBK_JAVA_SHA256=671208d205e70c9805da45a483f670d49dd64654990a7b7223ccffb2abb070dd
            ;;
        Darwin:x86_64|Darwin:amd64)
            SBK_JAVA_PLATFORM=macos-x64
            SBK_JAVA_SHA256=4ec2f4bc47b057fdf9cda07af27fae8f3605e90fa963d4240d63baeb46ede460
            ;;
        Darwin:arm64|Darwin:aarch64)
            SBK_JAVA_PLATFORM=macos-aarch64
            SBK_JAVA_SHA256=7581b0d1752cd5acbf39e286c03f07b6cd6c205b562eb2fe753ff0253cf4c1bf
            ;;
        *)
            sbk_java_error "automatic JDK installation is unsupported on $sbk_java_os/$sbk_java_arch. Set SBK_JAVA_HOME to a JDK 25 installation."
            ;;
    esac
}

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
    sbk_java_lock=$sbk_java_target.lock
    sbk_java_attempt=0
    while ! mkdir "$sbk_java_lock" 2>/dev/null; do
        if sbk_java_select_home "$sbk_java_home"; then
            return 0
        fi
        sbk_java_attempt=$((sbk_java_attempt + 1))
        if [ "$sbk_java_attempt" -ge 120 ]; then
            sbk_java_error "timed out waiting for another SBK JDK installation: $sbk_java_lock"
            return 1
        fi
        sleep 1
    done

    if sbk_java_select_home "$sbk_java_home"; then
        rmdir "$sbk_java_lock"
        return 0
    fi

    sbk_java_temp=$(mktemp -d "$sbk_java_cache/.openjdk-$SBK_JAVA_VERSION.XXXXXX") || {
        rmdir "$sbk_java_lock"
        return 1
    }
    sbk_java_archive=$sbk_java_temp/openjdk.tar.gz
    sbk_java_url=$SBK_JAVA_BASE_URL/openjdk-${SBK_JAVA_VERSION}_${SBK_JAVA_PLATFORM}_bin.tar.gz
    echo "Downloading OpenJDK $SBK_JAVA_VERSION for $SBK_JAVA_PLATFORM to the SBK user cache..." >&2
    if ! sbk_java_download "$sbk_java_url" "$sbk_java_archive" ||
        ! sbk_java_checksum "$sbk_java_archive" "$SBK_JAVA_SHA256" ||
        ! tar -xzf "$sbk_java_archive" -C "$sbk_java_temp"; then
        rm -rf "$sbk_java_temp"
        rmdir "$sbk_java_lock"
        sbk_java_error "managed JDK installation failed; check network/proxy access and retry."
        return 1
    fi

    case "$SBK_JAVA_PLATFORM" in
        macos-*) sbk_java_extracted=$sbk_java_temp/jdk-$SBK_JAVA_VERSION.jdk ;;
        *) sbk_java_extracted=$sbk_java_temp/jdk-$SBK_JAVA_VERSION ;;
    esac
    if ! sbk_java_valid_home "$(case "$SBK_JAVA_PLATFORM" in macos-*) printf '%s' "$sbk_java_extracted/Contents/Home" ;; *) printf '%s' "$sbk_java_extracted" ;; esac)"; then
        rm -rf "$sbk_java_temp"
        rmdir "$sbk_java_lock"
        sbk_java_error "downloaded archive does not contain a valid JDK $SBK_JAVA_MAJOR."
        return 1
    fi

    if [ -e "$sbk_java_target" ]; then
        mv "$sbk_java_target" "$sbk_java_target.invalid.$$" || {
            rm -rf "$sbk_java_temp"
            rmdir "$sbk_java_lock"
            return 1
        }
    fi
    mv "$sbk_java_extracted" "$sbk_java_target" || {
        rm -rf "$sbk_java_temp"
        rmdir "$sbk_java_lock"
        return 1
    }
    rm -rf "$sbk_java_temp"
    rmdir "$sbk_java_lock"
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
