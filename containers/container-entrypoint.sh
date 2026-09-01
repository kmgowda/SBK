#!/bin/sh
# Copyright (c) KMG. All Rights Reserved.
# Licensed under the Apache License, Version 2.0.

set -eu

case "${SBK_ROLE:-}" in
    sbk|sbk-yal|sbm|sbk-gem-yal)
        ;;
    *)
        echo "Unsupported or missing SBK_ROLE: ${SBK_ROLE:-<unset>}" >&2
        exit 64
        ;;
esac

exec "/opt/sbk/bin/${SBK_ROLE}" "$@"
