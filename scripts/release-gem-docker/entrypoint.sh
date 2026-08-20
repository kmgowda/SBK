#!/usr/bin/env bash
# Copyright (c) KMG. All Rights Reserved.
# Licensed under the Apache License, Version 2.0.

set -euo pipefail

install -d -m 0700 -o "$SBK_RELEASE_SSH_USER" -g "$SBK_RELEASE_SSH_USER" \
    "/home/$SBK_RELEASE_SSH_USER/.ssh"
install -m 0600 -o "$SBK_RELEASE_SSH_USER" -g "$SBK_RELEASE_SSH_USER" \
    /run/sbk/authorized_key "/home/$SBK_RELEASE_SSH_USER/.ssh/authorized_keys"
ssh-keygen -A

exec /usr/sbin/sshd -D -e \
    -p "$SBK_RELEASE_SSH_PORT" \
    -o PasswordAuthentication=no \
    -o KbdInteractiveAuthentication=no \
    -o PubkeyAuthentication=yes \
    -o PermitRootLogin=no
