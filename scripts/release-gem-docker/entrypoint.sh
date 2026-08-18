#!/usr/bin/env bash
# Copyright (c) KMG. All Rights Reserved.
# Licensed under the Apache License, Version 2.0.

set -euo pipefail

install -d -m 0700 -o sbk-release -g sbk-release /home/sbk-release/.ssh
install -m 0600 -o sbk-release -g sbk-release \
    /run/sbk/authorized_key /home/sbk-release/.ssh/authorized_keys
ssh-keygen -A

exec /usr/sbin/sshd -D -e \
    -o PasswordAuthentication=no \
    -o KbdInteractiveAuthentication=no \
    -o PubkeyAuthentication=yes \
    -o PermitRootLogin=no
